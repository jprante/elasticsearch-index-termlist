
package org.xbib.elasticsearch.action.termlist;

import org.elasticsearch.action.support.broadcast.BroadcastShardOperationResponse;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;

import java.io.IOException;
import java.util.Map;

class ShardTermlistResponse extends BroadcastShardOperationResponse {

    private Map<String,TermInfo> map;

    ShardTermlistResponse() {
    }

    public ShardTermlistResponse(String index, int shardId, Map<String,TermInfo> map) {
        super(index, shardId);
        this.map = map;
    }

    public Map<String,TermInfo> getTermList() {
        return map;
    }

    @Override
    public void readFrom(StreamInput in) throws IOException {
        super.readFrom(in);
        int n = in.readInt();
        map = new CompactHashMap<String,TermInfo>();
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
        out.writeInt(map.size());
        for (Map.Entry<String,TermInfo> t : map.entrySet()) {
            out.writeString(t.getKey());
            t.getValue().writeTo(out);
        }
    }
}