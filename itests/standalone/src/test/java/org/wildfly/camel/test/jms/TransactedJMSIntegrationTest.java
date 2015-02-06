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

package org.wildfly.camel.test.jms;

import java.io.InputStream;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.jms.XAQueueConnectionFactory;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.transaction.TransactionManager;

import org.apache.camel.CamelContext;
import org.apache.camel.ConsumerTemplate;
import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.jms.JmsComponent;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.spring.spi.SpringTransactionPolicy;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.test.integration.common.jms.JMSOperations;
import org.jboss.as.test.integration.common.jms.JMSOperationsProvider;
import org.jboss.gravia.resource.ManifestBuilder;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.transaction.jta.JtaTransactionManager;

@RunWith(Arquillian.class)
@ServerSetup({TransactedJMSIntegrationTest.JmsQueueSetup.class})
public class TransactedJMSIntegrationTest {

    @ArquillianResource
    InitialContext initialctx;

    private enum JmsQueue {
        QUEUE_ONE("camel-jms-queue-one"),
        QUEUE_TWO("camel-jms-queue-two"),
        DEAD_LETTER_QUEUE("DLQ");

        private final String queueName;

        JmsQueue(String queueName) {
            this.queueName = queueName;
        }

        public String getQueueName() {
            return this.queueName;
        }

        public String getJndiName() {
            return "java:/jms/queue/" + this.queueName;
        }

        public String getCamelEndpointUri() {
            return "jms:queue:" + this.queueName + "?connectionFactory=JmsXA";
        }
    }

    static class JmsQueueSetup implements ServerSetupTask {

        private JMSOperations jmsAdminOperations;

        @Override
        public void setup(ManagementClient managementClient, String containerId) throws Exception {
            for (JmsQueue jmsQueue : JmsQueue.values()) {
                jmsAdminOperations = JMSOperationsProvider.getInstance(managementClient);
                jmsAdminOperations.createJmsQueue(jmsQueue.getQueueName(), jmsQueue.getJndiName());
            }
        }

        @Override
        public void tearDown(ManagementClient managementClient, String containerId) throws Exception {
            if (jmsAdminOperations != null) {
                for (JmsQueue jmsQueue : JmsQueue.values()) {
                    jmsAdminOperations.removeJmsQueue(jmsQueue.getQueueName());
                }
                jmsAdminOperations.close();
            }
        }
    }

