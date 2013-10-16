
package org.xbib.elasticsearch.rest.action.termlist;

import org.elasticsearch.action.ActionListener;
import org.xbib.elasticsearch.action.termlist.TermlistAction;
import org.xbib.elasticsearch.action.termlist.TermlistRequest;
import org.xbib.elasticsearch.action.termlist.TermlistResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.Strings;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.rest.BaseRestHandler;
import org.elasticsearch.rest.RestChannel;
import org.elasticsearch.rest.RestController;
import org.elasticsearch.rest.RestRequest;
import org.elasticsearch.rest.XContentRestResponse;
import org.elasticsearch.rest.XContentThrowableRestResponse;
import org.elasticsearch.rest.action.support.RestXContentBuilder;

import java.io.IOException;

import static org.elasticsearch.rest.RestRequest.Method.GET;
import static org.elasticsearch.rest.RestRequest.Method.POST;
import static org.elasticsearch.rest.RestStatus.OK;
import static org.elasticsearch.rest.action.support.RestActions.buildBroadcastShardsHeader;

public class RestTermlistAction extends BaseRestHandler {

    @Inject
    public RestTermlistAction(Settings settings, Client client, RestController controller) {
        super(settings, client);
        controller.registerHandler(POST, "/_termlist", this);
        controller.registerHandler(POST, "/{index}/_termlist", this);
        controller.registerHandler(POST, "/_termlist/{field}", this);
        controller.registerHandler(POST, "/{index}/_termlist/{field}", this);
        controller.registerHandler(GET, "/_termlist", this);
        controller.registerHandler(GET, "/{index}/_termlist", this);
        controller.registerHandler(GET, "/_termlist/{field}", this);
        controller.registerHandler(GET, "/{index}/_termlist/{field}", this);
    }

    public void handleRequest(final RestRequest request, final RestChannel channel) {
        TermlistRequest termlistRequest = new TermlistRequest(
                Strings.splitStringByCommaToArray(request.param("index")));
        termlistRequest.setField(request.param("field"));
        client.execute(TermlistAction.INSTANCE, termlistRequest, new ActionListener<TermlistResponse>() {

            public void onResponse(TermlistResponse response) {
                try {
                    XContentBuilder builder = RestXContentBuilder.restContentBuilder(request);
                    builder.startObject();
                    builder.field("ok", true);
                    buildBroadcastShardsHeader(builder, response);
                    builder.array("terms", response.getTermlist().toArray());
                    builder.endObject();
                    channel.sendResponse(new XContentRestResponse(request, OK, builder));
                } catch (Exception e) {
                    onFailure(e);
                }
            }

            public void onFailure(Throwable e) {
                try {
                    channel.sendResponse(new XContentThrowableRestResponse(request, e));
                } catch (IOException e1) {
                    logger.error("Failed to send failure response", e1);
                }
            }
        });
    }
}