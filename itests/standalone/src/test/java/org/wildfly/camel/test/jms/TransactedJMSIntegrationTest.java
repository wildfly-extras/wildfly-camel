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

import org.apache.camel.CamelContext;
import org.apache.camel.PollingConsumer;
import org.apache.camel.RoutesBuilder;
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
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.jta.JtaTransactionManager;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.Resource;
import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.naming.InitialContext;
import javax.transaction.TransactionManager;
import javax.transaction.UserTransaction;
import java.io.InputStream;

@RunWith(Arquillian.class)
@ServerSetup({TransactedJMSIntegrationTest.JmsQueueSetup.class})
public class TransactedJMSIntegrationTest {

    @ArquillianResource
    InitialContext initialctx;

    @Resource(mappedName = "java:/JmsXA")
    private ConnectionFactory connectionFactory;

    @Resource(mappedName = "java:/TransactionManager")
    private TransactionManager transactionManager;

    @Resource(mappedName = "java:/jboss/UserTransaction")
    private UserTransaction userTransaction;

    private JmsComponent jmsComponent;

    static class JmsQueueSetup implements ServerSetupTask {
        private JMSOperations jmsAdminOperations;

        @Override
        public void setup(ManagementClient managementClient, String containerId) throws Exception {
            for (JmsQueue jmsQueue : JmsQueue.values()) {
                if (!jmsQueue.equals(JmsQueue.DEAD_LETTER_QUEUE)) {
                    jmsAdminOperations = JMSOperationsProvider.getInstance(managementClient);
                    jmsAdminOperations.createJmsQueue(jmsQueue.getQueueName(), jmsQueue.getJndiName());
                }
            }
        }

        @Override
        public void tearDown(ManagementClient managementClient, String containerId) throws Exception {
            if (jmsAdminOperations != null) {
                for (JmsQueue jmsQueue : JmsQueue.values()) {
                    if (!jmsQueue.equals(JmsQueue.DEAD_LETTER_QUEUE)) {
                        jmsAdminOperations.removeJmsQueue(jmsQueue.getQueueName());
                    }
                }
                jmsAdminOperations.close();
            }
        }
    }

    @Deployment
    public static JavaArchive createdeployment() {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "camel-jms-tx-tests");
        archive.setManifest(new Asset() {
            @Override
            public InputStream openStream() {
                ManifestBuilder builder = new ManifestBuilder();
                builder.addManifestHeader("Dependencies", "org.jboss.as.controller-client,javax.jms.api");
                return builder.openStream();
            }
        });
        return archive;
    }

    @Before
    public void setUp() throws Exception {
        JtaTransactionManager jtaTransactionManager = new JtaTransactionManager();
        jtaTransactionManager.setTransactionManager(transactionManager);
        jtaTransactionManager.setUserTransaction(userTransaction);

        TransactionTemplate template = new TransactionTemplate(jtaTransactionManager,
                new DefaultTransactionDefinition(TransactionDefinition.PROPAGATION_REQUIRED));

        SpringTransactionPolicy transactionPolicy = new SpringTransactionPolicy();
        transactionPolicy.setTransactionTemplate(template);
        transactionPolicy.setTransactionManager(jtaTransactionManager);

        initialctx.bind("PROPAGATION_REQUIRED", transactionPolicy);
        initialctx.bind("transactionManager", jtaTransactionManager);

        jmsComponent = JmsComponent.jmsComponentTransacted(connectionFactory, jtaTransactionManager);
    }

    @After
    public void tearDown() throws Exception {
        initialctx.unbind("PROPAGATION_REQUIRED");
        initialctx.unbind("transactionManager");
    }

    @Test
    public void testJMSTransactionToDLQ() throws Exception {
        CamelContext camelctx = new DefaultCamelContext();
        camelctx.addComponent("jms", jmsComponent);
        camelctx.addRoutes(configureJmsRoutes());

        camelctx.start();

        PollingConsumer consumer = camelctx.getEndpoint("direct:dlq").createPollingConsumer();
        consumer.start();

        // Send a message to queue camel-jms-queue-one
        Connection connection = connectionFactory.createConnection();
        sendMessage(connection, JmsQueue.QUEUE_ONE.getJndiName(), "Hello Bob");

        // The JMS transaction should have been rolled back and the message sent to the DLQ
        String result = consumer.receive().getIn().getBody(String.class);
        Assert.assertNotNull(result);
        Assert.assertEquals("Hello Bob", result);

        connection.close();
        camelctx.stop();
    }

    @Test
    public void testJMSTransaction() throws Exception {
        CamelContext camelctx = new DefaultCamelContext();
        camelctx.addComponent("jms", jmsComponent);
        camelctx.addRoutes(configureJmsRoutes());

        camelctx.start();

        PollingConsumer consumer = camelctx.getEndpoint("direct:success").createPollingConsumer();
        consumer.start();

        // Send a message to queue camel-jms-queue-one
        Connection connection = connectionFactory.createConnection();
        sendMessage(connection, JmsQueue.QUEUE_ONE.getJndiName(), "Hello Kermit");

        // The JMS transaction should have been committed and the message payload sent to the direct:success endpoint
        String result = consumer.receive().getIn().getBody(String.class);
        Assert.assertNotNull(result);
        Assert.assertEquals("Hello Kermit", result);

        connection.close();
        camelctx.stop();
    }

    private RoutesBuilder configureJmsRoutes() {
        return new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                errorHandler(deadLetterChannel(JmsQueue.DEAD_LETTER_QUEUE.getCamelEndpointUri())
                .useOriginalMessage()
                .maximumRedeliveries(0)
                .redeliveryDelay(1000));

                from(JmsQueue.QUEUE_ONE.getCamelEndpointUri())
                .transacted("PROPAGATION_REQUIRED")
                .to(JmsQueue.QUEUE_TWO.getCamelEndpointUri());

                from(JmsQueue.QUEUE_TWO.getCamelEndpointUri())
                .choice()
                    .when(body().contains("Bob"))
                        .throwException(new IllegalArgumentException("Invalid name"))
                    .otherwise()
                        .to("direct:success");

                from(JmsQueue.DEAD_LETTER_QUEUE.getCamelEndpointUri())
                .to("direct:dlq");
            }
        };
    }

    private void sendMessage(Connection connection, String jndiName, String message) throws Exception {
        InitialContext initialctx = new InitialContext();
        Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        Destination destination = (Destination) initialctx.lookup(jndiName);
        MessageProducer producer = session.createProducer(destination);
        TextMessage msg = session.createTextMessage(message);
        producer.send(msg);
        connection.start();
    }

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
            return "jms:queue:" + this.queueName;
        }
    }
}
