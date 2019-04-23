package org.xbib.elasticsearch.plugin.termlist.action;

import org.elasticsearch.action.Action;
import org.elasticsearch.client.ElasticsearchClient;

public class TermlistAction extends Action<TermlistRequest, TermlistResponse, TermlistRequestBuilder> {

    public static final String NAME = "indices:admin/termlist";

    public static final TermlistAction INSTANCE = new TermlistAction();

    private TermlistAction() {
        super(NAME);
    }

    @Override
    public TermlistRequestBuilder newRequestBuilder(ElasticsearchClient client) {
        return new TermlistRequestBuilder(client);
    }

    @Override
    public TermlistResponse newResponse() {
        return new TermlistResponse();
    }

}
