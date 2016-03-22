package org.xbib.elasticsearch.action.termlist;

import java.util.*;
import java.util.concurrent.atomic.AtomicReferenceArray;

import org.apache.lucene.index.DocsAndPositionsEnum;
import org.apache.lucene.index.Fields;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.MultiFields;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.util.BytesRef;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.ShardOperationFailedException;
import org.elasticsearch.action.support.ActionFilters;
import org.elasticsearch.action.support.DefaultShardOperationFailedException;
import org.elasticsearch.action.support.broadcast.BroadcastShardOperationFailedException;
import org.elasticsearch.action.support.broadcast.TransportBroadcastAction;
import org.elasticsearch.cluster.ClusterService;
import org.elasticsearch.cluster.ClusterState;
import org.elasticsearch.cluster.block.ClusterBlockException;
import org.elasticsearch.cluster.block.ClusterBlockLevel;
import org.elasticsearch.cluster.metadata.IndexNameExpressionResolver;
import org.elasticsearch.cluster.routing.GroupShardsIterator;
import org.elasticsearch.cluster.routing.ShardRouting;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.common.logging.ESLoggerFactory;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.index.engine.Engine;
import org.elasticsearch.index.shard.IndexShard;
import org.elasticsearch.indices.IndicesService;
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.transport.TransportService;
import org.xbib.elasticsearch.common.termlist.CompactHashMap;
import org.xbib.elasticsearch.common.termlist.math.SummaryStatistics;
//import static org.elasticsearch.common.collect.;

/**
 * Termlist index/indices action.
 */
