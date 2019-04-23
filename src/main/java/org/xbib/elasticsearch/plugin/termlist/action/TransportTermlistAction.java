package org.xbib.elasticsearch.plugin.termlist.action;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.lucene.index.Fields;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.MultiFields;
import org.apache.lucene.index.PostingsEnum;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.search.spell.LevensteinDistance;
import org.apache.lucene.util.BytesRef;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.ShardOperationFailedException;
import org.elasticsearch.action.support.ActionFilters;
import org.elasticsearch.action.support.DefaultShardOperationFailedException;
import org.elasticsearch.action.support.broadcast.BroadcastShardOperationFailedException;
import org.elasticsearch.action.support.broadcast.TransportBroadcastAction;
import org.elasticsearch.cluster.ClusterState;
import org.elasticsearch.cluster.block.ClusterBlockException;
import org.elasticsearch.cluster.block.ClusterBlockLevel;
import org.elasticsearch.cluster.metadata.IndexNameExpressionResolver;
import org.elasticsearch.cluster.routing.GroupShardsIterator;
import org.elasticsearch.cluster.routing.ShardIterator;
import org.elasticsearch.cluster.routing.ShardRouting;
import org.elasticsearch.cluster.service.ClusterService;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.index.IndexService;
import org.elasticsearch.index.engine.Engine;
import org.elasticsearch.index.shard.IndexShard;
import org.elasticsearch.indices.IndicesService;
import org.elasticsearch.tasks.Task;
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.transport.TransportService;
import org.xbib.elasticsearch.plugin.termlist.common.CompactHashMap;
import org.xbib.elasticsearch.plugin.termlist.common.math.SummaryStatistics;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicReferenceArray;

