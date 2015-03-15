package org.xbib.elasticsearch.action.termlist;

import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicReferenceArray;

import org.apache.lucene.index.DocsAndPositionsEnum;
import org.apache.lucene.index.Fields;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.MultiFields;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.util.BytesRef;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.ShardOperationFailedException;
import org.elasticsearch.action.support.ActionFilters;
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
                                   TransportService transportService, IndicesService indicesService,
                                   ActionFilters actionFilters) {
        super(settings, TermlistAction.NAME, threadPool, clusterService, transportService, actionFilters);
        this.indicesService = indicesService;
    }

    @Override
    protected String executor() {
        return ThreadPool.Names.GENERIC;
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
                BroadcastShardOperationFailedException e = (BroadcastShardOperationFailedException)shardResponse;
                logger.error(e.getMessage(), e);
                failedShards++;
                if (shardFailures == null) {
                    shardFailures = newLinkedList();
                }
                shardFailures.add(new DefaultShardOperationFailedException(e));
            } else {
                if (shardResponse instanceof ShardTermlistResponse) {
                    successfulShards++;
                    ShardTermlistResponse resp = (ShardTermlistResponse) shardResponse;
                    merge(map, resp.getTermList());
                }
            }
        }
        int size = map.size();
        map = request.sortByTotalFreq() ? sortTotalFreq(map, request.getFrom(), request.getSize()) :
                request.sortByDocFreq() ? sortDocFreq(map, request.getFrom(), request.getSize()) :
                        request.sortByTerm() ? sortTerm(map, request.getFrom(), request.getSize()) :
                                truncate(map, request.getFrom(), request.getSize());

        return new TermlistResponse(shardsResponses.length(), successfulShards, failedShards, shardFailures, size, map);
    }

    @Override
    protected ShardTermlistRequest newShardRequest() {
        return new ShardTermlistRequest();
    }

    @Override
    protected ShardTermlistRequest newShardRequest(int numShards, ShardRouting shard, TermlistRequest request) {
        return new ShardTermlistRequest(shard.getIndex(), shard.shardId(), request);
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
        InternalIndexShard indexShard = (InternalIndexShard) indicesService.indexServiceSafe(request.getIndex()).shardSafe(request.shardId().id());
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
                    if (request.getRequest().getField() == null || field.equals(request.getRequest().getField())) {
                        Terms terms = fields.terms(field);
                        // Returns the number of documents that have at least one
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
                                String term = text.utf8ToString();
                                TermInfo termInfo = new TermInfo();
                                if (request.getRequest().getWithTermFreq()) {
                                    // just get first document and pos (which is absurd...)
                                    DocsAndPositionsEnum docPosEnum = termsEnum.docsAndPositions(null, null);
                                    docPosEnum.nextDoc();
                                    termInfo.setTermFreq(docPosEnum.freq());
                                }
                                if (request.getRequest().getWithDocFreq()) {
                                    // the number of documents containing this term
                                    termInfo.setDocFreq(termsEnum.docFreq());
                                }
                                if (request.getRequest().getWithTotalFreq()) {
                                    // Returns the total number of occurrences of this term
                                    // across all documents (the sum of the freq() for each
                                    // doc that has this term).
                                    termInfo.setTotalFreq(termsEnum.totalTermFreq());
                                }
                                if (request.getRequest().getTerm() == null || term.startsWith(request.getRequest().getTerm())) {
                                    map.put(term, termInfo);
                                }
                            }
                        }
                    }
                }
            }
            return new ShardTermlistResponse(request.getIndex(), request.shardId(), map);
        } catch (Throwable ex) {
            logger.error(ex.getMessage(), ex);
            throw new ElasticsearchException(ex.getMessage(), ex);
        } finally {
            searcher.close();
        }
    }

    private void merge(Map<String, TermInfo> map, Map<String, TermInfo> other) {
        for (Map.Entry<String, TermInfo> t : other.entrySet()) {
            if (map.containsKey(t.getKey())) {
                TermInfo info = map.get(t.getKey());
                Integer termFreq = info.getTermFreq();
                if (termFreq != null) {
                    if (t.getValue().getTermFreq() != null) {
                        info.setTermFreq(termFreq + t.getValue().getTermFreq());
                    }
                } else {
                    if (t.getValue().getTermFreq() != null) {
                        info.setTermFreq(t.getValue().getTermFreq());
                    }
                }
                Integer docCount = info.getDocCount();
                if (docCount != null) {
                    if (t.getValue().getDocCount() != null) {
                        info.setDocCount(docCount + t.getValue().getDocCount());
                    }
                } else {
                    if (t.getValue().getDocCount() != null) {
                        info.setDocCount(t.getValue().getDocCount());
                    }
                }
                Integer docFreq = info.getDocFreq();
                if (docFreq != null) {
                    if (t.getValue().getDocFreq() != null) {
                        info.setDocFreq(docFreq + t.getValue().getDocFreq());
                    }
                } else {
                    if (t.getValue().getDocFreq() != null) {
                        info.setDocFreq(t.getValue().getDocFreq());
                    }
                }
                Long totalFreq = info.getTotalFreq();
                if (totalFreq != null) {
                    if (t.getValue().getTotalFreq() != null) {
                        info.setTotalFreq(totalFreq + t.getValue().getTotalFreq());
                    }
                } else {
                    if (t.getValue().getTotalFreq() != null) {
                        info.setTotalFreq(t.getValue().getTotalFreq());
                    }
                }
            } else {
                map.put(t.getKey(), t.getValue());
            }
        }
    }

    private SortedMap<String, TermInfo> sortTerm(final Map<String, TermInfo> map, Integer from, Integer size) {
        /**
         * Should be collator based
         */
        Comparator<String> comp = new Comparator<String>() {
            @Override
            public int compare(String t1, String t2) {
                return t1.compareTo(t2);
            }
        };
        TreeMap<String, TermInfo> m = new TreeMap<String, TermInfo>(comp);
        m.putAll(map);
        if (size != null && size > 0) {
            TreeMap<String, TermInfo> treeMap = new TreeMap<String, TermInfo>(comp);
            for (int i = 0; i < m.size(); i++) {
                Map.Entry<String, TermInfo> me = m.pollFirstEntry();
                if (from <= i && i < from + size) {
                    treeMap.put(me.getKey(), me.getValue());
                }
            }
            return treeMap;
        }
        return m;
    }
    private SortedMap<String, TermInfo> sortTotalFreq(final Map<String, TermInfo> map, Integer from, Integer size) {
        Comparator<String> comp = new Comparator<String>() {
            @Override
            public int compare(String t1, String t2) {
                Long l1 = map.get(t1).getTotalFreq();
                String sl1 = Long.toString(l1);
                String s1 = sl1.length() + sl1 + t1;
                Long l2 = map.get(t2).getTotalFreq();
                String sl2 = Long.toString(l2);
                String s2 =sl2.length() + sl2 + t2;
                return -s1.compareTo(s2);
            }
        };
        TreeMap<String, TermInfo> m = new TreeMap<String, TermInfo>(comp);
        m.putAll(map);
        if (size != null && size > 0) {
            TreeMap<String, TermInfo> treeMap = new TreeMap<String, TermInfo>(comp);
            for (int i = 0; i < m.size(); i++) {
                Map.Entry<String, TermInfo> me = m.pollFirstEntry();
                if (from <= i && i < from + size) {
                    treeMap.put(me.getKey(), me.getValue());
                }
            }
            return treeMap;
        }
        return m;
    }

    private SortedMap<String, TermInfo> sortDocFreq(final Map<String, TermInfo> map, Integer from, Integer size) {
        Comparator<String> comp = new Comparator<String>() {
            @Override
            public int compare(String t1, String t2) {
                Integer i1 = map.get(t1).getDocFreq();
                String si1 = Integer.toString(i1);
                String s1 = si1.length() + si1 + t1;
                Integer i2 = map.get(t2).getDocFreq();
                String si2 = Integer.toString(i2);
                String s2 = si2.length() + si2 + t2;
                return -s1.compareTo(s2);
            }
        };
        TreeMap<String, TermInfo> m = new TreeMap<String, TermInfo>(comp);
        m.putAll(map);
        if (size != null && size > 0) {
            TreeMap<String, TermInfo> treeMap = new TreeMap<String, TermInfo>(comp);
            for (int i = 0; i < m.size(); i++) {
                Map.Entry<String, TermInfo> me = m.pollFirstEntry();
                if (from <= i && i < from + size) {
                    treeMap.put(me.getKey(), me.getValue());
                }
            }
            return treeMap;
        }
        return m;
    }

    private Map<String, TermInfo> truncate(Map<String, TermInfo> source, Integer from, Integer size) {
        if (size == null || size < 1) {
            return source;
        }
        Map<String, TermInfo> target = new CompactHashMap<String, TermInfo>();
        Iterator<Map.Entry<String, TermInfo>> it = source.entrySet().iterator();
        for (int i = 0 ; i < source.size(); i++) {
            Map.Entry<String, TermInfo> entry = it.next();
            if (from <= i && i < from + size) {
                target.put(entry.getKey(), entry.getValue());
            }
        }
        return target;
    }

}