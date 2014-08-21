package org.xbib.elasticsearch.action.termlist;

import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.support.broadcast.BroadcastOperationRequestBuilder;
import org.elasticsearch.client.Client;

/**
 * A request to get termlists of one or more indices.
 */
public class TermlistRequestBuilder extends BroadcastOperationRequestBuilder<TermlistRequest, TermlistResponse, TermlistRequestBuilder, Client> {

    public TermlistRequestBuilder(Client client) {
        super(client, new TermlistRequest());
    }

    public TermlistRequestBuilder setField(String field) {
        request.setField(field);
        return this;
    }

    public TermlistRequestBuilder setTerm(String term) {
        request.setTerm(term);
        return this;
    }

    public TermlistRequestBuilder setFrom(Integer from) {
        request.setFrom(from);
        return this;
    }

    public TermlistRequestBuilder setSize(Integer size) {
        request.setSize(size);
        return this;
    }

    public TermlistRequestBuilder withDocFreq() {
        request.setWithDocFreq(true);
        return this;
    }

    public TermlistRequestBuilder withTotalFreq() {
        request.setWithTotalFreq(true);
        return this;
    }

    public TermlistRequestBuilder sortByDocFreq(boolean sortByDocFreq) {
        request.sortByDocFreq(sortByDocFreq);
        return this;
    }

    public TermlistRequestBuilder sortByTotalFreq(boolean sortByTotalFreq) {
        request.sortByTotalFreq(sortByTotalFreq);
        return this;
    }


    @Override
    protected void doExecute(ActionListener<TermlistResponse> listener) {
        client.execute(TermlistAction.INSTANCE, request, listener);
    }
}
