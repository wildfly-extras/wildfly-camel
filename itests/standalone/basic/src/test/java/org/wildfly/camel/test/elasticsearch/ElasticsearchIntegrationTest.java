/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.wildfly.camel.test.elasticsearch;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.elasticsearch.ElasticsearchComponent;
import org.apache.camel.component.elasticsearch.ElasticsearchConstants;
import org.apache.camel.component.elasticsearch.ElasticsearchOperation;
import org.apache.camel.impl.DefaultCamelContext;
import org.elasticsearch.action.DocWriteResponse;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.search.SearchHits;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.gravia.resource.ManifestBuilder;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.camel.test.common.utils.AvailablePortFinder;
import org.wildfly.camel.test.common.utils.FileUtils;
import org.wildfly.camel.test.elasticsearch.subA.ElasticsearchServerServlet;
import org.wildfly.extension.camel.CamelAware;

@CamelAware
@RunWith(Arquillian.class)
public class ElasticsearchIntegrationTest {

    private static final String PREFIX = "camel-elasticsearch-rest-";

    // TODO: CAMEL-12684: Replace with client side ServerSetupTask when Lucene versions are aligned
    @Deployment(order = 1, testable = false, name = "elasticsearch-server.war")
    public static WebArchive createElasticSearchServerDeployment() {
        File[] files = Maven.configureResolverViaPlugin()
            .resolve("org.elasticsearch.plugin:transport-netty4-client")
            .withoutTransitivity()
            .asFile();

        return ShrinkWrap.create(WebArchive.class, "elasticsearch-server.war")
            .addClasses(FileUtils.class, AvailablePortFinder.class, ElasticsearchServerServlet.class)
            .addAsLibraries(files)
            .setManifest(() -> {
                ManifestBuilder builder = new ManifestBuilder();
                builder.addManifestHeader("Dependencies",
                    "org.elasticsearch,io.netty,com.carrotsearch.hppc,org.apache.logging.log4j,org.apache.lucene:7.1");
                return builder.openStream();
            });
    }

    @Deployment(order = 2)
    public static JavaArchive createDeployment() {
        return ShrinkWrap.create(JavaArchive.class, "camel-elasticsearch-rest-tests.jar")
            .addClass(AvailablePortFinder.class);
    }

