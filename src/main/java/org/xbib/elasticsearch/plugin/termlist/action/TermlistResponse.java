package org.xbib.elasticsearch.plugin.termlist.action;

import org.elasticsearch.action.support.broadcast.BroadcastResponse;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.common.xcontent.StatusToXContentObject;
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.rest.RestStatus;
import org.xbib.elasticsearch.plugin.termlist.common.CompactHashMap;

import java.io.IOException;
import java.util.Map;

import static org.elasticsearch.rest.RestStatus.OK;

public class TermlistResponse extends BroadcastResponse implements StatusToXContentObject {

    private int numdocs;

    private Map<String, TermInfo> map;

    public TermlistResponse() {
    }

    public TermlistResponse(int numdocs, Map<String, TermInfo> map) {
        this.numdocs = numdocs;
        this.map = map;
    }

    public TermlistResponse setNumDocs(int numdocs) {
        this.numdocs = numdocs;
        return this;
    }

    public int getNumDocs() {
        return numdocs;
    }

    public Map<String, TermInfo> getTermlist() {
        return map;
    }

    public TermlistResponse setTermlist(Map<String, TermInfo> map) {
        this.map = map;
        return this;
    }

    @Override
    public void readFrom(StreamInput in) throws IOException {
        super.readFrom(in);
        numdocs = in.readInt();
        int n = in.readInt();
        map = new CompactHashMap<>();
        for (int i = 0; i < n; i++) {
            String text = in.readString();
            TermInfo t = new TermInfo();
            t.readFrom(in);
            map.put(text, t);
        }
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        super.writeTo(out);
        out.writeInt(numdocs);
        out.writeInt(map.size());
        for (Map.Entry<String, TermInfo> t : map.entrySet()) {
            out.writeString(t.getKey());
            t.getValue().writeTo(out);
        }
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
       builder.startObject();
       // builder.field("took", (System.nanoTime() - t0) / 1000000);
        builder.field("numdocs", this.getNumDocs());
        builder.field("numterms", this.getTermlist().size());
        builder.startArray("terms");
        for (Map.Entry<String, TermInfo> t : this.getTermlist().entrySet()) {
            builder.startObject().field("term", t.getKey());
            t.getValue().toXContent(builder, ToXContent.EMPTY_PARAMS);
            builder.endObject();
        }
        builder.endArray();
        builder.endObject();
        return builder;
    }


    public RestStatus status() {
        return OK;
    }
}
