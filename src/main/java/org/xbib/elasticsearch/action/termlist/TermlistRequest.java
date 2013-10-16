
package org.xbib.elasticsearch.action.termlist;

import org.elasticsearch.action.support.broadcast.BroadcastOperationRequest;
import org.elasticsearch.action.support.broadcast.BroadcastOperationThreading;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;

import java.io.IOException;

public class TermlistRequest extends BroadcastOperationRequest<TermlistRequest> {

    private String field;

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
