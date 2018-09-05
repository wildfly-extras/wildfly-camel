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
package org.wildfly.camel.test.lra;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.equalTo;

import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Response;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.Header;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.model.SagaPropagation;
import org.apache.camel.service.lra.LRASagaService;
import org.arquillian.cube.CubeController;
import org.arquillian.cube.docker.impl.requirement.RequiresDocker;
import org.arquillian.cube.requirement.ArquillianConditionalRunner;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.gravia.resource.ManifestBuilder;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.After;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.camel.test.common.utils.TestUtils;
import org.wildfly.extension.camel.CamelAware;

@org.junit.Ignore("https://github.com/wildfly-extras/wildfly-camel/issues/2601")
@CamelAware
@RunWith(ArquillianConditionalRunner.class)
@RequiresDocker
public class LRAIntegrationTest {

    private static final String CONTAINER_NAME = "lra";

    @ArquillianResource
    private CubeController cubeController;
    private OrderManagerService orderManagerService;
    private CreditService creditService;

    @Deployment
    public static JavaArchive createDeployment() {
        return ShrinkWrap.create(JavaArchive.class, "camel-lra-tests.jar")
            .addClass(TestUtils.class)
            .addPackages(true, "org.awaitility")
            .setManifest(() -> {
                ManifestBuilder builder = new ManifestBuilder();
                builder.addManifestHeader("Dependencies", "org.jboss.resteasy.resteasy-jaxrs,com.fasterxml.jackson.core.jackson-databind");
                return builder.openStream();
            });
    }

    @Before
    public void setUp() throws Exception {
        Assume.assumeTrue("LRAIntegrationTest can only run against local docker daemon", TestUtils.getDockerHost().equals("localhost"));
        cubeController.create(CONTAINER_NAME);
        cubeController.start(CONTAINER_NAME);
    }

    @After
    public void tearDown() throws Exception {
        cubeController.stop(CONTAINER_NAME);
        cubeController.destroy(CONTAINER_NAME);
    }

    @Test
    public void testCreditExhausted() throws Exception {
        CamelContext camelctx = createCamelContext();
        camelctx.start();
        try {
            int activeLRAs = getNumberOfActiveLRAs();

            // total credit is 100
            buy(camelctx, 20, false, false);
            buy(camelctx, 70, false, false);
            buy(camelctx, 20, false, true); // fail
            buy(camelctx, 5, false, false);

            await().until(() -> orderManagerService.getOrders().size(), equalTo(3));
            await().until(() -> creditService.getCredit(), equalTo(5));

            Assert.assertEquals("Some LRA have been left pending", activeLRAs, getNumberOfActiveLRAs());
        } finally {
            camelctx.stop();
        }
    }

    @Test
    public void testTotalCompensation() throws Exception {
        CamelContext camelctx = createCamelContext();
        camelctx.start();
        try {
            int activeLRAs = getNumberOfActiveLRAs();

            // total credit is 100
            for (int i = 0; i < 10; i++) {
                if (i % 2 == 0) {
                    buy(camelctx, 10, false, false);
                } else {
                    buy(camelctx, 10, true, true);
                }
            }

            await().until(() -> orderManagerService.getOrders().size(), equalTo(5));
            await().until(() -> creditService.getCredit(), equalTo(50));

            Assert.assertEquals("Some LRA have been left pending", activeLRAs, getNumberOfActiveLRAs());
        } finally {
            camelctx.stop();
        }
    }

    private void buy(CamelContext camelctx, int amount, boolean failAtTheEnd, boolean shouldFail) {
        try {
            camelctx.createFluentProducerTemplate()
                .to("direct:saga")
                .withHeader("amount", amount)
                .withHeader("fail", failAtTheEnd)
                .request();

            if (shouldFail) {
                Assert.fail("Exception not thrown");
            }
        } catch (Exception ex) {
            if (!shouldFail) {
                ex.printStackTrace();
                Assert.fail("Unexpected exception");
            }
        }
    }

