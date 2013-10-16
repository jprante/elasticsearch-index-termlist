
package org.xbib.elasticsearch.action.termlist;

import org.elasticsearch.action.support.broadcast.BroadcastShardOperationResponse;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;

import java.io.IOException;
import java.util.Set;

class ShardTermlistResponse extends BroadcastShardOperationResponse {

    private Set<String> termlist;

    ShardTermlistResponse() {
    }

    public ShardTermlistResponse(String index, int shardId, Set<String> termlist) {
        super(index, shardId);
        this.termlist = termlist;
    }

    public Set<String> getTermList() {
        return termlist;
    }

    @Override
    public void readFrom(StreamInput in) throws IOException {
        super.readFrom(in);
        int n = in.readInt();
        termlist = new CompactHashSet<String>(n);
        for (int i = 0; i < n; i++) {
            termlist.add(in.readString());
        }
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        super.writeTo(out);
        out.writeInt(termlist.size());
        for (String t : termlist) {
            out.writeString(t);
        }
    }
}