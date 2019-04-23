package org.xbib.elasticsearch.plugin.termlist.rest;

import org.elasticsearch.client.node.NodeClient;
import org.elasticsearch.common.Strings;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.rest.BaseRestHandler;
import org.elasticsearch.rest.RestController;
import org.elasticsearch.rest.RestRequest;
import org.elasticsearch.rest.action.RestStatusToXContentListener;
import org.xbib.elasticsearch.plugin.termlist.action.TermlistAction;
import org.xbib.elasticsearch.plugin.termlist.action.TermlistRequest;

import java.io.IOException;

import static org.elasticsearch.rest.RestRequest.Method.GET;

public class RestTermlistAction extends BaseRestHandler {

    @Inject
    public RestTermlistAction(Settings settings, RestController controller) {
        super(settings);
        controller.registerHandler(GET, "/_termlist", this);
        controller.registerHandler(GET, "/{index}/_termlist", this);
    }

    @Override
    public String getName() {
        return "termlist";
    }

    @Override
    protected RestChannelConsumer prepareRequest(RestRequest request, NodeClient client) throws IOException {
        TermlistRequest termlistRequest = new TermlistRequest(Strings.splitStringByCommaToArray(request.param("index")));
        termlistRequest.setField(request.param("field"));
        termlistRequest.setTerm(request.param("term"));
        termlistRequest.setFrom(request.paramAsInt("from", 0));
        termlistRequest.setSize(request.paramAsInt("size", -1));
        termlistRequest.setBackTracingCount(request.paramAsInt("backtracingcount", 0));
        termlistRequest.sortByDocFreq(request.paramAsBoolean("sortbydocfreqs", false));
        termlistRequest.sortByTotalFreq(request.paramAsBoolean("sortbytotalfreqs", false));
        termlistRequest.sortByTerm(request.paramAsBoolean("sortbyterms", false));
        termlistRequest.setMinDocFreq(request.paramAsInt("minDocFreq", 1));
        termlistRequest.setMinTotalFreq(request.paramAsInt("minTotalFreq", 1));
        final long t0 = System.nanoTime();


        return channel -> client.execute(TermlistAction.INSTANCE, termlistRequest,
                new RestStatusToXContentListener<>(channel));
    }

    /*public void handleRequest(final RestRequest request, final RestChannel channel, final Client client) {
        try {
            TermlistRequest termlistRequest = new TermlistRequest(Strings.splitStringByCommaToArray(request.param("index")));
            termlistRequest.setField(request.param("field"));
            termlistRequest.setTerm(request.param("term"));
            termlistRequest.setFrom(request.paramAsInt("from", 0));
            termlistRequest.setSize(request.paramAsInt("size", -1));
            termlistRequest.setBackTracingCount(request.paramAsInt("backtracingcount", 0));
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
    }*/
}