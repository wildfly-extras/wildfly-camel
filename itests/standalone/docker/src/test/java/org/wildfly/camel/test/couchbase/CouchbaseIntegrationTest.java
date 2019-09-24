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
package org.wildfly.camel.test.couchbase;

import org.apache.camel.builder.RouteBuilder;
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
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.camel.test.common.http.HttpRequest;
import org.wildfly.camel.test.common.http.HttpRequest.HttpResponse;
import org.wildfly.camel.test.common.utils.TestUtils;
import org.wildfly.extension.camel.CamelAware;

@CamelAware
@RunWith(ArquillianConditionalRunner.class)
@RequiresDocker
@Ignore("[#2173] Intermittent failure of CouchbaseIntegrationTest")
public class CouchbaseIntegrationTest {

    private static final String CONTAINER_NAME = "couchbase";

    @ArquillianResource
    private CubeController cubeController;

    @Deployment
    public static JavaArchive createDeployment() {
        return ShrinkWrap.create(JavaArchive.class, "camel-couchbase-tests.jar")
            .addClasses(TestUtils.class, HttpRequest.class);
    }

    @Before
    public void setUp() throws Exception {
        cubeController.create(CONTAINER_NAME);
        cubeController.start(CONTAINER_NAME);
    }

    @After
    public void tearDown() throws Exception {
        cubeController.stop(CONTAINER_NAME);
        cubeController.destroy(CONTAINER_NAME);
    }

    @Test
    public void testComponent() throws Exception {
        initCouchbaseServer();

        DefaultCamelContext camelctx = new DefaultCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                fromF("couchbase:http://%s/beer-sample?password=&designDocumentName=beer&viewName=brewery_beers&limit=10", TestUtils.getDockerHost())
                .to("mock:result");
            }
        });

        MockEndpoint mockEndpoint = camelctx.getEndpoint("mock:result", MockEndpoint.class);
        mockEndpoint.expectedMessageCount(10);

        camelctx.start();
        try {
            mockEndpoint.assertIsSatisfied();
        } finally {
            camelctx.close();
        }
    }

    private void initCouchbaseServer() throws Exception {
        HttpResponse response = HttpRequest.post(getResource("pools/default"))
            .header("ns_server-ui", "yes")
            .content("memoryQuota=256")
            .getResponse();

        Assert.assertEquals(200, response.getStatusCode());

        response = HttpRequest.post(getResource("pools/default/buckets"))
            .header("ns_server-ui", "yes")
            .content("threadsNumber=3&replicaIndex=0&replicaNumber=1&evictionPolicy=valueOnly"
                + "&ramQuotaMB=100&bucketType=membase&name=default&authType=sasl"
                + "&saslPassword=&otherBucketsRamQuotaMB=100")
            .getResponse();

        Assert.assertEquals(202, response.getStatusCode());

        response = HttpRequest.post(getResource("settings/stats"))
            .header("ns_server-ui", "yes")
            .content("sendStats=false")
            .getResponse();

        Assert.assertEquals(200, response.getStatusCode());

        response = HttpRequest.post(getResource("settings/web"))
            .header("ns_server-ui", "yes")
            .content("password=p4ssw0rd&port=SAME&username=admin")
            .getResponse();

        Assert.assertEquals(200, response.getStatusCode());

        HttpResponse loginResponse = HttpRequest.post(getResource("uilogin"))
            .header("ns_server-ui", "yes")
            .content("password=p4ssw0rd&user=admin")
            .getResponse();

        String loginCookie = loginResponse.getHeader("Set-Cookie");
        Assert.assertEquals(200, loginResponse.getStatusCode());
        Assert.assertNotNull("Login cookie was null", loginCookie);

        // Guard against admin credentials not immediately taking effect
        Thread.sleep(100);

        response = HttpRequest.post(getResource("sampleBuckets/install"))
            .header("ns_server-ui", "yes")
            .header("Cookie", loginCookie)
            .content("[\"beer-sample\"]")
            .getResponse();

        Assert.assertEquals(202, response.getStatusCode());

        // Wait for sample data to be loaded
        int attempts = 0;
        do {
            response = HttpRequest.get(getResource("logs"))
                .header("Accept", "application/json, text/plain, */*")
                .header("ns_server-ui", "yes")
                .header("Cookie", loginCookie)
                .getResponse();

            Assert.assertEquals(200, response.getStatusCode());

            if (!response.getBody().contains("Completed loading sample bucket beer-sample")) {
                attempts++;
                Thread.sleep(500);
            } else {
                return;
            }
        } while(attempts < 30);

        throw new IllegalStateException("Gave up waiting for Couchbase server to become ready");
    }

    private String getResource(String resourcePath) throws Exception {
        return String.format("http://%s:8091/%s", TestUtils.getDockerHost(), resourcePath);
    }
}
