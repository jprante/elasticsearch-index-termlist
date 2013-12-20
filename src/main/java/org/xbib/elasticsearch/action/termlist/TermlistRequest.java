
package org.xbib.elasticsearch.action.termlist;

import org.elasticsearch.action.support.broadcast.BroadcastOperationRequest;
import org.elasticsearch.action.support.broadcast.BroadcastOperationThreading;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;

import java.io.IOException;

public class TermlistRequest extends BroadcastOperationRequest<TermlistRequest> {

    private String field;

    private Integer size;

    private boolean withDocFreq;

    private boolean withTotalFreq;

    TermlistRequest() {
    }

    public TermlistRequest(String... indices) {
        super(indices);
        operationThreading(BroadcastOperationThreading.THREAD_PER_SHARD);
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
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        super.writeTo(out);
        out.writeString(field);
    }
}