public class TransportTermlistAction
        extends TransportBroadcastAction<TermlistRequest, TermlistResponse, ShardTermlistRequest, ShardTermlistResponse> {

    private static final Logger logger = LogManager.getLogger(TransportTermlistAction.class.getName());

    private final IndicesService indicesService;

    @Inject
    public TransportTermlistAction(Settings settings, ThreadPool threadPool, ClusterService clusterService,
                                   TransportService transportService,
                                   IndicesService indicesService,
                                   ActionFilters actionFilters, IndexNameExpressionResolver indexNameExpressionResolver) {
        super(settings, TermlistAction.NAME, threadPool, clusterService, transportService, actionFilters,
                indexNameExpressionResolver, TermlistRequest::new, ShardTermlistRequest::new, ThreadPool.Names.GENERIC);
        this.indicesService = indicesService;
    }


    @SuppressWarnings("rawtypes")
    @Override
    protected TermlistResponse newResponse(TermlistRequest request, AtomicReferenceArray shardsResponses, ClusterState clusterState) {
        List<ShardOperationFailedException> shardFailures = null;
        int numdocs = 0;
        Map<String, TermInfo> map = new CompactHashMap<String, TermInfo>();
        for (int i = 0; i < shardsResponses.length(); i++) {
            Object shardResponse = shardsResponses.get(i);
            if (shardResponse instanceof BroadcastShardOperationFailedException) {
                BroadcastShardOperationFailedException e = (BroadcastShardOperationFailedException) shardResponse;
                logger.error(e.getMessage(), e);
                if (shardFailures == null) {
                    shardFailures = new ArrayList<>();
                }
                shardFailures.add(new DefaultShardOperationFailedException(e));
            } else {
                if (shardResponse instanceof ShardTermlistResponse) {
                    ShardTermlistResponse resp = (ShardTermlistResponse) shardResponse;
                    numdocs += resp.getNumDocs();
                    update(map, resp.getTermList());
                }
            }
        }
        map = request.sortByTotalFreq() ? sortTotalFreq(map, request.getFrom(), request.getSize()) : map;
        map = request.sortByDocFreq() ? sortDocFreq(map, request.getFrom(), request.getSize()) : map;
        map = request.sortByTerm() ? sortTerm(map, request.getTerm(), request.getFrom(), request.getSize(),
                request.getBackTracingCount()) : map;
        //map = request.getSize() >= 0 ? truncate(map, request.sortByTerm(), request.getTerm(), request.getFrom(), request.getSize()) : map;
        return new TermlistResponse(numdocs, map);
    }

    @Override
    protected ShardTermlistRequest newShardRequest(int numShards, ShardRouting shard, TermlistRequest request) {
        return new ShardTermlistRequest(shard.index().getName(), shard.shardId(), request);
    }

    @Override
    protected ShardTermlistResponse newShardResponse() {
        return new ShardTermlistResponse();
    }

    @Override
    protected GroupShardsIterator<ShardIterator> shards(ClusterState clusterState, TermlistRequest request, String[] concreteIndices) {
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

    boolean anyMatch(ArrayList<String> arrList, String indexTerm) {
        for (String term : arrList) {
            if (indexTerm.startsWith(term))
                return true;
        }
        return false;
    }

    @Override
    protected ShardTermlistResponse shardOperation(ShardTermlistRequest request, Task task) throws IOException {
        boolean found = false;
        IndexService indexService = null;
        Iterator<IndexService> iterator = indicesService.iterator();
        while (iterator.hasNext()) {
            indexService = iterator.next();
            if (indexService.getIndexSettings().getIndex().getName().equals(request.getIndex())) {
                found = true;
            }
        }
        if (!found) {
            throw new IOException("index not found");
        }
        IndexShard indexShard = indexService.getShard(request.shardId().id());
        Engine.Searcher searcher = indexShard.acquireSearcher("termlist");
        try {
            Map<String, TermInfo> map = new CompactHashMap<>();
            ArrayList<String> stringsToSearch = new ArrayList<>();
            if (request.getRequest().getTerm() != null) {
                String requestTerm = request.getRequest().getTerm();
                int backtracingCount = request.getRequest().getBackTracingCount();
                stringsToSearch.add(requestTerm);
                int i = 1;
                while (backtracingCount > 0) {
                    if (requestTerm.length() > i) {
                        stringsToSearch.add(requestTerm.substring(0, requestTerm.length() - i));
                    }
                    else {
                        break;
                    }
                    i++;
                    backtracingCount--;
                }
            }
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
                                if (request.getRequest().getTerm() == null || anyMatch(stringsToSearch, term.text())) {
                                    TermInfo termInfo = new TermInfo();
                                    PostingsEnum docPosEnum = termsEnum.postings(null);
                                    SummaryStatistics stat = new SummaryStatistics();
                                    while (docPosEnum.nextDoc() != PostingsEnum.NO_MORE_DOCS) {
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
            } else {
                logger.warn("fields is null");
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


    private SortedMap<String, TermInfo> sortTotalFreq(final Map<String, TermInfo> map, Integer from, Integer size) {
        Comparator<String> comp = (t1, t2) -> {
            Long l1 = map.get(t1).getTotalFreq();
            String sl1 = Long.toString(l1);
            String s1 = sl1.length() + sl1 + t1;
            Long l2 = map.get(t2).getTotalFreq();
            String sl2 = Long.toString(l2);
            String s2 = sl2.length() + sl2 + t2;
            return -s1.compareTo(s2);
        };
        TreeMap<String, TermInfo> m = new TreeMap<>(comp);
        m.putAll(map);
        if (size != null && size > 0) {
            TreeMap<String, TermInfo> treeMap = new TreeMap<>(comp);
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
        Comparator<String> comp = (t1, t2) -> {
            Integer i1 = map.get(t1).getDocFreq();
            String si1 = Integer.toString(i1);
            String s1 = si1.length() + si1 + t1;
            Integer i2 = map.get(t2).getDocFreq();
            String si2 = Integer.toString(i2);
            String s2 = si2.length() + si2 + t2;
            return -s1.compareTo(s2);
        };
        TreeMap<String, TermInfo> m = new TreeMap<>(comp);
        m.putAll(map);
        if (size != null && size > 0) {
            TreeMap<String, TermInfo> treeMap = new TreeMap<>(comp);
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

    private Integer findFromLoc(Map<String, TermInfo> source, String term, Integer from, Integer size) {
        if (source.size() < size) {
            return from;
        }
        int position = 0;
        float highDistance = 0;
        LevensteinDistance distanceCalculator = new LevensteinDistance();
        Iterator<Map.Entry<String, TermInfo>> it = source.entrySet().iterator();
        for (int i = 0; i < source.size(); i++) {
            Map.Entry<String, TermInfo> entry = it.next();
            float currdis = distanceCalculator.getDistance(term, entry.getKey());
            if (currdis >= highDistance) {
                highDistance = currdis;
                position = i;
            }
        }
        return (position - (size / 2)) < 0 ? 0 : position - (size / 2);
    }

    private SortedMap<String, TermInfo> sortTerm(final Map<String, TermInfo> map, String term, Integer from,
                                                 Integer size, Integer backtrackingCount) {
        Comparator<String> comp = String::compareTo;
        TreeMap<String, TermInfo> m = new TreeMap<>(comp);
        m.putAll(map);
        if (size == null || size < 1) {
            return m;
        }
        if (backtrackingCount > 0) {
            from = findFromLoc(m, term, from, size);
        }
        TreeMap<String, TermInfo> treeMap = new TreeMap<String, TermInfo>(comp);
        int mapsize = m.size();
        for (int i = 0; i < mapsize; i++) {
            Map.Entry<String, TermInfo> me = m.pollFirstEntry();
            if (i >= from && i < from + size) {
                treeMap.put(me.getKey(), me.getValue());
            }
        }
        return treeMap;
    }


    private Map<String, TermInfo> truncate(Map<String, TermInfo> source, boolean sortByTerm, String term, Integer from, Integer size) {
        if (size == null || size < 1) {
            return source;
        }
        if (sortByTerm) {
            from = findFromLoc(source, term, from, size);
            logger.error("the from loc i got was " + from);
        }

        TreeMap<String, TermInfo> target = new TreeMap<String, TermInfo>();
        Iterator<Map.Entry<String, TermInfo>> it = source.entrySet().iterator();
        for (int i = 0; i < source.size(); i++) {
            Map.Entry<String, TermInfo> entry = it.next();
            if (from <= i && i < from + size) {
                target.put(entry.getKey(), entry.getValue());
            }
        }
        return target;
    }
}