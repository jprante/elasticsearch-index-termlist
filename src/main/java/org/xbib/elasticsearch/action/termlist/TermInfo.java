package org.xbib.elasticsearch.action.termlist;

import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.common.io.stream.Streamable;
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;

import java.io.IOException;

public class TermInfo implements Streamable, ToXContent {

    private Integer termFreq;

    private Integer docFreq;

    private Long totalFreq;

    private Double tfidf;

    public TermInfo setTermFreq(int termFreq) {
        this.termFreq = termFreq;
        return this;
    }

    public Integer getTermFreq() {
        return termFreq;
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

    public TermInfo setTfIdf(double tfidf) {
        this.tfidf = tfidf;
        return this;
    }

    public Double getTfIdf() {
        return tfidf;
    }

    @Override
    public void readFrom(StreamInput in) throws IOException {
        boolean b = in.readBoolean();
        if (b) {
            setTermFreq(in.readInt());
        }
        b = in.readBoolean();
        if (b) {
            setDocFreq(in.readInt());
        }
        b = in.readBoolean();
        if (b) {
            setTotalFreq(in.readVLong());
        }
        b = in.readBoolean();
        if (b) {
            setTfIdf(in.readDouble());
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
        if (tfidf != null) {
            out.writeBoolean(true);
            out.writeDouble(tfidf);
        } else {
            out.writeBoolean(false);
        }
    }

    public String toString() {
        try {
            XContentBuilder builder = XContentFactory.jsonBuilder().prettyPrint();
            builder.startObject();
            toXContent(builder, EMPTY_PARAMS);
            builder.endObject();
            return builder.string();
        } catch (IOException e) {
            return "{ \"error\" : \"" + e.getMessage() + "\"}";
        }
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        if (termFreq != null) {
            builder.field("termFreq", termFreq);
        }
        if (docFreq != null) {
            builder.field("docFreq", docFreq);
        }
        if (totalFreq != null) {
            builder.field("totalFreq", totalFreq);
        }
        if (tfidf != null) {
            builder.field("tfidf", tfidf);
        }
        return builder;
    }
}
