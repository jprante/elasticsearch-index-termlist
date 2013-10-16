
package org.xbib.elasticsearch.action.termlist;

import org.elasticsearch.action.ShardOperationFailedException;
import org.elasticsearch.action.support.broadcast.BroadcastOperationResponse;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;

import java.io.IOException;
import java.util.List;
import java.util.Set;

/**
 * A response for termlist action.
 */
public class TermlistResponse extends BroadcastOperationResponse {

    private Set<String> termlist;

    TermlistResponse() {
    }

    TermlistResponse(int totalShards, int successfulShards, int failedShards, List<ShardOperationFailedException> shardFailures, Set<String> termlist) {
        super(totalShards, successfulShards, failedShards, shardFailures);
        this.termlist = termlist;
    }

    public Set<String> getTermlist() {
        return termlist;
    }

    @Override
    public void readFrom(StreamInput in) throws IOException {
        super.readFrom(in);
        int n = in.readInt();
        termlist = new CompactHashSet<String>();
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