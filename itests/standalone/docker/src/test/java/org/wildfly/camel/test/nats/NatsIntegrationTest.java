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

import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.component.nats.NatsComponent;
import org.apache.camel.component.nats.NatsConsumer;
import org.apache.camel.impl.DefaultCamelContext;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.camel.test.common.utils.TestUtils;
import org.wildfly.camel.test.dockerjava.DockerManager;
import org.wildfly.extension.camel.CamelAware;

import io.nats.client.Connection;
import io.nats.client.Nats;
import io.nats.client.Options;

@CamelAware
@RunWith(Arquillian.class)
@ServerSetup({NatsIntegrationTest.ContainerSetupTask.class})
public class NatsIntegrationTest {

    private static final String CONTAINER_NAME = "nats";

    @Deployment
    public static JavaArchive createDeployment() {
        return ShrinkWrap.create(JavaArchive.class, "camel-nats-tests.jar")
            .addClasses(TestUtils.class);
    }

    static class ContainerSetupTask implements ServerSetupTask {

    	private DockerManager dockerManager;

        @Override
        public void setup(ManagementClient managementClient, String someId) throws Exception {
        	
			/*
			docker run --detach \
				--name nats \
				-p 4222:4222 \
				nats:0.9.6
			*/
        	
        	dockerManager = new DockerManager()
        			.createContainer("nats:0.9.6", true)
        			.withName(CONTAINER_NAME)
        			.withPortBindings("4222:4222")
        			.startContainer();

			dockerManager
					.withAwaitLogMessage("Server is ready")
					.awaitCompletion(60, TimeUnit.SECONDS);
        }

        @Override
        public void tearDown(ManagementClient managementClient, String someId) throws Exception {
        	if (dockerManager != null) {
            	dockerManager.removeContainer();
        	}
        }
    }

    @Test
    public void testNatsRoutes() throws Exception {

        CamelContext camelctx = new DefaultCamelContext();
        try {
            camelctx.addRoutes(new RouteBuilder() {
                @Override
                public void configure() throws Exception {
                    from("nats:test").id("nats-route")
                    .to("mock:result");
                }
            });

            NatsComponent nats = camelctx.getComponent("nats", NatsComponent.class);
            nats.setServers(TestUtils.getDockerHost() + ":4222");

            MockEndpoint to = camelctx.getEndpoint("mock:result", MockEndpoint.class);
            to.expectedMessageCount(1);

            camelctx.start();

            // Make sure the consumer has subscribed to the topic before sending messages
            NatsConsumer consumer = (NatsConsumer) camelctx.getRoute("nats-route").getConsumer();
            int count = 0;
            while(!consumer.isActive()) {
                Thread.sleep(500);
                count += 1;
                if (count > 10) {
                    throw new IllegalStateException("Gave up waiting for nats consumer to subscribe to topic");
                }
            }

            Options options = new Options.Builder().server("nats://" + TestUtils.getDockerHost() + ":4222").build();
            Connection connection = Nats.connect(options);

            final byte[] payload = "test-message".getBytes(StandardCharsets.UTF_8);
            connection.publish("test", payload);

            to.assertIsSatisfied(5000);

            Exchange exchange = to.getExchanges().get(0);
            Assert.assertNotNull(exchange);

            Message message = exchange.getMessage();
            String body = message.getBody(String.class);
            Assert.assertEquals("test-message", body);

        } finally {
            camelctx.close();
        }
    }
}
