/*
 * Licensed to ElasticSearch and Shay Banon under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. ElasticSearch licenses this
 * file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.elasticsearch.rest.action.termlist;

import java.io.IOException;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.termlist.TermlistAction;
import org.elasticsearch.action.termlist.TermlistRequest;
import org.elasticsearch.action.termlist.TermlistResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.Strings;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.rest.*;
import static org.elasticsearch.rest.RestRequest.Method.GET;
import static org.elasticsearch.rest.RestRequest.Method.POST;
import static org.elasticsearch.rest.RestStatus.OK;
import org.elasticsearch.rest.action.support.RestActions;
import static org.elasticsearch.rest.action.support.RestActions.buildBroadcastShardsHeader;
import org.elasticsearch.rest.action.support.RestXContentBuilder;

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

    @Override
    public void handleRequest(final RestRequest request, final RestChannel channel) {
        TermlistRequest termlistRequest = new TermlistRequest(
            Strings.splitStringByCommaToArray(request.param("index")));
        termlistRequest.setField(request.param("field"));
        client.execute(TermlistAction.INSTANCE, termlistRequest, new ActionListener<TermlistResponse>() {

            @Override
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

            @Override
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