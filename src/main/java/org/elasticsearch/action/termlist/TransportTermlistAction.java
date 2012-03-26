/*
 * Licensed to ElasticSearch and Shay Banon under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. ElasticSearch licenses this
 * file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.elasticsearch.action.termlist;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReferenceArray;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermEnum;
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
import static org.elasticsearch.common.collect.Lists.newArrayList;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.index.engine.Engine;
import org.elasticsearch.index.shard.service.IndexShard;
import org.elasticsearch.indices.IndicesService;
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.transport.TransportService;

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
    protected boolean ignoreNonActiveExceptions() {
        return true;
    }

    @Override
    protected TermlistResponse newResponse(TermlistRequest request, AtomicReferenceArray shardsResponses, ClusterState clusterState) {
        int successfulShards = 0;
        int failedShards = 0;
        List<ShardOperationFailedException> shardFailures = null;
        Set<String> termlist = new CompactHashSet();
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
            IndexShard indexShard = indicesService.indexServiceSafe(request.index()).shardSafe(request.shardId());
            Engine.Searcher searcher = indexShard.searcher();
            try {
                Set<String> set = new CompactHashSet();
                TermEnum te = searcher.reader().terms();
                do {
                    Term t = te.term();
                    if (t != null && t.field().charAt(0) != '_') {
                        if (request.getField() == null || t.field().equals(request.getField())) {
                            set.add(t.text());
                        }
                    }
                } while (te.next());
                te.close();
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