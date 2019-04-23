package org.xbib.elasticsearch.plugin.termlist;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.analysis.common.CommonAnalysisPlugin;
import org.elasticsearch.common.Strings;
import org.elasticsearch.common.io.Streams;
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.common.xcontent.json.JsonXContent;
import org.elasticsearch.plugins.Plugin;
import org.elasticsearch.test.ESSingleNodeTestCase;
import org.xbib.elasticsearch.plugin.termlist.action.TermlistRequestBuilder;
import org.xbib.elasticsearch.plugin.termlist.action.TermlistResponse;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.StringTokenizer;

public class TermlistTests extends ESSingleNodeTestCase {

    private static final Logger logger = LogManager.getLogger("test");

    @Override
    protected Collection<Class<? extends Plugin>> getPlugins() {
        return Arrays.asList(TermlistPlugin.class, CommonAnalysisPlugin.class);
    }

    public void testPlugin() throws Exception {
        XContentBuilder builder = XContentFactory.jsonBuilder()
                .startObject()
                .startObject("properties")
                .startObject("content")
                .field("type", "text")
                .field("analyzer", "german")
                .endObject()
                .endObject()
                .endObject();
        client().admin().indices().prepareCreate("test")
                .addMapping("docs", builder).execute().actionGet();
        for (int i = 0; i < 1; i++) {
            String content = join(fullContent(), " ");
            logger.info("{} -> {}", i, content);
            client().prepareIndex()
                    .setIndex("test")
                    .setType("docs")
                    .setId(Integer.toString(i))
                    .setSource("content", content).execute().actionGet();
        }
        client().admin().indices().prepareRefresh("test").execute().actionGet();

        TermlistRequestBuilder termlistRequestBuilder = new TermlistRequestBuilder(client());
        TermlistResponse termlistResponse = termlistRequestBuilder.execute().actionGet();
        XContentBuilder contentBuilder = XContentBuilder.builder(JsonXContent.jsonXContent);
        logger.info("termlist response = {}",
                Strings.toString(termlistResponse.toXContent(contentBuilder, ToXContent.EMPTY_PARAMS)));
    }

    private List<String> randomContent() throws IOException {
        InputStream in = getClass().getResourceAsStream("/navid-kermani.txt");
        String s = Streams.copyToString(new InputStreamReader(in, StandardCharsets.UTF_8));
        in.close();
        StringTokenizer tokenizer = new StringTokenizer(s);
        List<String> list = new ArrayList<>();
        while (tokenizer.hasMoreTokens()) {
            String token = tokenizer.nextToken();
            if (!token.isEmpty()) {
                list.add(token);
            }
        }
        Random random = new Random();
        Collections.shuffle(list);
        return list.subList(0, Math.min(10, random.nextInt(list.size())));
    }

    private List<String> fullContent() throws IOException {
        InputStream in = getClass().getResourceAsStream("/navid-kermani.txt");
        String s = Streams.copyToString(new InputStreamReader(in, StandardCharsets.UTF_8));
        in.close();
        StringTokenizer tokenizer = new StringTokenizer(s);
        List<String> list = new ArrayList<>();
        while (tokenizer.hasMoreTokens()) {
            String token = tokenizer.nextToken();
            if (!token.isEmpty()) {
                list.add(token);
            }
        }
        return list;
    }

    private String join(List<String> s, String delimiter) {
        if (s == null || s.isEmpty()) return "";
        Iterator<String> it = s.iterator();
        StringBuilder builder = new StringBuilder(it.next());
        while (it.hasNext()) {
            builder.append(delimiter).append(it.next());
        }
        return builder.toString();
    }
}