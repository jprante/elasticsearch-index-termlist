package org.xbib.elasticsearch.action.termlist;

import org.elasticsearch.action.ClientAction;
import org.elasticsearch.client.Client;

public class TermlistAction extends ClientAction<TermlistRequest, TermlistResponse, TermlistRequestBuilder> {

    public static final TermlistAction INSTANCE = new TermlistAction();

    public static final String NAME = "indices/termlist";

    private TermlistAction() {
        super(NAME);
    }

    @Override
    public TermlistResponse newResponse() {
        return new TermlistResponse();
    }

    @Override
    public TermlistRequestBuilder newRequestBuilder(Client client) {
        return new TermlistRequestBuilder(client);
    }
}
