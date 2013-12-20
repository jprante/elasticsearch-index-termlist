package org.xbib.elasticsearch.action.termlist;

import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.common.io.stream.Streamable;

import java.io.IOException;

public class TermInfo implements Streamable {

    Integer docFreq;

    Long totalFreq;

    public TermInfo docfreq(int docFreq) {
        this.docFreq = docFreq;
        return this;
    }

    public Integer getDocFreq() {
        return docFreq;
    }

    public TermInfo totalFreq(long totalFreq) {
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
            docfreq(in.readInt());
        }
        b = in.readBoolean();
        if (b) {
            totalFreq(in.readVLong());
        }
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        Integer i = getDocFreq();
        if (i != null) {
            out.writeBoolean(true);
            out.writeInt(i);
        } else {
            out.writeBoolean(false);
        }
        Long l = getTotalFreq();
        if (l != null) {
            out.writeBoolean(true);
            out.writeVLong(l);
        } else {
            out.writeBoolean(false);
        }
    }
}
