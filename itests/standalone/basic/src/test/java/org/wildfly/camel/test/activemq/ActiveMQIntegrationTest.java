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

package org.wildfly.camel.test.activemq;

import java.io.File;
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
import javax.naming.NamingException;

import org.apache.activemq.command.ActiveMQTextMessage;
import org.apache.camel.CamelContext;
import org.apache.camel.PollingConsumer;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.support.jndi.JndiBeanRepository;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.ResourceAdapterArchive;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.jms.support.converter.MessageConversionException;
import org.springframework.jms.support.converter.MessageConverter;
import org.wildfly.camel.test.common.utils.DMRUtils;
import org.wildfly.extension.camel.CamelAware;

@CamelAware
@RunWith(Arquillian.class)
@ServerSetup({ActiveMQIntegrationTest.ActiveMQRarSetupTask.class})
@Ignore("[#2783] No ActiveMQ connection factory with RAR deployment")
public class ActiveMQIntegrationTest {

    private static final String ACTIVEMQ_RAR = "activemq-rar.rar";
    private static final String QUEUE_NAME = "testQueue";

    @ArquillianResource
    private InitialContext context;

    static class ActiveMQRarSetupTask implements ServerSetupTask {

        @Override
        public void setup(ManagementClient managementClient, String containerId) throws Exception {
            ModelNode batchNode = DMRUtils.batchNode()
                .addStep("subsystem=resource-adapters/resource-adapter=amq-ra.rar", "add(archive=activemq-rar.rar)")
                .addStep("subsystem=resource-adapters/resource-adapter=amq-ra.rar", "write-attribute(name=transaction-support,value=NoTransaction)")
                .addStep("subsystem=resource-adapters/resource-adapter=amq-ra.rar/connection-definitions=QueueConnectionFactory", "add(class-name=org.apache.activemq.ra.ActiveMQManagedConnectionFactory, jndi-name=java:/ActiveMQConnectionFactory)")
                .addStep("subsystem=resource-adapters/resource-adapter=amq-ra.rar/connection-definitions=QueueConnectionFactory/config-properties=ServerUrl", "add(value=vm://localhost?broker.persistent=false&broker.useJmx=false&broker.useShutdownHook=false)")
                .addStep("subsystem=resource-adapters/resource-adapter=amq-ra.rar/admin-objects=OrdersQueue", "add(class-name=org.apache.activemq.command.ActiveMQQueue, jndi-name=java:/OrdersQueue)")
                .addStep("subsystem=resource-adapters/resource-adapter=amq-ra.rar/admin-objects=OrdersQueue/config-properties=PhysicalName", "add(value=OrdersQueue)")
                .build();
            managementClient.getControllerClient().execute(batchNode);
        }

        @Override
        public void tearDown(ManagementClient managementClient, String containerId) throws Exception {
            ModelNode batchNode = DMRUtils.batchNode()
                .addStep("subsystem=resource-adapters/resource-adapter=amq-ra.rar", "remove")
                .build();
            managementClient.getControllerClient().execute(batchNode);
        }
    }

    @Deployment
    public static JavaArchive createDeployment() {
        return ShrinkWrap.create(JavaArchive.class, "camel-activemq-tests.jar");
    }

    @Deployment(name = ACTIVEMQ_RAR, testable = false, order = 1)
    public static ResourceAdapterArchive createRarDeployment() {
        return ShrinkWrap.createFromZipFile(ResourceAdapterArchive.class, new File("target/dependencies/" + ACTIVEMQ_RAR));
    }

