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
package org.wildfly.camel.test.elasticsearch5;

import static org.wildfly.camel.test.elasticsearch5.subA.ElasticsearchNodeProducer.ES_TRANSPORT_PORT;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.elasticsearch5.ElasticsearchConstants;
import org.apache.camel.impl.DefaultCamelContext;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.node.Node;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.gravia.resource.ManifestBuilder;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wildfly.camel.test.common.utils.AvailablePortFinder;
import org.wildfly.camel.test.common.utils.FileUtils;
import org.wildfly.camel.test.elasticsearch5.subA.ElasticsearchNodeProducer;
import org.wildfly.extension.camel.CamelAware;

@CamelAware
@RunWith(Arquillian.class)
public class Elasticsearch5IntegrationTest {

    private static Logger log = LoggerFactory.getLogger("elasticsearch5");

    @Inject
    @SuppressWarnings("unused")
    private Node node;

    @Rule
    public TestName testName = new TestName();

    @Deployment
    public static JavaArchive createDeployment() {
        JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "camel-elasticsearch5-tests.jar");
        archive.addClasses(ElasticsearchNodeProducer.class, AvailablePortFinder.class, FileUtils.class);
        archive.addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml");
        archive.setManifest(() -> {
            ManifestBuilder builder = new ManifestBuilder();
            builder.addManifestHeader("Dependencies", "org.elasticsearch5");
            return builder.openStream();
        });
        return archive;
    }

    @Test
    public void testGetSearchUpdateDelete() throws Exception {

        CamelContext camelctx = new DefaultCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:index").to("elasticsearch5://elasticsearch?operation=INDEX&indexName=twitter&indexType=tweet&ip=localhost&port=" + ES_TRANSPORT_PORT);
                from("direct:get").to("elasticsearch5://elasticsearch?operation=GET_BY_ID&indexName=twitter&indexType=tweet&ip=localhost&port=" + ES_TRANSPORT_PORT);
                from("direct:search").to("elasticsearch5://elasticsearch?operation=SEARCH&indexName=twitter&indexType=tweet&ip=localhost&port=" + ES_TRANSPORT_PORT);
                from("direct:update").to("elasticsearch5://elasticsearch?operation=UPDATE&indexName=twitter&indexType=tweet&ip=localhost&port=" + ES_TRANSPORT_PORT);
                from("direct:delete").to("elasticsearch5://elasticsearch?operation=DELETE&indexName=twitter&indexType=tweet&ip=localhost&port=" + ES_TRANSPORT_PORT);
            }
        });

        camelctx.start();
        try {
            ProducerTemplate template = camelctx.createProducerTemplate();

            //first, INDEX a value
            Map<String, String> map = createIndexedData();
            template.sendBody("direct:index", map);
            String indexId = template.requestBody("direct:index", map, String.class);
            Assert.assertNotNull("indexId should be set", indexId);

            //now, verify GET succeeded
            GetResponse getres = template.requestBody("direct:get", indexId, GetResponse.class);
            Assert.assertNotNull("response should not be null", getres);
            Assert.assertNotNull("response source should not be null", getres.getSource());

            //now, verify GET succeeded
            String query = "{\"query\":{\"match\":{\"content\":\"searchtest\"}}}";
            SearchResponse searchres = template.requestBody("direct:search", query, SearchResponse.class);
            Assert.assertNotNull("response should not be null", searchres);
            Assert.assertNotNull("response hits should be == 1", searchres.getHits().totalHits());

            Map<String, String> newMap = new HashMap<>();
            newMap.put(createPrefix() + "key2", createPrefix() + "value2");
            Map<String, Object> headers = new HashMap<>();
            headers.put(ElasticsearchConstants.PARAM_INDEX_ID, indexId);
            indexId = template.requestBodyAndHeaders("direct:update", newMap, headers, String.class);
            Assert.assertNotNull("indexId should be set", indexId);

            //now, perform DELETE
            DeleteResponse delres = template.requestBody("direct:delete", indexId, DeleteResponse.class);
            Assert.assertNotNull("response should not be null", delres);

            //now, verify GET fails to find the indexed value
            getres = template.requestBody("direct:get", indexId, GetResponse.class);
            Assert.assertNotNull("response should not be null", getres);
            Assert.assertNull("response source should be null", getres.getSource());
        } finally {
            camelctx.stop();
        }
    }

    private String createPrefix() {
        return testName.getMethodName().toLowerCase() + "-";
    }

    /**
     * As we don't delete the {@code target/data} folder for <b>each</b> test
     * below (otherwise they would run much slower), we need to make sure
     * there's no side effect of the same used data through creating unique
     * indexes.
     */
    private Map<String, String> createIndexedData(String... additionalPrefixes) {
        String prefix = createPrefix();

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
        log.info("Creating indexed data using the key/value pair {} => {}", key, value);

        Map<String, String> map = new HashMap<String, String>();
        map.put(key, value);
        return map;
    }

}
