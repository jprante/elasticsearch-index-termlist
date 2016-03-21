package org.xbib.elasticsearch.action.termlist;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.common.io.stream.Streamable;
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.xbib.elasticsearch.common.termlist.math.SummaryStatistics;

import java.io.IOException;

public class TermInfo implements Streamable, ToXContent {

    private Integer docFreq;

    private Long totalFreq;

    private SummaryStatistics summaryStatistics;

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

    public TermInfo setSummaryStatistics(SummaryStatistics stat) {
        this.summaryStatistics = stat;
        return this;
    }

    public SummaryStatistics getSummaryStatistics() {
        return summaryStatistics;
    }

    @Override
    public void readFrom(StreamInput in) throws IOException {
        boolean b = in.readBoolean();
        if (b) {
            setDocFreq(in.readInt());
        }
        b = in.readBoolean();
        if (b) {
            setTotalFreq(in.readVLong());
        }
        summaryStatistics = new SummaryStatistics();
        summaryStatistics.readFrom(in);
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
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
        summaryStatistics.writeTo(out);
    }

    public String toString() {
        try {
            XContentBuilder builder = XContentFactory.jsonBuilder();
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
        if (totalFreq != null) {
            builder.field("totalfreq", totalFreq);
        }
        if (docFreq != null) {
            builder.field("docfreq", docFreq);
        }
        if (summaryStatistics != null) {
            summaryStatistics.toXContent(builder, params);
        }
        return builder;
    }
}
