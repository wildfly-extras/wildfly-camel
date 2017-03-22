/*
 * #%L
 * Wildfly Camel :: Testsuite
 * %%
 * Copyright (C) 2013 - 2014 RedHat
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

package org.wildfly.camel.test.sjms;

import java.util.ArrayList;
import java.util.List;

import javax.naming.InitialContext;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.processor.aggregate.AggregationStrategy;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.camel.test.common.utils.EnvironmentUtils;
import org.wildfly.camel.test.common.utils.JMSUtils;
import org.wildfly.extension.camel.CamelAware;

@CamelAware
@RunWith(Arquillian.class)
@ServerSetup({ SJMSBatchIntegrationTest.JmsQueueSetup.class })
public class SJMSBatchIntegrationTest {

    static final String QUEUE_NAME = "sjms-queue";
    static final String QUEUE_JNDI_NAME = "java:/" + QUEUE_NAME;

    @ArquillianResource
    InitialContext initialctx;

    static class JmsQueueSetup implements ServerSetupTask {

        @Override
        public void setup(ManagementClient managementClient, String containerId) throws Exception {
            JMSUtils.createJmsQueue(QUEUE_NAME, QUEUE_JNDI_NAME, managementClient.getControllerClient());
        }

        @Override
        public void tearDown(ManagementClient managementClient, String containerId) throws Exception {
            JMSUtils.removeJmsQueue(QUEUE_NAME, managementClient.getControllerClient());
        }
    }

    @Deployment
    public static JavaArchive createDeployment() {
        return ShrinkWrap.create(JavaArchive.class, "camel-sjms-batch-tests")
            .addClasses(EnvironmentUtils.class);
    }

    @Before
    public void setUp() throws Exception {
        initialctx.bind("aggregationStrategy", new ListAggregationStrategy());
    }

    @After
    public void tearDown() throws Exception {
        initialctx.unbind("aggregationStrategy");
    }

    @Test
    public void testBatchMessageConsumerRoute() throws Exception {

        Assume.assumeFalse("[#1707] SJMSBatchIntegrationTest fails on AIX", EnvironmentUtils.isAIX());

        int messageCount = 5000;
        int consumerCount = 5;
        int completionTimeout = 1000;
        int completionSize = 200;

        CamelContext camelctx = new DefaultCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                fromF("sjms-batch:%s?completionTimeout=%s&completionSize=%s&consumerCount=%s&aggregationStrategy=%s&connectionFactory=%s",
                    QUEUE_NAME, completionTimeout, completionSize, consumerCount, "#aggregationStrategy", "ConnectionFactory")
                    .routeId("batchConsumer").autoStartup(false)
                    .split(body())
                    .to("mock:split");

                from("direct:in")
                    .split(body())
                    .toF("sjms:queue:%s?transacted=true&connectionFactory=ConnectionFactory", QUEUE_NAME)
                    .to("mock:before");
            }
        });

        MockEndpoint mockBefore = camelctx.getEndpoint("mock:before", MockEndpoint.class);
        mockBefore.setExpectedMessageCount(messageCount);

        MockEndpoint mockSplit = camelctx.getEndpoint("mock:split", MockEndpoint.class);
        mockSplit.setExpectedMessageCount(messageCount);

        camelctx.start();
        try {
            ProducerTemplate template = camelctx.createProducerTemplate();
            String[] messages = new String[messageCount];
            for (int i = 0; i < messageCount; i++) {
                messages[i] = "message:" + i;
            }

            // Send messages to the test queue
            template.sendBody("direct:in", messages);
            mockBefore.assertIsSatisfied();

            // Start up the batch consumer route
            camelctx.startRoute("batchConsumer");
            mockSplit.assertIsSatisfied();
        } finally {
            camelctx.stop();
        }
    }

    private final class ListAggregationStrategy implements AggregationStrategy {
        @Override
        public Exchange aggregate(Exchange oldExchange, Exchange newExchange) {
            String body = newExchange.getIn().getBody(String.class);
            if (oldExchange == null) {
                List<String> list = new ArrayList<>();
                list.add(body);
                newExchange.getIn().setBody(list);
                return newExchange;
            } else {
                List<String> list = oldExchange.getIn().getBody(List.class);
                list.add(body);
                return oldExchange;
            }
        }
    }
}
