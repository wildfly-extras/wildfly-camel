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
import java.util.concurrent.TimeUnit;

import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.couchdb.CouchDbConstants;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.impl.DefaultCamelContext;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.lightcouch.CouchDbClient;
import org.wildfly.camel.test.common.utils.TestUtils;
import org.wildfly.camel.test.dockerjava.DockerManager;
import org.wildfly.extension.camel.CamelAware;

import com.google.gson.Gson;
import com.google.gson.JsonElement;

@CamelAware
@RunWith(Arquillian.class)
@ServerSetup({CouchDBIntegrationTest.ContainerSetupTask.class})
public class CouchDBIntegrationTest {

    private static final String CONTAINER_NAME = "couchdb";
    private static final String COUCHDB_NAME = "camelcouchdb";
    private static final String COUCHDB_USERNAME = "admin";
    private static final String COUCHDB_PASSWORD = "p4ssw0rd";
    private static final int COUCHDB_PORT = 5984;

    private CouchDbClient client;

    @Deployment
    public static JavaArchive createDeployment() {
        return ShrinkWrap.create(JavaArchive.class, "camel-couchdb-tests.jar")
            .addClass(TestUtils.class);
    }

    static class ContainerSetupTask implements ServerSetupTask {

    	private DockerManager dockerManager;

        @Override
        public void setup(ManagementClient managementClient, String someId) throws Exception {
        	
            String dockerHost = TestUtils.getDockerHost();
            
			/*
			docker run --detach \
				--name couchdb \
				-e COUCHDB_USER=admin \
				-e COUCHDB_PASSWORD=p4ssw0rd \
				-p 5984:5984 \
				couchdb:1.6.1
			*/
        	
        	dockerManager = new DockerManager()
        			.createContainer("couchdb:1.6.1", true)
        			.withName(CONTAINER_NAME)
        			.withEnv("COUCHDB_USER=admin", "COUCHDB_PASSWORD=p4ssw0rd")
        			.withPortBindings(COUCHDB_PORT + ":" + COUCHDB_PORT)
        			.startContainer();

			dockerManager
				.withAwaitHttp("http://" + dockerHost + ":" + COUCHDB_PORT)
				.withResponseCode(200)
				.withSleepPolling(500)
				.awaitCompletion(60, TimeUnit.SECONDS);
        }

        @Override
        public void tearDown(ManagementClient managementClient, String someId) throws Exception {
        	if (dockerManager != null) {
            	dockerManager.removeContainer();
        	}
        }
    }

    @Before
    public void setUp() throws Exception {
        client = new CouchDbClient(COUCHDB_NAME, true,
            "http", TestUtils.getDockerHost(), COUCHDB_PORT, COUCHDB_USERNAME, COUCHDB_PASSWORD);
    }

    @Test
    public void testCouchDBConsumer() throws Exception {
        CamelContext camelctx = new DefaultCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                fromF("couchdb:http://%s:%d/%s?username=%s&password=%s", TestUtils.getDockerHost(), COUCHDB_PORT, COUCHDB_NAME, COUCHDB_USERNAME, COUCHDB_PASSWORD)
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
            camelctx.close();
        }
    }

    @Test
    public void testCouchDBProducer() throws Exception {
        CamelContext camelctx = new DefaultCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:start")
                .toF("couchdb:http://%s:%d/%s?username=%s&password=%s", TestUtils.getDockerHost(), COUCHDB_PORT, COUCHDB_NAME, COUCHDB_USERNAME, COUCHDB_PASSWORD)
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
            camelctx.close();
        }
    }

    private String getJSONString() {
        return "{ \"randomString\" : \"" + UUID.randomUUID() + "\" }";
    }
}
