/*
 * #%L
 * Wildfly Camel :: Testsuite
 * %%
 * Copyright (C) 2013 - 2016 RedHat
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
package org.wildfly.camel.test.nats;

import io.nats.client.Connection;
import io.nats.client.ConnectionFactory;

import java.util.Properties;

import org.apache.camel.CamelContext;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.component.nats.NatsConsumer;
import org.apache.camel.impl.DefaultCamelContext;
import org.arquillian.cube.CubeController;
import org.arquillian.cube.requirement.ArquillianConditionalRunner;
import org.arquillian.cube.requirement.RequiresEnvironmentVariable;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.camel.test.common.utils.TestUtils;
import org.wildfly.extension.camel.CamelAware;

@CamelAware
@RunWith(ArquillianConditionalRunner.class)
@RequiresEnvironmentVariable({"DOCKER_HOST"})
public class NatsIntegrationTest {

    private static final String CONTAINER_NATS = "nats";

    @ArquillianResource
    private CubeController cubeController;

    @Deployment
    public static JavaArchive createDeployment() {
        return ShrinkWrap.create(JavaArchive.class, "camel-nats-tests.jar")
            .addClass(TestUtils.class);
    }

    @Before
    public void setUp() {
        cubeController.create(CONTAINER_NATS);
        cubeController.start(CONTAINER_NATS);
    }

    @After
    public void tearDown() {
        cubeController.stop(CONTAINER_NATS);
        cubeController.destroy(CONTAINER_NATS);
    }

    @Test
    public void testNatsRoutes() throws Exception {
        CamelContext camelctx = new DefaultCamelContext();
        try {
            camelctx.addRoutes(new RouteBuilder() {
                @Override
                public void configure() throws Exception {
                    from("nats://" + TestUtils.getDockerHost() + ":4222?topic=test").id("nats-route")
                    .to("mock:result");
                }
            });

            MockEndpoint to = camelctx.getEndpoint("mock:result", MockEndpoint.class);
            to.expectedBodiesReceived("{Subject=test;Reply=null;Payload=<test-message>}");
            to.expectedMessageCount(1);

            camelctx.start();

            // Make sure the consumer has subscribed to the topic before sending messages
            NatsConsumer consumer = (NatsConsumer) camelctx.getRoute("nats-route").getConsumer();
            int count = 0;
            while(!consumer.isSubscribed()) {
                Thread.sleep(500);
                count += 1;
                if (count > 10) {
                    throw new IllegalStateException("Gave up waiting for nats consumer to subscribe to topic");
                }
            }

            Properties opts = new Properties();
            opts.put("servers", "nats://localhost:4222");

            ConnectionFactory factory = new ConnectionFactory(opts);
            Connection connection = factory.createConnection();
            connection.publish("test", "test-message".getBytes());
            
            to.assertIsSatisfied(5000);

        } finally {
            camelctx.stop();
        }
    }
}
