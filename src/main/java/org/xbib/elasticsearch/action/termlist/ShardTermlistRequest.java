package org.xbib.elasticsearch.action.termlist;

import org.elasticsearch.action.support.broadcast.BroadcastShardOperationRequest;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.index.shard.ShardId;

import java.io.IOException;

class ShardTermlistRequest extends BroadcastShardOperationRequest {

    private String index;

    private TermlistRequest request;

    ShardTermlistRequest() {
    }

    public ShardTermlistRequest(String index, ShardId shardId, TermlistRequest request) {
        super(shardId, request);
        this.index = index;
        this.request = request;
    }

    public String getIndex() {
        return index;
    }

    public TermlistRequest getRequest() {
        return request;
    }

    @Override
    public void readFrom(StreamInput in) throws IOException {
        super.readFrom(in);
        index = in.readString();
        request = TermlistRequest.from(in);
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        super.writeTo(out);
        out.writeString(index);
        request.writeTo(out);
    }
}