    @Deployment
    public static JavaArchive createdeployment() {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "camel-jms-tests");
        archive.setManifest(new Asset() {
            @Override
            public InputStream openStream() {
                ManifestBuilder builder = new ManifestBuilder();
                builder.addManifestHeader("Dependencies", "org.jboss.as.controller-client,javax.jms.api,org.apache.camel.spring,org.springframework.tx");
                return builder.openStream();
            }
        });
        return archive;
    }

    @Before
    public void setUp() throws Exception {
        configureTransactionality();
    }

    @After
    public void tearDown() throws Exception {
        initialctx.unbind("PROPAGATION_REQUIRED");
        initialctx.unbind("transactionManager");
    }

    @Test
    public void testJMSTransactionToDLQ() throws Exception {
        // Create the CamelContext
        CamelContext camelctx = new DefaultCamelContext();
        camelctx.addComponent("jms", configureJMSComponent());

        camelctx.addRoutes(configureRouteBuilder());
        camelctx.start();

        // Send a message to the queue
        ConnectionFactory connectionFactory = (ConnectionFactory) initialctx.lookup("java:/JmsXA");
        Connection connection = connectionFactory.createConnection();
        sendMessage(connection, JmsQueue.QUEUE_ONE.getJndiName(), "Hello Bob");

        // Forwarding the hello message on to QUEUE_TWO should have been rolled back
        String result = consumeMessageFromEndpoint(camelctx, JmsQueue.QUEUE_TWO.getCamelEndpointUri());
        Assert.assertNull(result);

        // The message should have been placed onto the dead letter queue
        result = consumeMessageFromEndpoint(camelctx, JmsQueue.DEAD_LETTER_QUEUE.getCamelEndpointUri());
        Assert.assertNotNull(result);
        Assert.assertEquals("Hello Bob", result);

        connection.close();
        camelctx.stop();
    }

    @Test
    public void testJMSTransaction() throws Exception {
        // Create the CamelContext
        CamelContext camelctx = new DefaultCamelContext();
        camelctx.addComponent("jms", configureJMSComponent());

        camelctx.addRoutes(configureRouteBuilder());
        camelctx.start();

        // Send a message to the queue
        ConnectionFactory connectionFactory = (ConnectionFactory) initialctx.lookup("java:/JmsXA");
        Connection connection = connectionFactory.createConnection();
        sendMessage(connection, JmsQueue.QUEUE_ONE.getJndiName(), "Hello Kermit");

        // Verify that the message was forwarded to QUEUE_TWO
        String result = consumeMessageFromEndpoint(camelctx, JmsQueue.QUEUE_TWO.getCamelEndpointUri());
        Assert.assertEquals("Hello Kermit", result);

        connection.close();
        camelctx.stop();
    }

    private void sendMessage(Connection connection, String jndiName, String message) throws Exception {
        Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        Destination destination = (Destination) initialctx.lookup(jndiName);
        MessageProducer producer = session.createProducer(destination);
        TextMessage msg = session.createTextMessage(message);
        producer.send(msg);
        connection.start();
    }

    private String consumeMessageFromEndpoint(CamelContext camelctx, String endpoint) throws Exception {
        String result = null;

        ConsumerTemplate consumer = camelctx.createConsumerTemplate();
        consumer.start();

        Exchange exchange = consumer.receive(endpoint, 2000);
        if (exchange != null) {
            if (exchange.hasOut()) {
                result = exchange.getOut().getBody(String.class);
            } else {
                result = exchange.getIn().getBody(String.class);
            }
        }

        return result;
    }

    private RouteBuilder configureRouteBuilder() {
        return new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                errorHandler(deadLetterChannel(JmsQueue.DEAD_LETTER_QUEUE.getCamelEndpointUri())
                        .maximumRedeliveries(1)
                        .redeliveryDelay(1000));

                from(JmsQueue.QUEUE_ONE.getCamelEndpointUri())
                        .transacted("PROPAGATION_REQUIRED")
                        .to("direct:greet")
                        .to(JmsQueue.QUEUE_TWO.getCamelEndpointUri());

                from("direct:greet")
                        .choice()
                        .when(body().contains("Bob"))
                        .throwException(new IllegalArgumentException("Invalid name"))
                        .otherwise()
                        .log("{body}");
            }
        };
    }

    private void configureTransactionality() throws NamingException {
        JtaTransactionManager jtaTransactionManager = configureJtaTransactionManager();
        SpringTransactionPolicy transactionPolicy = configureTransactionPolicy(jtaTransactionManager);
        configureJndiRegistry(jtaTransactionManager, transactionPolicy);
    }

    private void configureJndiRegistry(JtaTransactionManager jtaTransactionManager, SpringTransactionPolicy transactionPolicy) throws NamingException {
        initialctx.bind("PROPAGATION_REQUIRED", transactionPolicy);
        initialctx.bind("transactionManager", jtaTransactionManager);
    }

    private JtaTransactionManager configureJtaTransactionManager() throws NamingException {
        TransactionManager transactionManger = (TransactionManager) initialctx.lookup("java:/TransactionManager");
        return new JtaTransactionManager(transactionManger);
    }

    private SpringTransactionPolicy configureTransactionPolicy(JtaTransactionManager jtaTransactionManager) {
        SpringTransactionPolicy transactionPolicy = new SpringTransactionPolicy();
        transactionPolicy.setTransactionManager(jtaTransactionManager);
        transactionPolicy.setPropagationBehaviorName("PROPAGATION_REQUIRED");
        return transactionPolicy;
    }

    private JmsComponent configureJMSComponent() throws NamingException {
        XAQueueConnectionFactory connectionFactory = (XAQueueConnectionFactory) initialctx.lookup("java:/JmsXA");

        JmsComponent jmsComponent = new JmsComponent();
        jmsComponent.setConnectionFactory(connectionFactory);
        jmsComponent.setCacheLevelName("CACHE_NONE");
        return jmsComponent;
    }
}
