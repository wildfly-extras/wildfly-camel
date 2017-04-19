/*
 * #%L
 * Wildfly Camel :: Testsuite
 * %%
 * Copyright (C) 2013 - 2017 RedHat
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
package org.wildfly.camel.test.couchdb;

import java.util.Map;
import java.util.UUID;

import com.google.gson.Gson;
import com.google.gson.JsonElement;

import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.couchdb.CouchDbConstants;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.impl.DefaultCamelContext;
import org.arquillian.cube.CubeController;
import org.arquillian.cube.docker.impl.requirement.RequiresDocker;
import org.arquillian.cube.requirement.ArquillianConditionalRunner;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.lightcouch.CouchDbClient;
import org.wildfly.camel.test.common.utils.TestUtils;
import org.wildfly.extension.camel.CamelAware;

@CamelAware
@RunWith(ArquillianConditionalRunner.class)
@RequiresDocker
public class CouchDBIntegrationTest {

    private static final String CONTAINER_NAME = "couchdb";
    private static final String COUCHDB_NAME = "camelcouchdb";
    private static final String COUCHDB_USERNAME = "admin";
    private static final String COUCHDB_PASSWORD = "p4ssw0rd";

    @ArquillianResource
    private CubeController cubeController;

    private CouchDbClient client;

    @Deployment
    public static JavaArchive createDeployment() {
        return ShrinkWrap.create(JavaArchive.class, "camel-couchdb-tests.jar")
            .addClass(TestUtils.class);
    }

    @Before
    public void setUp() throws Exception {
        cubeController.create(CONTAINER_NAME);
        cubeController.start(CONTAINER_NAME);
        client = new CouchDbClient(COUCHDB_NAME, true,
            "http", TestUtils.getDockerHost(), 5984, COUCHDB_USERNAME, COUCHDB_PASSWORD);
    }

    @After
    public void tearDown() throws Exception {
        cubeController.stop(CONTAINER_NAME);
        cubeController.destroy(CONTAINER_NAME);
    }

    @Test
    public void testCouchDBConsumer() throws Exception {
        DefaultCamelContext camelctx = new DefaultCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                fromF("couchdb:http://%s:5984/%s?username=%s&password=%s", TestUtils.getDockerHost(), COUCHDB_NAME, COUCHDB_USERNAME, COUCHDB_PASSWORD)
                .to("mock:result");
            }
        });

        MockEndpoint mockEndpoint = camelctx.getEndpoint("mock:result", MockEndpoint.class);
        mockEndpoint.expectedHeaderReceived(CouchDbConstants.HEADER_METHOD, "UPDATE");
        mockEndpoint.expectedMessageCount(1);

        camelctx.start();
        try {
            JsonElement element = new Gson().fromJson(getJSONString(), JsonElement.class);
            client.save(element);

            mockEndpoint.assertIsSatisfied();
        } finally {
            camelctx.stop();
        }
    }

    @Test
    public void testCouchDBProducer() throws Exception {
        DefaultCamelContext camelctx = new DefaultCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:start")
                .toF("couchdb:http://%s:5984/%s?username=%s&password=%s", TestUtils.getDockerHost(), COUCHDB_NAME, COUCHDB_USERNAME, COUCHDB_PASSWORD)
                .to("mock:result");
            }
        });

        MockEndpoint mockEndpoint = camelctx.getEndpoint("mock:result", MockEndpoint.class);
        mockEndpoint.expectedMessageCount(1);

        camelctx.start();
        try {
            JsonElement element = new Gson().fromJson(getJSONString(), JsonElement.class);

            ProducerTemplate template = camelctx.createProducerTemplate();
            template.sendBody("direct:start", element);

            mockEndpoint.assertIsSatisfied();

            Map<String, Object> headers = mockEndpoint.getExchanges().get(0).getIn().getHeaders();
            Assert.assertTrue(headers.containsKey(CouchDbConstants.HEADER_DOC_ID));
            Assert.assertTrue(headers.containsKey(CouchDbConstants.HEADER_DOC_REV));
        } finally {
            camelctx.stop();
        }
    }


    private String getJSONString() {
        return "{ \"randomString\" : \"" + UUID.randomUUID() + "\" }";
    }
}
