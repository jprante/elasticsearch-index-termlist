
package org.xbib.elasticsearch.action.termlist;

import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.support.broadcast.BroadcastOperationRequestBuilder;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.internal.InternalGenericClient;

/**
 * A request to get termlists of one or more indices.
 */
public class TermlistRequestBuilder extends BroadcastOperationRequestBuilder<TermlistRequest, TermlistResponse, TermlistRequestBuilder> {

    public TermlistRequestBuilder(InternalGenericClient client) {
        super(client, new TermlistRequest());
    }

    public TermlistRequestBuilder withDocFreq() {
        request.setWithDocFreq(true);
        return this;
    }

    public TermlistRequestBuilder withTotalFreq() {
        request.setWithTotalFreq(true);
        return this;
    }

    @Override
    protected void doExecute(ActionListener<TermlistResponse> listener) {
        ((Client) client).execute(TermlistAction.INSTANCE, request, listener);
    }
}
