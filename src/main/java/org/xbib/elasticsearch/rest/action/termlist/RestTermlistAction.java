package org.xbib.elasticsearch.rest.action.termlist;

import org.elasticsearch.client.Client;
import org.elasticsearch.common.Strings;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.rest.BaseRestHandler;
import org.elasticsearch.rest.BytesRestResponse;
import org.elasticsearch.rest.RestChannel;
import org.elasticsearch.rest.RestController;
import org.elasticsearch.rest.RestRequest;
import org.elasticsearch.rest.RestResponse;
import org.elasticsearch.rest.RestStatus;
import org.elasticsearch.rest.action.support.RestBuilderListener;
import org.elasticsearch.rest.action.support.RestStatusToXContentListener;
import org.xbib.elasticsearch.action.termlist.TermInfo;
import org.xbib.elasticsearch.action.termlist.TermlistAction;
import org.xbib.elasticsearch.action.termlist.TermlistRequest;
import org.xbib.elasticsearch.action.termlist.TermlistResponse;

import java.util.Map;

import static org.elasticsearch.rest.RestRequest.Method.GET;
import static org.elasticsearch.rest.action.support.RestActions.buildBroadcastShardsHeader;

public class RestTermlistAction extends BaseRestHandler {

    @Inject
    public RestTermlistAction(Settings settings, Client client, RestController controller) {
        super(settings, controller, client);
        controller.registerHandler(GET, "/_termlist", this);
        controller.registerHandler(GET, "/{index}/_termlist", this);
    }


    public void handleRequest(final RestRequest request, final RestChannel channel, final Client client) {
        try {
            TermlistRequest termlistRequest = new TermlistRequest(/*Strings.splitStringByCommaToArray(request.param("index"))*/);
            termlistRequest.setField(request.param("field"));
            termlistRequest.setTerm(request.param("term"));
            termlistRequest.setFrom(request.paramAsInt("from", 0));
            termlistRequest.setSize(request.paramAsInt("size", -1));
            termlistRequest.sortByDocFreq(request.paramAsBoolean("sortbydocfreqs", false));
            termlistRequest.sortByTotalFreq(request.paramAsBoolean("sortbytotalfreqs", false));
            termlistRequest.sortByTerm(request.paramAsBoolean("sortbyterms", false));
            termlistRequest.setMinDocFreq(request.paramAsInt("minDocFreq", 1));
            termlistRequest.setMinTotalFreq(request.paramAsInt("minTotalFreq", 1));
            final long t0 = System.nanoTime();

            client.execute(TermlistAction.INSTANCE, termlistRequest, new RestBuilderListener<TermlistResponse>(channel) {
                @Override
                public RestResponse buildResponse(TermlistResponse response, XContentBuilder builder) throws Exception {
                    builder.startObject();
                    buildBroadcastShardsHeader(builder,request, response);
                    builder.field("took", (System.nanoTime() - t0) / 1000000);
                    builder.field("numdocs", response.getNumDocs());
                    builder.field("numterms", response.getTermlist().size());
                    builder.startArray("terms");
                    for (Map.Entry<String, TermInfo> t : response.getTermlist().entrySet()) {
                        builder.startObject().field("term", t.getKey());
                        t.getValue().toXContent(builder, ToXContent.EMPTY_PARAMS);
                        builder.endObject();
                    }
                    builder.endArray();
                    builder.endObject();
                    return new BytesRestResponse(RestStatus.OK, builder);
                }
            });

        } catch (Throwable t) {
            logger.error(t.getMessage(), t);
            channel.sendResponse(new BytesRestResponse(RestStatus.INTERNAL_SERVER_ERROR, t.getMessage()));
        }
    }
}