public class TransportTermlistAction
        extends TransportBroadcastAction<TermlistRequest, TermlistResponse, ShardTermlistRequest, ShardTermlistResponse> {

    private final static ESLogger logger = ESLoggerFactory.getLogger(TransportTermlistAction.class.getName());

    private final IndicesService indicesService;

    @Inject
    public TransportTermlistAction(Settings settings, ThreadPool threadPool, ClusterService clusterService,
                                   TransportService transportService,
                                   IndicesService indicesService,
                                   ActionFilters actionFilters, IndexNameExpressionResolver indexNameExpressionResolver) {
        super(settings, TermlistAction.NAME, threadPool, clusterService, transportService, actionFilters,
                    indexNameExpressionResolver,TermlistRequest.class,ShardTermlistRequest.class,ThreadPool.Names.GENERIC );
        this.indicesService = indicesService;
    }


    @Override
    protected TermlistResponse newResponse(TermlistRequest request, AtomicReferenceArray shardsResponses, ClusterState clusterState) {
        int successfulShards = 0;
        int failedShards = 0;
        List<ShardOperationFailedException> shardFailures = null;
        int numdocs = 0;
        Map<String, TermInfo> map = new CompactHashMap<String, TermInfo>();
        for (int i = 0; i < shardsResponses.length(); i++) {
            Object shardResponse = shardsResponses.get(i);
            if (shardResponse instanceof BroadcastShardOperationFailedException) {
                BroadcastShardOperationFailedException e = (BroadcastShardOperationFailedException)shardResponse;
                logger.error(e.getMessage(), e);
                failedShards++;
                if (shardFailures == null) {
                    shardFailures = new ArrayList<ShardOperationFailedException>();
                }
                shardFailures.add(new DefaultShardOperationFailedException(e));
            } else {
                if (shardResponse instanceof ShardTermlistResponse) {
                    successfulShards++;
                    ShardTermlistResponse resp = (ShardTermlistResponse) shardResponse;
                    numdocs += resp.getNumDocs();
                    update(map, resp.getTermList());
                }
            }
        }
        map = request.sortByTotalFreq() ? sortTotalFreq(map, request.getFrom(), request.getSize()) :
                request.sortByDocFreq() ? sortDocFreq(map, request.getFrom(), request.getSize()) :
                        request.sortByTerm() ? sortTerm(map, request.getFrom(), request.getSize()) :
                                request.getSize() >= 0 ? truncate(map, request.getFrom(), request.getSize()) : map;

        return new TermlistResponse(shardsResponses.length(), successfulShards, failedShards, shardFailures, numdocs, map);
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
        return state.blocks().globalBlockedException(ClusterBlockLevel.METADATA_READ);
    }

    @Override
    protected ClusterBlockException checkRequestBlock(ClusterState state, TermlistRequest request, String[] concreteIndices) {
        return state.blocks().indicesBlockedException(ClusterBlockLevel.METADATA_READ, concreteIndices);
    }

    @Override
    protected ShardTermlistResponse shardOperation(ShardTermlistRequest request) throws ElasticsearchException {
        IndexShard indexShard = indicesService.indexServiceSafe(request.getIndex()).shardSafe(request.shardId().id());
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
                            TermsEnum termsEnum = terms.iterator();
                            BytesRef text;
                            while ((text = termsEnum.next()) != null) {
                                // skip invalid terms
                                if (termsEnum.docFreq() < request.getRequest().getMinDocFreq()) {
                                    continue;
                                }
                                if (termsEnum.totalTermFreq() < request.getRequest().getMinTotalFreq()) {
                                    continue;
                                }
                                // docFreq() = the number of documents containing the current term
                                // totalTermFreq() = total number of occurrences of this term across all documents
                                Term term = new Term(field, text);
                                if (request.getRequest().getTerm() == null || term.text().startsWith(request.getRequest().getTerm())) {
                                    TermInfo termInfo = new TermInfo();
                                    DocsAndPositionsEnum docPosEnum = termsEnum.docsAndPositions(null, null);
                                    SummaryStatistics stat = new SummaryStatistics();
                                    while (docPosEnum.nextDoc() != DocsAndPositionsEnum.NO_MORE_DOCS) {
                                        stat.addValue(docPosEnum.freq());
                                    }
                                    termInfo.setSummaryStatistics(stat);
                                    termInfo.setDocFreq(termsEnum.docFreq());
                                    termInfo.setTotalFreq(termsEnum.totalTermFreq());
                                    map.put(term.text(), termInfo);
                                }
                            }
                        }
                    }
                }
            }
            return new ShardTermlistResponse(request.getIndex(), request.shardId(), reader.numDocs(), map);
        } catch (Throwable ex) {
            logger.error(ex.getMessage(), ex);
            throw new ElasticsearchException(ex.getMessage(), ex);
        } finally {
            searcher.close();
        }
    }

    private void update(Map<String, TermInfo> map, Map<String, TermInfo> other) {
        for (Map.Entry<String, TermInfo> t2 : other.entrySet()) {
            if (map.containsKey(t2.getKey())) {
                TermInfo t1 = map.get(t2.getKey());
                Long totalFreq = t1.getTotalFreq();
                if (totalFreq != null) {
                    if (t2.getValue().getTotalFreq() != null) {
                        t1.setTotalFreq(totalFreq + t2.getValue().getTotalFreq());
                    }
                } else {
                    if (t2.getValue().getTotalFreq() != null) {
                        t1.setTotalFreq(t2.getValue().getTotalFreq());
                    }
                }
                Integer docFreq = t1.getDocFreq();
                if (docFreq != null) {
                    if (t2.getValue().getDocFreq() != null) {
                        t1.setDocFreq(docFreq + t2.getValue().getDocFreq());
                    }
                } else {
                    if (t2.getValue().getDocFreq() != null) {
                        t1.setDocFreq(t2.getValue().getDocFreq());
                    }
                }
                SummaryStatistics summaryStatistics = t1.getSummaryStatistics();
                if (summaryStatistics != null) {
                    if (t2.getValue().getSummaryStatistics() != null) {
                        summaryStatistics.update(t2.getValue().getSummaryStatistics());
                    }
                } else {
                    if (t2.getValue().getSummaryStatistics() != null) {
                        t1.setSummaryStatistics(t2.getValue().getSummaryStatistics());
                    }
                }
                map.put(t2.getKey(), t1);
            } else {
                map.put(t2.getKey(), t2.getValue());
            }
        }
    }

    private SortedMap<String, TermInfo> sortTerm(final Map<String, TermInfo> map, Integer from, Integer size) {
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