/*
 * #%L
 * Wildfly Camel :: Testsuite
 * %%
 * Copyright (C) 2013 - 2015 RedHat
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package org.wildfly.camel.test.elasticsearch;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.Map;

import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.elasticsearch.ElasticsearchComponent;
import org.apache.camel.component.elasticsearch.ElasticsearchConstants;
import org.apache.camel.impl.DefaultCamelContext;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.settings.Settings.Builder;
import org.elasticsearch.node.Node;
import org.elasticsearch.node.NodeBuilder;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.extension.camel.CamelAware;

@CamelAware
@RunWith(Arquillian.class)
public class ElasticSearchIntegrationTest {

    private static final Path DATA_PATH = Paths.get("target", "elasticsearch", "data");
    private static final Path HOME_PATH = Paths.get("target", "elasticsearch", "home");

    @Deployment
    public static JavaArchive createDeployment() {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "camel-elasticsearch-tests.jar");
        return archive;
    }

    static Node node;

    @Test
    public void testIndexContentUsingHeaders() throws Exception {

        CamelContext camelctx = new DefaultCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:start").to("elasticsearch://local");
            }
        });

        initEleasticSearchClient(camelctx);

        camelctx.start();
        try {
            Map<String, String> indexedData = new HashMap<>();
            indexedData.put("content", "test");

            Map<String, Object> headers = new HashMap<>();
            headers.put(ElasticsearchConstants.PARAM_OPERATION, ElasticsearchConstants.OPERATION_INDEX);
            headers.put(ElasticsearchConstants.PARAM_INDEX_NAME, "twitter");
            headers.put(ElasticsearchConstants.PARAM_INDEX_TYPE, "tweet");

            ProducerTemplate template = camelctx.createProducerTemplate();

            String indexId = template.requestBodyAndHeaders("direct:start", indexedData, headers, String.class);
            Assert.assertNotNull("Index id should not be null", indexId);
        } finally {
            camelctx.stop();
        }
    }

    @Test
    public void testGetContent() throws Exception {
        CamelContext camelctx = new DefaultCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:index").to("elasticsearch://local?operation=INDEX&indexName=twitter&indexType=tweet");
                from("direct:get").to("elasticsearch://local?operation=GET_BY_ID&indexName=twitter&indexType=tweet");
            }
        });

        initEleasticSearchClient(camelctx);

        camelctx.start();
        try {
            Map<String, String> indexedData = new HashMap<>();
            indexedData.put("content", "test");

            // Index some initial data
            ProducerTemplate template = camelctx.createProducerTemplate();
            template.sendBody("direct:index", indexedData);

            String indexId = template.requestBody("direct:index", indexedData, String.class);
            Assert.assertNotNull("Index id should not be null", indexId);

            // Retrieve indexed data
            GetResponse response = template.requestBody("direct:get", indexId, GetResponse.class);
            Assert.assertNotNull("getResponse should not be null", response);
        } finally {
            camelctx.stop();
        }
    }

    @Test
    public void testDeleteContent() throws Exception {
        CamelContext camelctx = new DefaultCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:index").to("elasticsearch://local?operation=INDEX&indexName=twitter&indexType=tweet");
                from("direct:get").to("elasticsearch://local?operation=GET_BY_ID&indexName=twitter&indexType=tweet");
                from("direct:delete").to("elasticsearch://local?operation=DELETE&indexName=twitter&indexType=tweet");
            }
        });

        initEleasticSearchClient(camelctx);

        camelctx.start();
        try {
            Map<String, String> indexedData = new HashMap<>();
            indexedData.put("content", "test");

            // Index some initial data
            ProducerTemplate template = camelctx.createProducerTemplate();
            template.sendBody("direct:index", indexedData);

            String indexId = template.requestBody("direct:index", indexedData, String.class);
            Assert.assertNotNull("Index id should not be null", indexId);

            // Retrieve indexed data
            GetResponse getResponse = template.requestBody("direct:get", indexId, GetResponse.class);
            Assert.assertNotNull("getResponse should not be null", getResponse);

            // Delete indexed data
            DeleteResponse deleteResponse = template.requestBody("direct:delete", indexId, DeleteResponse.class);
            Assert.assertNotNull("deleteResponse should not be null", deleteResponse);

            // Verify that the data has been deleted
            getResponse = template.requestBody("direct:get", indexId, GetResponse.class);
            Assert.assertNotNull("getResponse should not be null", getResponse);
            Assert.assertNull("getResponse source should be null", getResponse.getSource());
        } finally {
            camelctx.stop();
        }
    }

    @Test
    public void testSearchContent() throws Exception {
        CamelContext camelctx = new DefaultCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:index").to("elasticsearch://local?operation=INDEX&indexName=twitter&indexType=tweet");
                from("direct:search").to("elasticsearch://local?operation=SEARCH&indexName=twitter&indexType=tweet");
            }
        });

        initEleasticSearchClient(camelctx);

        camelctx.start();
        try {
            Map<String, String> indexedData = new HashMap<>();
            indexedData.put("content", "test");

            // Index some initial data
            ProducerTemplate template = camelctx.createProducerTemplate();
            template.sendBody("direct:index", indexedData);

            // Search for content
            Map<String, Object> actualQuery = new HashMap<>();
            actualQuery.put("content", "searchtest");
            Map<String, Object> match = new HashMap<>();
            match.put("match", actualQuery);
            Map<String, Object> query = new HashMap<>();
            query.put("query", match);

            SearchResponse searchResponse = template.requestBody("direct:search", query, SearchResponse.class);

            Assert.assertNotNull("searchResponse should not be null", searchResponse);
            Assert.assertNotNull("searchResponse hit count should equal 1", searchResponse.getHits().totalHits());
        } finally {
            camelctx.stop();
        }
    }

    private void initEleasticSearchClient(CamelContext camelctx) throws IOException {
        if (node == null) {
            deleteDataDirectory();
            Builder settings = Settings.settingsBuilder().put("http.enabled", false).put("path.data", DATA_PATH).put("path.home", HOME_PATH);
            node = NodeBuilder.nodeBuilder().local(true).settings(settings).node();
        }
        camelctx.getComponent("elasticsearch", ElasticsearchComponent.class).setClient(node.client());
    }

    private static void deleteDataDirectory() throws IOException {
        // ES component currently has no method of configuring the data dir for a local server, hence manual cleanup
        if (DATA_PATH.toFile().isDirectory()) {
            Files.walkFileTree(DATA_PATH, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Files.delete(file);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exception) throws IOException {
                    exception.printStackTrace();
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exception) throws IOException {
                    if (exception == null) {
                        Files.delete(dir);
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        }
    }
}
