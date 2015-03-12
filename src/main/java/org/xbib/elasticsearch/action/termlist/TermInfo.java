package org.xbib.elasticsearch.action.termlist;

import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.common.io.stream.Streamable;

import java.io.IOException;

public class TermInfo implements Streamable {

    private Integer termFreq;

    private Integer docCount;

    private Integer docFreq;

    private Long totalFreq;

    public TermInfo setTermFreq(int termFreq) {
        this.termFreq = termFreq;
        return this;
    }

    public Integer getTermFreq() {
        return termFreq;
    }

    public TermInfo setDocCount(int docCount) {
        this.docCount = docCount;
        return this;
    }

    public Integer getDocCount() {
        return docCount;
    }

    public TermInfo setDocFreq(int docFreq) {
        this.docFreq = docFreq;
        return this;
    }

    public Integer getDocFreq() {
        return docFreq;
    }

    public TermInfo setTotalFreq(long totalFreq) {
        this.totalFreq = totalFreq;
        return this;
    }

    public Long getTotalFreq() {
        return totalFreq;
    }

    @Override
    public void readFrom(StreamInput in) throws IOException {
        boolean b = in.readBoolean();
        if (b) {
            setTermFreq(in.readInt());
        }
        b = in.readBoolean();
        if (b) {
            setDocCount(in.readInt());
        }
        b = in.readBoolean();
        if (b) {
            setDocFreq(in.readInt());
        }
        b = in.readBoolean();
        if (b) {
            setTotalFreq(in.readVLong());
        }
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        if (termFreq != null) {
            out.writeBoolean(true);
            out.writeInt(termFreq);
        } else {
            out.writeBoolean(false);
        }
        if (docCount != null) {
            out.writeBoolean(true);
            out.writeInt(docCount);
        } else {
            out.writeBoolean(false);
        }
        if (docFreq != null) {
            out.writeBoolean(true);
            out.writeInt(docFreq);
        } else {
            out.writeBoolean(false);
        }
        if (totalFreq != null) {
            out.writeBoolean(true);
            out.writeVLong(totalFreq);
        } else {
            out.writeBoolean(false);
        }
    }
}
