
package org.xbib.elasticsearch.action.termlist;

import org.apache.lucene.index.Fields;
import org.apache.lucene.index.MultiFields;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.util.BytesRef;

import org.elasticsearch.ElasticSearchException;
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

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReferenceArray;

import static org.elasticsearch.common.collect.Lists.newArrayList;

/**
 * Termlist index/indices action.
 */
public class TransportTermlistAction
        extends TransportBroadcastOperationAction<TermlistRequest, TermlistResponse, ShardTermlistRequest, ShardTermlistResponse> {

    private final IndicesService indicesService;
    private final Object termlistMutex = new Object();

    @Inject
    public TransportTermlistAction(Settings settings, ThreadPool threadPool, ClusterService clusterService,
                                   TransportService transportService, IndicesService indicesService) {
        super(settings, threadPool, clusterService, transportService);
        this.indicesService = indicesService;
    }

    @Override
    protected String executor() {
        return ThreadPool.Names.MERGE;
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
        Set<String> termlist = new CompactHashSet<String>();
        for (int i = 0; i < shardsResponses.length(); i++) {
            Object shardResponse = shardsResponses.get(i);
            if (shardResponse == null) {
                // a non active shard, ignore...
            } else if (shardResponse instanceof BroadcastShardOperationFailedException) {
                failedShards++;
                if (shardFailures == null) {
                    shardFailures = newArrayList();
                }
                shardFailures.add(new DefaultShardOperationFailedException((BroadcastShardOperationFailedException) shardResponse));
            } else {
                successfulShards++;
                if (shardResponse instanceof ShardTermlistResponse) {
                    ShardTermlistResponse resp = (ShardTermlistResponse) shardResponse;
                    termlist.addAll(resp.getTermList());
                }
            }
        }
        return new TermlistResponse(shardsResponses.length(), successfulShards, failedShards, shardFailures, termlist);
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

    @Override
    protected ShardTermlistResponse shardOperation(ShardTermlistRequest request) throws ElasticSearchException {
        synchronized (termlistMutex) {
            InternalIndexShard indexShard = (InternalIndexShard) indicesService.indexServiceSafe(request.index()).shardSafe(request.shardId());
            indexShard.store().directory();
            Engine.Searcher searcher = indexShard.acquireSearcher();
            try {
                Set<String> set = new CompactHashSet<String>();
                Fields fields = MultiFields.getFields(searcher.reader());
                if (fields != null) {
                    for (Iterator<String> it = fields.iterator(); it.hasNext(); ) {
                        String field = it.next();
                        if (field.charAt(0) == '_') {
                            continue;
                        }
                        if (request.getField() == null || field.equals(request.getField())) {
                            Terms terms = fields.terms(field);
                            if (terms != null) {
                                TermsEnum termsEnum = terms.iterator(null);
                                BytesRef text;
                                while ((text = termsEnum.next()) != null) {
                                    set.add(text.utf8ToString());
                                    System.out.println("field=" + field + "; text=" + text.utf8ToString());
                                }
                            }
                        }
                    }
                }
                return new ShardTermlistResponse(request.index(), request.shardId(), set);
            } catch (IOException ex) {
                throw new ElasticSearchException(ex.getMessage(), ex);
            }
        }
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
}