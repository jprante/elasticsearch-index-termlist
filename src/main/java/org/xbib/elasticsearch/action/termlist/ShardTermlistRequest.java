package org.xbib.elasticsearch.action.termlist;

import org.elasticsearch.action.support.broadcast.BroadcastShardOperationRequest;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;

import java.io.IOException;

class ShardTermlistRequest extends BroadcastShardOperationRequest {

    private String field;

    private Integer size;

    private boolean withDocFreq;

    private boolean withTotalFreq;

    ShardTermlistRequest() {
    }

    public ShardTermlistRequest(String index, int shardId, TermlistRequest request) {
        super(index, shardId, request);
        this.field = request.getField();
        this.size = request.getSize();
        this.withDocFreq = request.getWithDocFreq();
        this.withTotalFreq = request.getWithTotalFreq();
    }

    public void setField(String field) {
        this.field = field;
    }

    public String getField() {
        return field;
    }

    public void setSize(Integer size) {
        this.size = size;
    }

    public Integer getSize() {
        return size;
    }

    public void setWithDocFreq(boolean withDocFreq) {
        this.withDocFreq = withDocFreq;
    }

    public boolean getWithDocFreq() {
        return withDocFreq;
    }

    public void setWithTotalFreq(boolean withTotalFreq) {
        this.withTotalFreq = withTotalFreq;
    }

    public boolean getWithTotalFreq() {
        return withTotalFreq;
    }

    @Override
    public void readFrom(StreamInput in) throws IOException {
        super.readFrom(in);
        field = in.readString();
        withDocFreq = in.readBoolean();
        withTotalFreq = in.readBoolean();
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        super.writeTo(out);
        out.writeString(field);
        out.writeBoolean(withDocFreq);
        out.writeBoolean(withTotalFreq);
    }
}