    @Test
    public void testGet() throws Exception {
        CamelContext camelctx = createCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:index")
                    .to("elasticsearch-rest://elasticsearch?operation=Index&indexName=twitter&indexType=tweet&hostAddresses=" + getElasticsearchHost());

                from("direct:get")
                    .to("elasticsearch-rest://elasticsearch?operation=GetById&indexName=twitter&indexType=tweet&hostAddresses=" + getElasticsearchHost());
            }
        });

        camelctx.start();
        try {
            ProducerTemplate template = camelctx.createProducerTemplate();

            //first, Index a value
            Map<String, String> map = createIndexedData("testGet");
            template.sendBody("direct:index", map);
            String indexId = template.requestBody("direct:index", map, String.class);
            Assert.assertNotNull("indexId should be set", indexId);

            //now, verify GET succeeded
            GetResponse response = template.requestBody("direct:get", indexId, GetResponse.class);
            Assert.assertNotNull("response should not be null", response);
            Assert.assertNotNull("response source should not be null", response.getSource());
        } finally {
            camelctx.stop();
        }
    }

    @Test
    public void testDelete() throws Exception {
        CamelContext camelctx = createCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:index")
                    .to("elasticsearch-rest://elasticsearch?operation=Index&indexName=twitter&indexType=tweet&hostAddresses=" + getElasticsearchHost());

                from("direct:get")
                    .to("elasticsearch-rest://elasticsearch?operation=GetById&indexName=twitter&indexType=tweet&hostAddresses=" + getElasticsearchHost());

                from("direct:delete")
                    .to("elasticsearch-rest://elasticsearch?operation=Delete&indexName=twitter&indexType=tweet&hostAddresses=" + getElasticsearchHost());
            }
        });

        camelctx.start();
        try {
            ProducerTemplate template = camelctx.createProducerTemplate();

            //first, Index a value
            Map<String, String> map = createIndexedData("testDelete");
            template.sendBody("direct:index", map);
            String indexId = template.requestBody("direct:index", map, String.class);
            Assert.assertNotNull("indexId should be set", indexId);

            //now, verify GET succeeded
            GetResponse response = template.requestBody("direct:get", indexId, GetResponse.class);
            Assert.assertNotNull("response should not be null", response);
            Assert.assertNotNull("response source should not be null", response.getSource());

            //now, perform Delete
            DeleteResponse.Result deleteResponse = template.requestBody("direct:delete", indexId, DeleteResponse.Result.class);
            Assert.assertNotNull("response should not be null", deleteResponse);

            //now, verify GET fails to find the indexed value
            response = template.requestBody("direct:get", indexId, GetResponse.class);
            Assert.assertNotNull("response should not be null", response);
            Assert.assertNull("response source should be null", response.getSource());
        } finally {
            camelctx.stop();
        }

    }

    @Test
    public void testSearchWithMapQuery() throws Exception {
        CamelContext camelctx = createCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:index")
                    .to("elasticsearch-rest://elasticsearch?operation=Index&indexName=twitter&indexType=tweet&hostAddresses=" + getElasticsearchHost());

                from("direct:get")
                    .to("elasticsearch-rest://elasticsearch?operation=GetById&indexName=twitter&indexType=tweet&hostAddresses=" + getElasticsearchHost());

                from("direct:search")
                    .to("elasticsearch-rest://elasticsearch?operation=Search&indexName=twitter&indexType=tweet&hostAddresses=" + getElasticsearchHost());
            }
        });

        camelctx.start();
        try {
            ProducerTemplate template = camelctx.createProducerTemplate();

            //first, Index a value
            Map<String, String> map = createIndexedData("testSearchWithMapQuery");
            String indexId = template.requestBody("direct:index", map, String.class);
            Assert.assertNotNull("indexId should be set", indexId);

            //now, verify GET succeeded
            GetResponse getResponse = template.requestBody("direct:get", indexId, GetResponse.class);
            Assert.assertNotNull("response should not be null", getResponse);
            Assert.assertNotNull("response source should not be null", getResponse.getSource());

            //now, verify GET succeeded
            Map<String, Object> actualQuery = new HashMap<>();
            actualQuery.put("testsearchwithmapquery-key", "testsearchwithmapquery-value");
            Map<String, Object> match = new HashMap<>();
            match.put("match", actualQuery);
            Map<String, Object> query = new HashMap<>();
            query.put("query", match);

            SearchHits response = template.requestBody("direct:search", match, SearchHits.class);
            Assert.assertNotNull("response should not be null", response);
            Assert.assertEquals("response hits should be == 0", 0, response.totalHits);
        } finally {
            camelctx.stop();
        }
    }

    @Test
    public void testUpdate() throws Exception {
        CamelContext camelctx = createCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:index")
                    .to("elasticsearch-rest://elasticsearch?operation=Index&indexName=twitter&indexType=tweet&hostAddresses=" + getElasticsearchHost());

                from("direct:update")
                    .to("elasticsearch-rest://elasticsearch?operation=Update&indexName=twitter&indexType=tweet&hostAddresses=" + getElasticsearchHost());
            }
        });

        camelctx.start();
        try {
            ProducerTemplate template = camelctx.createProducerTemplate();

            Map<String, String> map = createIndexedData("testUpdate");
            String indexId = template.requestBody("direct:index", map, String.class);
            Assert.assertNotNull("indexId should be set", indexId);

            Map<String, String> newMap = new HashMap<>();
            newMap.put(PREFIX + "key2", PREFIX + "value2");
            Map<String, Object> headers = new HashMap<>();
            headers.put(ElasticsearchConstants.PARAM_INDEX_ID, indexId);
            indexId = template.requestBodyAndHeaders("direct:update", newMap, headers, String.class);
            Assert.assertNotNull("indexId should be set", indexId);
        } finally {
            camelctx.stop();
        }
    }

    @Test
    public void testGetWithHeaders() throws Exception {
        CamelContext camelctx = createCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:start")
                    .to("elasticsearch-rest://elasticsearch?operation=Index&hostAddresses=" + getElasticsearchHost());
            }
        });

        camelctx.start();
        try {
            ProducerTemplate template = camelctx.createProducerTemplate();

            //first, Index a value
            Map<String, String> map = createIndexedData("testGetWithHeaders");
            Map<String, Object> headers = new HashMap<>();
            headers.put(ElasticsearchConstants.PARAM_OPERATION, ElasticsearchOperation.Index);
            headers.put(ElasticsearchConstants.PARAM_INDEX_NAME, "twitter");
            headers.put(ElasticsearchConstants.PARAM_INDEX_TYPE, "tweet");

            String indexId = template.requestBodyAndHeaders("direct:start", map, headers, String.class);

            //now, verify GET
            headers.put(ElasticsearchConstants.PARAM_OPERATION, ElasticsearchOperation.GetById);
            GetResponse response = template.requestBodyAndHeaders("direct:start", indexId, headers, GetResponse.class);
            Assert.assertNotNull("response should not be null", response);
            Assert.assertNotNull("response source should not be null", response.getSource());
        } finally {
            camelctx.stop();
        }
    }

    @Test
    public void testExistsWithHeaders() throws Exception {
        CamelContext camelctx = createCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:start")
                    .to("elasticsearch-rest://elasticsearch?operation=Index&hostAddresses=" + getElasticsearchHost());

                from("direct:exists")
                    .to("elasticsearch-rest://elasticsearch?operation=Exists&hostAddresses=" + getElasticsearchHost());
            }
        });

        camelctx.start();
        try {
            ProducerTemplate template = camelctx.createProducerTemplate();

            //first, Index a value
            Map<String, String> map = createIndexedData("testExistsWithHeaders");
            Map<String, Object> headers = new HashMap<>();
            headers.put(ElasticsearchConstants.PARAM_OPERATION, ElasticsearchOperation.Index);
            headers.put(ElasticsearchConstants.PARAM_INDEX_NAME, "twitter");
            headers.put(ElasticsearchConstants.PARAM_INDEX_TYPE, "tweet");

            String indexId = template.requestBodyAndHeaders("direct:start", map, headers, String.class);

            //now, verify GET
            headers.put(ElasticsearchConstants.PARAM_OPERATION, ElasticsearchOperation.Exists);
            headers.put(ElasticsearchConstants.PARAM_INDEX_NAME, "twitter");
            Boolean exists = template.requestBodyAndHeaders("direct:exists", "", headers, Boolean.class);
            Assert.assertNotNull("response should not be null", exists);
            Assert.assertTrue("Index should exist", exists);
        } finally {
            camelctx.stop();
        }
    }

    @Test
    public void testNotExistsWithHeaders() throws Exception {
        CamelContext camelctx = createCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:start")
                    .to("elasticsearch-rest://elasticsearch?operation=Index&hostAddresses=" + getElasticsearchHost());

                from("direct:exists")
                    .to("elasticsearch-rest://elasticsearch?operation=Exists&hostAddresses=" + getElasticsearchHost());
            }
        });

        camelctx.start();
        try {
            ProducerTemplate template = camelctx.createProducerTemplate();

            //first, Index a value
            Map<String, String> map = createIndexedData("testNotExistsWithHeaders");
            Map<String, Object> headers = new HashMap<>();
            headers.put(ElasticsearchConstants.PARAM_OPERATION, ElasticsearchOperation.Index);
            headers.put(ElasticsearchConstants.PARAM_INDEX_NAME, "twitter");
            headers.put(ElasticsearchConstants.PARAM_INDEX_TYPE, "tweet");

            String indexId = template.requestBodyAndHeaders("direct:start", map, headers, String.class);

            //now, verify GET
            headers.put(ElasticsearchConstants.PARAM_OPERATION, ElasticsearchOperation.Exists);
            headers.put(ElasticsearchConstants.PARAM_INDEX_NAME, "twitter-tweet");
            Boolean exists = template.requestBodyAndHeaders("direct:exists", "", headers, Boolean.class);
            Assert.assertNotNull("response should not be null", exists);
            Assert.assertFalse("Index should not exist", exists);
        } finally {
            camelctx.stop();
        }
    }


    @Test
    public void testDeleteWithHeaders() throws Exception {
        CamelContext camelctx = createCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:start")
                    .to("elasticsearch-rest://elasticsearch?operation=Index&hostAddresses=" + getElasticsearchHost());

                from("direct:delete")
                    .to("elasticsearch-rest://elasticsearch?operation=Delete&indexName=twitter&indexType=tweet&hostAddresses=" + getElasticsearchHost());
            }
        });

        camelctx.start();
        try {
            ProducerTemplate template = camelctx.createProducerTemplate();

            //first, Index a value
            Map<String, String> map = createIndexedData("testDeleteWithHeaders");
            Map<String, Object> headers = new HashMap<>();
            headers.put(ElasticsearchConstants.PARAM_OPERATION, ElasticsearchOperation.Index);
            headers.put(ElasticsearchConstants.PARAM_INDEX_NAME, "twitter");
            headers.put(ElasticsearchConstants.PARAM_INDEX_TYPE, "tweet");

            String indexId = template.requestBodyAndHeaders("direct:start", map, headers, String.class);

            //now, verify GET
            headers.put(ElasticsearchConstants.PARAM_OPERATION, ElasticsearchOperation.GetById);
            GetResponse response = template.requestBodyAndHeaders("direct:start", indexId, headers, GetResponse.class);
            Assert.assertNotNull("response should not be null", response);
            Assert.assertNotNull("response source should not be null", response.getSource());

            //now, perform Delete
            headers.put(ElasticsearchConstants.PARAM_OPERATION, ElasticsearchOperation.Delete);
            DocWriteResponse.Result deleteResponse = template.requestBodyAndHeaders("direct:start", indexId, headers, DocWriteResponse.Result.class);
            Assert.assertEquals("response should not be null", DocWriteResponse.Result.DELETED, deleteResponse);

            //now, verify GET fails to find the indexed value
            headers.put(ElasticsearchConstants.PARAM_OPERATION, ElasticsearchOperation.GetById);
            response = template.requestBodyAndHeaders("direct:start", indexId, headers, GetResponse.class);
            Assert.assertNotNull("response should not be null", response);
            Assert.assertNull("response source should be null", response.getSource());
        } finally {
            camelctx.stop();
        }
    }

    @Test
    public void testUpdateWithIDInHeader() throws Exception {
        CamelContext camelctx = createCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:start")
                    .to("elasticsearch-rest://elasticsearch?operation=Index&hostAddresses=" + getElasticsearchHost());
            }
        });

        camelctx.start();
        try {
            ProducerTemplate template = camelctx.createProducerTemplate();

            Map<String, String> map = createIndexedData("testUpdateWithIDInHeader");
            Map<String, Object> headers = new HashMap<>();
            headers.put(ElasticsearchConstants.PARAM_OPERATION, ElasticsearchOperation.Index);
            headers.put(ElasticsearchConstants.PARAM_INDEX_NAME, "twitter");
            headers.put(ElasticsearchConstants.PARAM_INDEX_TYPE, "tweet");
            headers.put(ElasticsearchConstants.PARAM_INDEX_ID, "123");

            String indexId = template.requestBodyAndHeaders("direct:start", map, headers, String.class);
            Assert.assertNotNull("indexId should be set", indexId);
            Assert.assertEquals("indexId should be equals to the provided id", "123", indexId);

            headers.put(ElasticsearchConstants.PARAM_OPERATION, ElasticsearchOperation.Update);

            indexId = template.requestBodyAndHeaders("direct:start", map, headers, String.class);
            Assert.assertNotNull("indexId should be set", indexId);
            Assert.assertEquals("indexId should be equals to the provided id", "123", indexId);
        } finally {
            camelctx.stop();
        }
    }

    @Test
    public void getRequestBody() throws Exception {
        CamelContext camelctx = createCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:index")
                    .to("elasticsearch-rest://elasticsearch?operation=Index&indexName=twitter&indexType=tweet&hostAddresses=" + getElasticsearchHost());

                from("direct:get")
                    .to("elasticsearch-rest://elasticsearch?operation=GetById&indexName=twitter&indexType=tweet&hostAddresses=" + getElasticsearchHost());
            }
        });

        camelctx.start();
        try {
            ProducerTemplate template = camelctx.createProducerTemplate();

            GetRequest request = new GetRequest(PREFIX + "foo").type(PREFIX + "bar");

            String documentId = template.requestBody("direct:index",
                new IndexRequest(PREFIX + "foo", PREFIX + "bar", PREFIX + "testId")
                    .source(PREFIX + "content", PREFIX + "hello"), String.class);

            GetResponse response = template.requestBody("direct:get",
                request.id(documentId), GetResponse.class);

            Assert.assertNotNull(response);
            Assert.assertEquals(PREFIX + "hello", response.getSourceAsMap().get(PREFIX + "content"));
        } finally {
            camelctx.stop();
        }
    }

    @Test
    public void deleteRequestBody() throws Exception {
        CamelContext camelctx = createCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:index")
                    .to("elasticsearch-rest://elasticsearch?operation=Index&indexName=twitter&indexType=tweet&hostAddresses=" + getElasticsearchHost());

                from("direct:delete")
                    .to("elasticsearch-rest://elasticsearch?operation=Delete&indexName=twitter&indexType=tweet&hostAddresses=" + getElasticsearchHost());
            }
        });

        camelctx.start();
        try {
            ProducerTemplate template = camelctx.createProducerTemplate();

            DeleteRequest request = new DeleteRequest(PREFIX + "foo").type(PREFIX + "bar");

            String documentId = template.requestBody("direct:index",
                new IndexRequest("" + PREFIX + "foo", "" + PREFIX + "bar", "" + PREFIX + "testId")
                    .source(PREFIX + "content", PREFIX + "hello"), String.class);
            DeleteResponse.Result response = template.requestBody("direct:delete", request.id(documentId), DeleteResponse.Result.class);

            Assert.assertNotNull(response);
        } finally {
            camelctx.stop();
        }
    }


    private CamelContext createCamelContext() throws InterruptedException {
        CamelContext camelctx = new DefaultCamelContext();
        ElasticsearchComponent elasticsearchComponent = new ElasticsearchComponent();
        elasticsearchComponent.setHostAddresses(getElasticsearchHost());
        camelctx.addComponent("elasticsearch-rest", elasticsearchComponent);
        return camelctx;
    }

    private Map<String, String> createIndexedData(String... additionalPrefixes) {
        String prefix = PREFIX;

        // take over any potential prefixes we may have been asked for
        if (additionalPrefixes.length > 0) {
            StringBuilder sb = new StringBuilder(prefix);
            for (String additionalPrefix : additionalPrefixes) {
                sb.append(additionalPrefix).append("-");
            }
            prefix = sb.toString();
        }

        String key = prefix + "key";
        String value = prefix + "value";

        Map<String, String> map = new HashMap<>();
        map.put(key, value);
        return map;
    }

    private String getElasticsearchHost() {
        return String.format("localhost:%s", AvailablePortFinder.readServerData("es-port.txt"));
    }

}
