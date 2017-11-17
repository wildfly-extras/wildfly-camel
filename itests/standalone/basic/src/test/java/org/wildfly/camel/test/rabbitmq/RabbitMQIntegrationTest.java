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
package org.wildfly.camel.test.rabbitmq;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.qpid.server.Broker;
import org.apache.qpid.server.BrokerOptions;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.camel.test.common.utils.AvailablePortFinder;
import org.wildfly.extension.camel.CamelAware;

@CamelAware
@RunWith(Arquillian.class)
@ServerSetup({ RabbitMQIntegrationTest.QpidSetup.class })
public class RabbitMQIntegrationTest {

    private static final String RABBITMQ_USERNAME = "wfcuser";
    private static final String RABBITMQ_PASSWORD = "p4ssw0rd";

    static class QpidSetup implements ServerSetupTask {

        private Broker broker = new Broker();

        @Override
        public void setup(ManagementClient managementClient, String containerId) throws Exception {
            int port = AvailablePortFinder.getNextAvailable();
            AvailablePortFinder.storeServerData("rabbitmq-port", port);

            String jbossHome = System.getProperty("jboss.home.dir");

            Path workPath = Paths.get(jbossHome, "standalone", "data", "rabbitmq");
            Path configPath = Paths.get(jbossHome, "standalone", "configuration", "rabbitmq");
            Path qpidConfig = configPath.resolve("qpid-config.json");
            configPath.toFile().mkdir();

            BrokerOptions brokerOptions = new BrokerOptions();
            brokerOptions.setConfigProperty("qpid.amqp_port", String.valueOf(port));
            brokerOptions.setConfigProperty("qpid.user", RABBITMQ_USERNAME);
            brokerOptions.setConfigProperty("qpid.password", RABBITMQ_PASSWORD);
            brokerOptions.setConfigProperty("qpid.work_dir", workPath.toString());
            brokerOptions.setInitialConfigurationLocation(qpidConfig.toString());

            Files.copy(RabbitMQIntegrationTest.class.getResourceAsStream("/rabbitmq/qpid-config.json"), qpidConfig, StandardCopyOption.REPLACE_EXISTING);

            broker.startup(brokerOptions);
        }

        @Override
        public void tearDown(ManagementClient managementClient, String containerId) throws Exception {
            broker.shutdown();
        }
    }

    @Deployment
    public static JavaArchive createDeployment() {
        return ShrinkWrap.create(JavaArchive.class, "camel-rabbitmq-tests")
            .addClass(AvailablePortFinder.class);
    }

    @Test
    public void testRabbitMQComponent() throws Exception {
        String port = AvailablePortFinder.readServerData("rabbitmq-port");
        String uri = String.format("rabbitmq:localhost:%s/ex1?vhost=default&username=%s&password=%s", port, RABBITMQ_USERNAME, RABBITMQ_PASSWORD);
        String[] messages = new String[] {"Message 1", "Message 2", "Message 3", "Message 4", "Message 5"};

        CamelContext camelctx = new DefaultCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:start")
                .to(uri);

                from(uri).routeId("rabitmq-consumer").autoStartup(false)
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
            camelctx.startRoute("rabitmq-consumer");

            mockEndpoint.assertIsSatisfied();
        } finally {
            camelctx.stop();
        }
    }
}
