package org.xbib.elasticsearch.plugin.termlist.action;

import org.elasticsearch.action.ActionRequestBuilder;
import org.elasticsearch.client.ElasticsearchClient;

public class TermlistRequestBuilder extends ActionRequestBuilder<TermlistRequest, TermlistResponse, TermlistRequestBuilder> {

    public TermlistRequestBuilder(ElasticsearchClient client) {
        super(client, TermlistAction.INSTANCE, new TermlistRequest());
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


    public TermlistRequestBuilder sortByDocFreq(boolean sortByDocFreq) {
        request.sortByDocFreq(sortByDocFreq);
        return this;
    }

    public TermlistRequestBuilder sortByTotalFreq(boolean sortByTotalFreq) {
        request.sortByTotalFreq(sortByTotalFreq);
        return this;
    }

}