    private CamelContext createCamelContext() throws Exception {
        CamelContext camelctx = new DefaultCamelContext();
        camelctx.addService(createLRASagaService());

        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                restConfiguration()
                    .component("undertow")
                    .contextPath("/lra");
            }
        });

        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                orderManagerService = new OrderManagerService();
                creditService = new CreditService(100);

                from("direct:saga")
                    .saga().propagation(SagaPropagation.REQUIRES_NEW)
                    .log("Creating a new order")
                    .to("direct:newOrder")
                    .log("Taking the credit")
                    .to("direct:reserveCredit")
                    .log("Finalizing")
                    .to("direct:finalize")
                    .log("Done!");

                // Order service
                from("direct:newOrder")
                    .saga()
                    .propagation(SagaPropagation.MANDATORY)
                    .compensation("direct:cancelOrder")
                    .transform().header(Exchange.SAGA_LONG_RUNNING_ACTION)
                    .bean(orderManagerService, "newOrder")
                    .log("Order ${body} created");

                from("direct:cancelOrder")
                    .transform().header(Exchange.SAGA_LONG_RUNNING_ACTION)
                    .bean(orderManagerService, "cancelOrder")
                    .log("Order ${body} cancelled");

                // Credit service
                from("direct:reserveCredit")
                    .saga()
                    .propagation(SagaPropagation.MANDATORY)
                    .compensation("direct:refundCredit")
                    .transform().header(Exchange.SAGA_LONG_RUNNING_ACTION)
                    .bean(creditService, "reserveCredit")
                    .log("Credit ${header.amount} reserved in action ${body}");

                from("direct:refundCredit")
                    .transform().header(Exchange.SAGA_LONG_RUNNING_ACTION)
                    .bean(creditService, "refundCredit")
                    .log("Credit for action ${body} refunded");

                // Final actions
                from("direct:finalize")
                    .saga().propagation(SagaPropagation.NOT_SUPPORTED)
                    .choice()
                    .when(header("fail").isEqualTo(true))
                    .process(x -> {
                        throw new RuntimeException("fail");
                    })
                    .end();
            }
        });

        return camelctx;
    }

    private LRASagaService createLRASagaService() throws Exception {
        LRASagaService sagaService = new LRASagaService();
        sagaService.setCoordinatorUrl(getCoordinatorURL());
        sagaService.setLocalParticipantUrl("http://localhost:8080/lra");
        return sagaService;
    }

    private int getNumberOfActiveLRAs() throws Exception {
        Client client = ClientBuilder.newClient();

        Response response = client.target(getCoordinatorURL() + "/lra-coordinator")
            .request()
            .accept("application/json")
            .get();

        ObjectMapper mapper = new ObjectMapper();
        JsonNode lras = mapper.readTree(InputStream.class.cast(response.getEntity()));
        return lras.size();
    }

    private String getCoordinatorURL() throws Exception {
        return String.format("http://%s:46000", TestUtils.getDockerHost());
    }

    public static class OrderManagerService {

        private Set<String> orders = new HashSet<>();

        public synchronized void newOrder(String id) {
            orders.add(id);
        }

        public synchronized void cancelOrder(String id) {
            orders.remove(id);
        }

        public synchronized Set<String> getOrders() {
            return new TreeSet<>(orders);
        }
    }

    public static class CreditService {

        private int totalCredit;

        private Map<String, Integer> reservations = new HashMap<>();

        public CreditService(int totalCredit) {
            this.totalCredit = totalCredit;
        }

        public synchronized void reserveCredit(String id, @Header("amount") int amount) {
            int credit = getCredit();
            if (amount > credit) {
                throw new IllegalStateException("Insufficient credit");
            }
            reservations.put(id, amount);
        }

        public synchronized void refundCredit(String id) {
            reservations.remove(id);
        }

        public synchronized int getCredit() {
            return totalCredit - reservations.values().stream().reduce(0, (a, b) -> a + b);
        }
    }
}
