package org.xbib.elasticsearch.plugin.termlist;

import org.elasticsearch.action.admin.cluster.node.info.NodesInfoResponse;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequestBuilder;
import org.elasticsearch.action.index.IndexRequestBuilder;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.io.Streams;
import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.common.logging.ESLoggerFactory;
import org.elasticsearch.common.network.NetworkUtils;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.node.Node;
import org.junit.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.xbib.elasticsearch.action.termlist.TermlistRequestBuilder;
import org.xbib.elasticsearch.action.termlist.TermlistResponse;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.StringTokenizer;

import static org.elasticsearch.common.settings.Settings.settingsBuilder;
import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;
import static org.elasticsearch.node.NodeBuilder.nodeBuilder;

public class SimpleTests extends org.junit.Assert {

    private final static ESLogger logger = ESLoggerFactory.getLogger("test");

    protected final String CLUSTER = "test-cluster-"; //+ //NetworkUtils.getLocalAddress().getHostName();

    private Node node;

    private Client client;

    @BeforeClass
    public void startNode() {
        Settings settings = settingsBuilder()
                .put("cluster.name", CLUSTER)
                .put("discovery.zen.ping.multicast.enabled", false)
                .put("node.local", true)
                .put("index.number_of_shards", 1)
                .put("index.number_of_replica", 0)
                .put("index.store.type", "ram")
                .put("gateway.type", "none")
                .build();
        node = nodeBuilder().settings(settings).build().start();
        client = node.client();
    }



    @AfterClass
    public void stopNode() {
        node.close();
    }

    @Test
    public void assertPluginLoaded() {
        NodesInfoResponse nodesInfoResponse = client.admin().cluster().prepareNodesInfo().setPlugins(true).get();
        /*assertEquals(nodesInfoResponse.getNodes().length, 1);
        assertNotNull(nodesInfoResponse.getNodes()[0].getPlugins().getInfos());
        assertEquals(nodesInfoResponse.getNodes()[0].getPlugins().getInfos().size(), 1);
        assertEquals(nodesInfoResponse.getNodes()[0].getPlugins().getInfos().get(0).isSite(), false);
        assertTrue(nodesInfoResponse.getNodes()[0].getPlugins().getInfos().get(0).getName().startsWith("index-termlist"));*/
    }

    @Test
    public void testPlugin() throws IOException {
        XContentBuilder builder = jsonBuilder()
                .startObject()
                .startObject("properties")
                .startObject("content")
                .field("type", "string")
                .field("analzyer", "german")
                .endObject()
                .endObject()
                .endObject();
        CreateIndexRequestBuilder createIndexRequestBuilder = new CreateIndexRequestBuilder(client,null)
                .setIndex("test")
                .addMapping("docs", builder);
        createIndexRequestBuilder.execute().actionGet();
        for (int i = 0; i < 10; i++) {
            String content = join(makeList(), " ");
            //logger.info("{} -> {}", i, content);
            IndexRequestBuilder indexRequestBuilder = new IndexRequestBuilder(client,null)
                    .setIndex("test")
                    .setType("docs")
                    .setId(Integer.toString(i))
                    .setSource("content", content);
            indexRequestBuilder.setRefresh(true).execute().actionGet();
        }
        TermlistRequestBuilder termlistRequestBuilder = new TermlistRequestBuilder(client);
        TermlistResponse termlistResponse = termlistRequestBuilder.execute().actionGet();
        logger.info("termlist={}", termlistResponse.getTermlist());
    }

    private List<String> makeList() throws IOException {
        InputStream in = getClass().getResourceAsStream("/navid-kermani.txt");
        String s = Streams.copyToString(new InputStreamReader(in, "UTF-8"));
        in.close();
        StringTokenizer tokenizer = new StringTokenizer(s);
        List<String> list = new LinkedList<String>();
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