    @Test
    public void testSendMessage() throws Exception {
        CamelContext camelctx = new DefaultCamelContext(new JndiBeanRepository());
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                fromF("activemq:queue:%s?connectionFactory=java:/ActiveMQConnectionFactory", QUEUE_NAME).
                transform(simple("Hello ${body}")).
                to("seda:end");
            }
        });

        PollingConsumer pollingConsumer = camelctx.getEndpoint("seda:end").createPollingConsumer();
        pollingConsumer.start();

        camelctx.start();
        try {
            ConnectionFactory connectionFactory = lookupConnectionFactory();
            Connection con = connectionFactory.createConnection();
            try {
                sendMessage(con, "Kermit");
                String result = pollingConsumer.receive(3000).getIn().getBody(String.class);
                Assert.assertEquals("Hello Kermit", result);
            } finally {
                con.close();
            }
        } finally {
            camelctx.close();
        }
    }

    @Test
    public void testReceiveMessage() throws Exception {
        CamelContext camelctx = new DefaultCamelContext(new JndiBeanRepository());
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:start").
                transform(simple("Hello ${body}")).
                toF("activemq:queue:%s?connectionFactory=java:/ActiveMQConnectionFactory", QUEUE_NAME);
            }
        });

        final StringBuffer result = new StringBuffer();
        final CountDownLatch latch = new CountDownLatch(1);

        camelctx.start();
        try {
            ConnectionFactory connectionFactory = lookupConnectionFactory();
            Connection con = connectionFactory.createConnection();
            try {
                receiveMessage(con, message -> {
                    TextMessage text = (TextMessage) message;
                    try {
                        result.append(text.getText());
                    } catch (JMSException ex) {
                        result.append(ex.getMessage());
                    }
                    latch.countDown();
                });

                ProducerTemplate producer = camelctx.createProducerTemplate();
                producer.asyncSendBody("direct:start", "Kermit");

                Assert.assertTrue(latch.await(10, TimeUnit.SECONDS));
                Assert.assertEquals("Hello Kermit", result.toString());
            } finally {
                con.close();
            }
        } finally {
            camelctx.close();
        }
    }

    @Test
    public void testCustomMessageConverter() throws Exception {
        MessageConverter converter = new MessageConverter() {
            @Override
            public Message toMessage(Object o, Session session) throws JMSException, MessageConversionException {
                return null;
            }

            @Override
            public Object fromMessage(Message message) throws JMSException, MessageConversionException {
                TextMessage originalMessage = (TextMessage) message;
                TextMessage modifiedMessage = new ActiveMQTextMessage();
                modifiedMessage.setText(originalMessage.getText() + " Modified");
                return modifiedMessage;
            }
        };
        context.bind("messageConverter", converter);

        CamelContext camelctx = new DefaultCamelContext(new JndiBeanRepository());
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                fromF("activemq:queue:%s?connectionFactory=java:/ActiveMQConnectionFactory&messageConverter=#messageConverter", QUEUE_NAME).
                transform(simple("Hello ${body.getText()}")).
                to("seda:end");
            }
        });

        PollingConsumer pollingConsumer = camelctx.getEndpoint("seda:end").createPollingConsumer();
        pollingConsumer.start();

        camelctx.start();
        try {
            ConnectionFactory connectionFactory = lookupConnectionFactory();
            Connection con = connectionFactory.createConnection();
            try {
                sendMessage(con, "Kermit");
                String result = pollingConsumer.receive(3000).getIn().getBody(String.class);
                Assert.assertEquals("Hello Kermit Modified", result);
            } finally {
                con.close();
            }
        } finally {
            camelctx.close();
            context.unbind("messageConverter");
        }
    }

    private ConnectionFactory lookupConnectionFactory() throws NamingException {
        return (ConnectionFactory) context.lookup("java:/ActiveMQConnectionFactory");
    }

    private void sendMessage(Connection connection, String message) throws Exception {
        Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        Destination destination = session.createQueue(QUEUE_NAME);
        MessageProducer producer = session.createProducer(destination);
        TextMessage msg = session.createTextMessage(message);
        producer.send(msg);
        connection.start();
    }

    private void receiveMessage(Connection connection, MessageListener listener) throws Exception {
        Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        Destination destination = session.createQueue(QUEUE_NAME);
        MessageConsumer consumer = session.createConsumer(destination);
        consumer.setMessageListener(listener);
        connection.start();
    }
}
