package org.xbib.elasticsearch.action.termlist;

import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicReferenceArray;

import org.apache.lucene.index.Fields;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.MultiFields;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.util.BytesRef;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.ShardOperationFailedException;
import org.elasticsearch.action.support.DefaultShardOperationFailedException;
import org.elasticsearch.action.support.broadcast.BroadcastShardOperationFailedException;
import org.elasticsearch.action.support.broadcast.TransportBroadcastOperationAction;
import org.elasticsearch.cluster.ClusterService;
import org.elasticsearch.cluster.ClusterState;
import org.elasticsearch.cluster.block.ClusterBlockException;
import org.elasticsearch.cluster.block.ClusterBlockLevel;
import org.elasticsearch.cluster.routing.GroupShardsIterator;
import org.elasticsearch.cluster.routing.ShardRouting;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.index.engine.Engine;
import org.elasticsearch.index.shard.service.InternalIndexShard;
import org.elasticsearch.indices.IndicesService;
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.transport.TransportService;
import org.xbib.elasticsearch.common.termlist.CompactHashMap;

import static org.elasticsearch.common.collect.Lists.newLinkedList;

/**
 * Termlist index/indices action.
 */
public class TransportTermlistAction
        extends TransportBroadcastOperationAction<TermlistRequest, TermlistResponse, ShardTermlistRequest, ShardTermlistResponse> {

    private final IndicesService indicesService;

    @Inject
    public TransportTermlistAction(Settings settings, ThreadPool threadPool, ClusterService clusterService,
                                   TransportService transportService, IndicesService indicesService) {
        super(settings, threadPool, clusterService, transportService);
        this.indicesService = indicesService;
    }

    @Override
    protected String executor() {
        return ThreadPool.Names.GENERIC;
    }

    @Override
    protected String transportAction() {
        return TermlistAction.NAME;
    }

    @Override
    protected TermlistRequest newRequest() {
        return new TermlistRequest();
    }

    @Override
    protected TermlistResponse newResponse(TermlistRequest request, AtomicReferenceArray shardsResponses, ClusterState clusterState) {
        int successfulShards = 0;
        int failedShards = 0;
        List<ShardOperationFailedException> shardFailures = null;
        Map<String, TermInfo> map = new CompactHashMap<String, TermInfo>();
        for (int i = 0; i < shardsResponses.length(); i++) {
            Object shardResponse = shardsResponses.get(i);
            if (shardResponse instanceof BroadcastShardOperationFailedException) {
                failedShards++;
                if (shardFailures == null) {
                    shardFailures = newLinkedList();
                }
                shardFailures.add(new DefaultShardOperationFailedException((BroadcastShardOperationFailedException) shardResponse));
            } else {
                successfulShards++;
                if (shardResponse instanceof ShardTermlistResponse) {
                    ShardTermlistResponse resp = (ShardTermlistResponse) shardResponse;
                    merge(map, resp.getTermList());
                }
            }
        }
        map = request.getWithTotalFreq() ? sortTotalFreq(map, request.getSize()) :
                request.getWithDocFreq() ? sortDocFreq(map, request.getSize()) :
                        truncate(map, request.getSize());

        return new TermlistResponse(shardsResponses.length(), successfulShards, failedShards, shardFailures, map);
    }

    @Override
    protected ShardTermlistRequest newShardRequest() {
        return new ShardTermlistRequest();
    }

    @Override
    protected ShardTermlistRequest newShardRequest(ShardRouting shard, TermlistRequest request) {
        return new ShardTermlistRequest(shard.index(), shard.id(), request);
    }

    @Override
    protected ShardTermlistResponse newShardResponse() {
        return new ShardTermlistResponse();
    }

    /**
     * The termlist request works against primary shards.
     */
    @Override
    protected GroupShardsIterator shards(ClusterState clusterState, TermlistRequest request, String[] concreteIndices) {
        return clusterState.routingTable().activePrimaryShardsGrouped(concreteIndices, true);
    }

    @Override
    protected ClusterBlockException checkGlobalBlock(ClusterState state, TermlistRequest request) {
        return state.blocks().globalBlockedException(ClusterBlockLevel.METADATA);
    }

    @Override
    protected ClusterBlockException checkRequestBlock(ClusterState state, TermlistRequest request, String[] concreteIndices) {
        return state.blocks().indicesBlockedException(ClusterBlockLevel.METADATA, concreteIndices);
    }

    @Override
    protected ShardTermlistResponse shardOperation(ShardTermlistRequest request) throws ElasticsearchException {
        InternalIndexShard indexShard = (InternalIndexShard) indicesService.indexServiceSafe(request.index()).shardSafe(request.shardId());
        Engine.Searcher searcher = indexShard.engine().acquireSearcher("termlist");
        try {
            Map<String, TermInfo> map = new CompactHashMap<String, TermInfo>();
            IndexReader reader = searcher.reader();
            Fields fields = MultiFields.getFields(reader);
            if (fields != null) {
                for (String field : fields) {
                    // skip internal fields
                    if (field.charAt(0) == '_') {
                        continue;
                    }
                    if (request.getField() == null || field.equals(request.getField())) {
                        Terms terms = fields.terms(field);
                        if (terms != null) {
                            TermsEnum termsEnum = terms.iterator(null);
                            BytesRef text;
                            while ((text = termsEnum.next()) != null) {
                                // skip invalid terms
                                if (termsEnum.docFreq() < 1) {
                                    continue;
                                }
                                if (termsEnum.totalTermFreq() < 1) {
                                    continue;
                                }
                                TermInfo t = new TermInfo();
                                if (request.getWithDocFreq()) {
                                    t.docfreq(termsEnum.docFreq());
                                }
                                if (request.getWithTotalFreq()) {
                                    t.totalFreq(termsEnum.totalTermFreq());
                                }
                                map.put(text.utf8ToString(), t);
                            }
                        }
                    }
                }
            }
            return new ShardTermlistResponse(request.index(), request.shardId(), map);
        } catch (IOException ex) {
            throw new ElasticsearchException(ex.getMessage(), ex);
        } finally {
            searcher.close();
        }
    }

    private void merge(Map<String, TermInfo> map, Map<String, TermInfo> other) {
        for (Map.Entry<String, TermInfo> t : other.entrySet()) {
            if (map.containsKey(t.getKey())) {
                TermInfo info = map.get(t.getKey());
                Integer docFreq = info.getDocFreq();
                if (docFreq != null) {
                    if (t.getValue().getDocFreq() != null) {
                        info.docfreq(docFreq + t.getValue().getDocFreq());
                    }
                } else {
                    if (t.getValue().getDocFreq() != null) {
                        info.docfreq(t.getValue().getDocFreq());
                    }
                }
                Long totalFreq = info.getTotalFreq();
                if (totalFreq != null) {
                    if (t.getValue().getTotalFreq() != null) {
                        info.totalFreq(totalFreq + t.getValue().getTotalFreq());
                    }
                } else {
                    if (t.getValue().getTotalFreq() != null) {
                        info.totalFreq(t.getValue().getTotalFreq());
                    }
                }
            } else {
                map.put(t.getKey(), t.getValue());
            }
        }
    }

    private SortedMap<String, TermInfo> sortTotalFreq(final Map<String, TermInfo> map, Integer size) {
        Comparator<String> comp = new Comparator<String>() {
            @Override
            public int compare(String t1, String t2) {
                Long l1 = map.get(t1).getTotalFreq();
                String s1 = Long.toString(l1).length() + Long.toString(l1) + t1;
                Long l2 = map.get(t2).getTotalFreq();
                String s2 = Long.toString(l2).length() + Long.toString(l2) + t2;
                return -s1.compareTo(s2);
            }
        };
        TreeMap<String, TermInfo> m = new TreeMap<String, TermInfo>(comp);
        m.putAll(map);
        if (size != null && size > 0) {
            TreeMap<String, TermInfo> n = new TreeMap<String, TermInfo>(comp);
            Map.Entry<String, TermInfo> me = m.pollFirstEntry();
            while (me != null && size-- > 0) {
                n.put(me.getKey(), me.getValue());
                me = m.pollFirstEntry();
            }
            return n;
        }
        return m;
    }

    private SortedMap<String, TermInfo> sortDocFreq(final Map<String, TermInfo> map, Integer size) {
        Comparator<String> comp = new Comparator<String>() {
            @Override
            public int compare(String t1, String t2) {
                Integer i1 = map.get(t1).getDocFreq();
                String s1 = Integer.toString(i1).length() + Integer.toString(i1) + t1;
                Integer i2 = map.get(t2).getDocFreq();
                String s2 = Integer.toString(i2).length() + Integer.toString(i2) + t2;
                return -s1.compareTo(s2);
            }
        };
        TreeMap<String, TermInfo> m = new TreeMap<String, TermInfo>(comp);
        m.putAll(map);
        if (size != null && size > 0) {
            TreeMap<String, TermInfo> n = new TreeMap<String, TermInfo>(comp);
            Map.Entry<String, TermInfo> me = m.pollFirstEntry();
            while (me != null && size-- > 0) {
                n.put(me.getKey(), me.getValue());
                me = m.pollFirstEntry();
            }
            return n;
        }
        return m;
    }

    private Map<String, TermInfo> truncate(Map<String, TermInfo> source, Integer max) {
        if (max == null || max < 1) {
            return source;
        }
        int count = 0;
        Map<String, TermInfo> target = new CompactHashMap<String, TermInfo>();
        for (Map.Entry<String, TermInfo> entry : source.entrySet()) {
            if (count >= max) {
                break;
            }
            target.put(entry.getKey(), entry.getValue());
            count++;
        }
        return target;
    }

}