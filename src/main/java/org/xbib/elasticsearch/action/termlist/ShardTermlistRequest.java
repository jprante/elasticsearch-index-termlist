package org.xbib.elasticsearch.action.termlist;

import org.elasticsearch.action.support.broadcast.BroadcastShardOperationRequest;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;

import java.io.IOException;

class ShardTermlistRequest extends BroadcastShardOperationRequest {

    private String field;

    private String term;

    private Integer from;

    private Integer size;

    private boolean withDocFreq;

    private boolean withTotalFreq;

    ShardTermlistRequest() {
    }

    public ShardTermlistRequest(String index, int shardId, TermlistRequest request) {
        super(index, shardId, request);
        this.field = request.getField();
        this.term = request.getTerm();
        this.from = request.getFrom();
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

    public void setTerm(String term) {
        this.term = term;
    }

    public String getTerm() {
        return term;
    }

    public void setFrom(Integer from) {
        this.from = from;
    }

    public Integer getFrom() {
        return from;
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
        term = in.readString();
        from = in.readInt();
        size = in.readInt();
        withDocFreq = in.readBoolean();
        withTotalFreq = in.readBoolean();
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        super.writeTo(out);
        out.writeString(field);
        out.writeString(term);
        out.writeInt(from);
        out.writeInt(size);
        out.writeBoolean(withDocFreq);
        out.writeBoolean(withTotalFreq);
    }
}