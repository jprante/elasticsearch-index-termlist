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
import org.elasticsearch.action.ShardOperationFailedException;
import org.elasticsearch.action.support.broadcast.BroadcastOperationResponse;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;

/**
 * A response for termlist action.
 *
 */
public class TermlistResponse extends BroadcastOperationResponse {

    private Set<String> termlist;

    TermlistResponse() {
    }

    TermlistResponse(int totalShards, int successfulShards, int failedShards, List<ShardOperationFailedException> shardFailures, Set<String> termlist) {
        super(totalShards, successfulShards, failedShards, shardFailures);
        this.termlist = termlist;
    }

    public Set<String> getTermlist() {
        return termlist;
    }
    
    @Override
    public void readFrom(StreamInput in) throws IOException {
        super.readFrom(in);
       int n = in.readInt();
        termlist = new CompactHashSet();
        for (int i = 0; i <n; i++) {
            termlist.add(in.readUTF());
        }
     }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        super.writeTo(out);
        out.writeInt(termlist.size());
        for (String t : termlist) {
            out.writeUTF(t);
        }
    }
}