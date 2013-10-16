
package org.xbib.elasticsearch.action.termlist;

import org.elasticsearch.action.Action;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.internal.InternalGenericClient;

public class TermlistAction extends Action<TermlistRequest, TermlistResponse, TermlistRequestBuilder> {

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
        return new TermlistRequestBuilder((InternalGenericClient) client);
    }
}
