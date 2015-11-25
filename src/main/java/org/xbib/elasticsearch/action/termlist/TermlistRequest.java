package org.xbib.elasticsearch.action.termlist;

import java.io.IOException;

import org.elasticsearch.action.support.broadcast.BroadcastOperationRequest;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;

public class TermlistRequest extends BroadcastOperationRequest<TermlistRequest> {

    private String field;

    private String term;

    private Integer from;

    private Integer size = -1;

    private boolean withTermFreq;

    private boolean withDocCount;

    private boolean withDocFreq;

    private boolean withTotalFreq;

    private boolean sortByTerm;

    private boolean sortByDocFreq;

    private boolean sortByTotalFreq;

    private int minDocFreq = 1;

    private int minTotalFreq = 1;

    TermlistRequest() {
    }

    public TermlistRequest(String... indices) {
        super(indices);
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

    public void sortByTerm(boolean sortByTerm) {
        this.sortByTerm = sortByTerm;
    }

    public boolean sortByTerm() {
        return sortByTerm;
    }

    public void sortByDocFreq(boolean sortByDocFreq) {
        this.sortByDocFreq = sortByDocFreq;
    }

    public boolean sortByDocFreq() {
        return sortByDocFreq;
    }

    public void sortByTotalFreq(boolean sortByTotalFreq) {
        this.sortByTotalFreq = sortByTotalFreq;
    }

    public boolean sortByTotalFreq() {
        return sortByTotalFreq;
    }

    public int getMinDocFreq() {
        return minDocFreq;
    }

    public void setMinDocFreq(int minDocFreq) {
        this.minDocFreq = minDocFreq;
    }

    public int getMinTotalFreq() {
        return minTotalFreq;
    }

    public void setMinTotalFreq(int minTotalFreq) {
        this.minTotalFreq = minTotalFreq;
    }

    static TermlistRequest from(StreamInput in) throws IOException {
        TermlistRequest request = new TermlistRequest();
        request.readFrom(in);
        return request;
    }

    @Override
    public void readFrom(StreamInput in) throws IOException {
        super.readFrom(in);
        field = in.readOptionalString();
        term = in.readOptionalString();
        from = in.readInt();
        size = in.readInt();
        withTermFreq = in.readBoolean();
        withDocCount = in.readBoolean();
        withDocFreq = in.readBoolean();
        withTotalFreq = in.readBoolean();
        sortByTerm = in.readBoolean();
        sortByDocFreq = in.readBoolean();
        sortByTotalFreq = in.readBoolean();

    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        super.writeTo(out);
        out.writeOptionalString(field);
        out.writeOptionalString(term);
        out.writeInt(from);
        out.writeInt(size);
        out.writeBoolean(withTermFreq);
        out.writeBoolean(withDocCount);
        out.writeBoolean(withDocFreq);
        out.writeBoolean(withTotalFreq);
        out.writeBoolean(sortByTerm);
        out.writeBoolean(sortByDocFreq);
        out.writeBoolean(sortByTotalFreq);
    }
}
