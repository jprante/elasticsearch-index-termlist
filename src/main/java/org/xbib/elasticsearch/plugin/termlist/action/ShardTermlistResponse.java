package org.xbib.elasticsearch.plugin.termlist.action;

import org.elasticsearch.action.support.broadcast.BroadcastShardResponse;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.index.shard.ShardId;
import org.xbib.elasticsearch.plugin.termlist.common.CompactHashMap;

import java.io.IOException;
import java.util.Map;

class ShardTermlistResponse extends BroadcastShardResponse {

    private String index;

    private int numDocs;

    private Map<String, TermInfo> map;

    ShardTermlistResponse() {
    }

    ShardTermlistResponse(String index, ShardId shardId, int numDocs, Map<String, TermInfo> map) {
        super(shardId);
        this.numDocs = numDocs;
        this.index = index;
        this.map = map;
    }

    public String getIndex() {
        return index;
    }

    public int getNumDocs() {
        return numDocs;
    }

    public Map<String, TermInfo> getTermList() {
        return map;
    }

    @Override
    public void readFrom(StreamInput in) throws IOException {
        super.readFrom(in);
        numDocs = in.readInt();
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
        out.writeInt(numDocs);
        out.writeInt(map.size());
        for (Map.Entry<String, TermInfo> t : map.entrySet()) {
            out.writeString(t.getKey());
            t.getValue().writeTo(out);
        }
    }
}