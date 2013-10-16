
package org.xbib.elasticsearch.action.termlist;

import org.elasticsearch.action.support.broadcast.BroadcastShardOperationRequest;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;

import java.io.IOException;

class ShardTermlistRequest extends BroadcastShardOperationRequest {

    private String field;

    ShardTermlistRequest() {
    }

    public ShardTermlistRequest(String index, int shardId, TermlistRequest request) {
        super(index, shardId, request);
        this.field = request.getField();
    }

    public void setField(String field) {
        this.field = field;
    }

    public String getField() {
        return field;
    }

    @Override
    public void readFrom(StreamInput in) throws IOException {
        super.readFrom(in);
        field = in.readString();
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        super.writeTo(out);
        out.writeString(field);
    }
}