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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.naming.InitialContext;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.PollingConsumer;
import org.apache.camel.Processor;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.jms.JmsMessage;
import org.apache.camel.impl.DefaultCamelContext;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.camel.test.common.utils.JMSUtils;
import org.wildfly.extension.camel.CamelAware;

/**
 * Test routes that use the jms component in routes.
 *
 * @author thomas.diesler@jboss.com
 * @since 18-May-2013
 */
@CamelAware
@RunWith(Arquillian.class)
@ServerSetup({ JMSIntegrationTest.JmsQueueSetup.class })
public class JMSIntegrationTest {

    static final String QUEUE_NAME = "camel-jms-queue";
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
    public static JavaArchive createdeployment() {
        return ShrinkWrap.create(JavaArchive.class, "camel-jms-tests");
    }

    @Test
    public void testMessageConsumerRoute() throws Exception {

        CamelContext camelctx = new DefaultCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("jms:queue:" + QUEUE_NAME + "?connectionFactory=ConnectionFactory").
                transform(body().prepend("Hello ")).to("direct:end");
            }
        });

        camelctx.start();

        PollingConsumer consumer = camelctx.getEndpoint("direct:end").createPollingConsumer();
        consumer.start();

        try {
            // Send a message to the queue
            ConnectionFactory cfactory = (ConnectionFactory) initialctx.lookup("java:/ConnectionFactory");
            Connection connection = cfactory.createConnection();
            try {
                sendMessage(connection, QUEUE_JNDI_NAME, "Kermit");
                String result = consumer.receive().getIn().getBody(String.class);
                Assert.assertEquals("Hello Kermit", result);
            } finally {
                connection.close();
            }
        } finally {
            camelctx.stop();
        }
    }

    @Test
    public void testMessageConsumerRouteWithClientAck() throws Exception {

        final List<String> result = new ArrayList<>();
        final CountDownLatch latch = new CountDownLatch(3);

        CamelContext camelctx = new DefaultCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("jms:queue:" + QUEUE_NAME + "?connectionFactory=ConnectionFactory&acknowledgementModeName=CLIENT_ACKNOWLEDGE").
                process(new Processor() {
                    @Override
                    public void process(Exchange exchange) throws Exception {
                        JmsMessage in = (JmsMessage) exchange.getIn();
                        Session session = in.getJmsSession();
                        TextMessage message = (TextMessage) in.getJmsMessage();
                        long count = latch.getCount();
                        try {
                            // always append the message text
                            result.add(message.getText() + " " + (4 - count));

                            if (count == 3) {
                                // do nothing on first
                                // note, this message is still acked because of
                                // [CAMEL-8749] JMS message always acknowledged even with CLIENT_ACKNOWLEDGE
                            } else if (count == 2) {
                                // recover causing a redelivery
                                session.recover();
                            } else {
                                // ackknowledge
                                message.acknowledge();
                            }
                        } catch (JMSException ex) {
                            result.add(ex.getMessage());
                        }
                        latch.countDown();
                    }
                }).
                transform(body().prepend("Hello ")).to("direct:end");
            }
        });
        camelctx.start();

        PollingConsumer consumer = camelctx.getEndpoint("direct:end").createPollingConsumer();
        consumer.start();

        try {
            // Send a message to the queue
            ConnectionFactory cfactory = (ConnectionFactory) initialctx.lookup("java:/ConnectionFactory");
            Connection connection = cfactory.createConnection();
            Session session = connection.createSession(false, Session.CLIENT_ACKNOWLEDGE);
            connection.start();

            Destination destination = (Destination) initialctx.lookup(QUEUE_JNDI_NAME);
            MessageProducer producer = session.createProducer(destination);
            try {
                producer.send(session.createTextMessage("Kermit"));
                producer.send(session.createTextMessage("Piggy"));

                Assert.assertTrue(latch.await(10, TimeUnit.SECONDS));
                Assert.assertEquals("Four messages", 3, result.size());
                Assert.assertEquals("Kermit 1", result.get(0));
                Assert.assertEquals("Piggy 2", result.get(1));
                Assert.assertEquals("Piggy 3", result.get(2));

            } finally {
                connection.close();
            }
        } finally {
            camelctx.stop();
        }
    }

    @Test
    public void testMessageProviderRoute() throws Exception {

        CamelContext camelctx = new DefaultCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:start").
                transform(body().prepend("Hello ")).
                to("jms:queue:" + QUEUE_NAME + "?connectionFactory=ConnectionFactory");
            }
        });

        final List<String> result = new ArrayList<>();
        final CountDownLatch latch = new CountDownLatch(1);

        camelctx.start();
        try {
            // Get the message from the queue
            ConnectionFactory cfactory = (ConnectionFactory) initialctx.lookup("java:/ConnectionFactory");
            Connection connection = cfactory.createConnection();
            try {
                receiveMessage(connection, QUEUE_JNDI_NAME, new MessageListener() {
                    @Override
                    public void onMessage(Message message) {
                        TextMessage text = (TextMessage) message;
                        try {
                            result.add(text.getText());
                        } catch (JMSException ex) {
                            result.add(ex.getMessage());
                        }
                        latch.countDown();
                    }
                });

                ProducerTemplate producer = camelctx.createProducerTemplate();
                producer.asyncSendBody("direct:start", "Kermit");

                Assert.assertTrue(latch.await(10, TimeUnit.SECONDS));
                Assert.assertEquals("One message", 1, result.size());
                Assert.assertEquals("Hello Kermit", result.get(0));
            } finally {
                connection.close();
            }
        } finally {
            camelctx.stop();
        }
    }

    @Test
    public void testMessageProviderRouteWithClientAck() throws Exception {

        CamelContext camelctx = new DefaultCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:start").
                transform(body().prepend("Hello ")).
                to("jms:queue:" + QUEUE_NAME + "?connectionFactory=ConnectionFactory");
            }
        });

        final List<String> result = new ArrayList<>();
        final CountDownLatch latch = new CountDownLatch(4);

        camelctx.start();
        try {
            // Get the message from the queue
            ConnectionFactory cfactory = (ConnectionFactory) initialctx.lookup("java:/ConnectionFactory");
            Connection connection = cfactory.createConnection();
            final Session session = connection.createSession(false, Session.CLIENT_ACKNOWLEDGE);
            Destination destination = (Destination) initialctx.lookup(QUEUE_JNDI_NAME);
            MessageConsumer consumer = session.createConsumer(destination);
            consumer.setMessageListener(new MessageListener() {
                @Override
                public synchronized void onMessage(Message message) {
                    TextMessage text = (TextMessage) message;
                    long count = latch.getCount();
                    try {
                        // always append the message text
                        result.add(text.getText() + " " + (5 - count));

                        if (count == 4) {
                            // do nothing on first
                        } else if (count == 3) {
                            // recover causing a redelivery
                            session.recover();
                        } else {
                            // ackknowledge
                            message.acknowledge();
                        }
                    } catch (JMSException ex) {
                        result.add(ex.getMessage());
                    }
                    latch.countDown();
                }
            });
            connection.start();

            try {
                ProducerTemplate producer = camelctx.createProducerTemplate();
                producer.asyncSendBody("direct:start", "Message");
                producer.asyncSendBody("direct:start", "Message");

                Assert.assertTrue(latch.await(10, TimeUnit.SECONDS));
                Assert.assertEquals("Four messages", 4, result.size());
                Assert.assertEquals("Hello Message 1", result.get(0));
                Assert.assertEquals("Hello Message 2", result.get(1));
                Assert.assertEquals("Hello Message 3", result.get(2));
                Assert.assertEquals("Hello Message 4", result.get(3));
            } finally {
                connection.close();
            }
        } finally {
            camelctx.stop();
        }
    }

    private void sendMessage(Connection connection, String jndiName, String message) throws Exception {
        Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        Destination destination = (Destination) initialctx.lookup(jndiName);
        MessageProducer producer = session.createProducer(destination);
        TextMessage msg = session.createTextMessage(message);
        producer.send(msg);
        connection.start();
    }

    private Session receiveMessage(Connection connection, String jndiName, MessageListener listener) throws Exception {
        Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        Destination destination = (Destination) initialctx.lookup(jndiName);
        MessageConsumer consumer = session.createConsumer(destination);
        consumer.setMessageListener(listener);
        connection.start();
        return session;
    }
}
