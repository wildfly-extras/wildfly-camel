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
package org.wildfly.camel.test.amqp;

import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.apache.activemq.broker.BrokerService;
import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.amqp.AMQPConnectionDetails;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.support.jndi.JndiBeanRepository;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.camel.test.common.utils.AvailablePortFinder;
import org.wildfly.extension.camel.CamelAware;

@CamelAware
@RunWith(Arquillian.class)
@ServerSetup({ AMQPIntegrationTest.ServerSetup.class })
public class AMQPIntegrationTest {

    @ArquillianResource
    private InitialContext context;

    static class ServerSetup implements ServerSetupTask {

        private BrokerService broker = new BrokerService();

        @Override
        public void setup(ManagementClient managementClient, String containerId) throws Exception {
            int port = AvailablePortFinder.getNextAvailable();
            AvailablePortFinder.storeServerData("amqp-port", port);
            broker.setPersistent(false);
            broker.addConnector("amqp://0.0.0.0:" + port);
            broker.start();
        }

        @Override
        public void tearDown(ManagementClient managementClient, String containerId) throws Exception {
            broker.stop();
        }
    }

    @Before
    public void setUp() throws Exception {
        String connection = String.format("amqp://localhost:%s", AvailablePortFinder.readServerData("amqp-port"));
        AMQPConnectionDetails connectionDetails = new AMQPConnectionDetails(connection);
        context.bind("AMQPConnectionDetails", connectionDetails);
    }

    @After
    public void tearDown() {
        try {
            context.unbind("AMQPConnectionDetails");
        } catch (NamingException e) {
            // Ignored
        }
    }

    @Deployment
    public static JavaArchive createDeployment() {
        return ShrinkWrap.create(JavaArchive.class, "camel-amqp-tests.jar")
            .addClass(AvailablePortFinder.class);
    }

    @Test
    public void testAMQPComponent() throws Exception {
        String[] messages = new String[] {"Message 1", "Message 2", "Message 3", "Message 4", "Message 5"};

        CamelContext camelctx = new DefaultCamelContext(new JndiBeanRepository());
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:start")
                .to("amqp:queue:test");

                from("amqp:queue:test").routeId("amqp-consumer").autoStartup(false)
                .to("mock:result");
            }
        });

        MockEndpoint mockEndpoint = camelctx.getEndpoint("mock:result", MockEndpoint.class);
        mockEndpoint.expectedBodiesReceived(messages);

        camelctx.start();
        try {
            ProducerTemplate template = camelctx.createProducerTemplate();

            // Enqueue messages
            for (String message : messages) {
                template.sendBody("direct:start", message);
            }

            // Start the consumer route
            camelctx.getRouteController().startRoute("amqp-consumer");

            mockEndpoint.assertIsSatisfied();
        } finally {
            camelctx.stop();
        }
    }
}
