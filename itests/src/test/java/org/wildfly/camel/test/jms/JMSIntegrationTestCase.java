/*
 * #%L
 * Wildfly Camel Testsuite
 * %%
 * Copyright (C) 2013 JBoss by Red Hat
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as 
 * published by the Free Software Foundation, either version 2.1 of the 
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public 
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-2.1.html>.
 * #L%
 */

package org.wildfly.camel.test.jms;

import java.io.InputStream;
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
import org.apache.camel.ConsumerTemplate;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.test.integration.common.jms.JMSOperations;
import org.jboss.as.test.integration.common.jms.JMSOperationsProvider;
import org.jboss.gravia.provision.Provisioner;
import org.jboss.gravia.provision.Provisioner.ResourceHandle;
import org.jboss.gravia.resource.IdentityNamespace;
import org.jboss.gravia.resource.ManifestBuilder;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.camel.CamelContextFactory;
import org.wildfly.camel.test.ProvisionerSupport;

/**
 * Test routes that use the jms component in routes.
 *
 * @author thomas.diesler@jboss.com
 * @since 18-May-2013
 */
@RunWith(Arquillian.class)
@ServerSetup({ JMSIntegrationTestCase.JmsQueueSetup.class })
public class JMSIntegrationTestCase {

    static final String QUEUE_NAME = "camel-jms-queue";
    static final String QUEUE_JNDI_NAME = "java:/" + QUEUE_NAME;

    @ArquillianResource
    CamelContextFactory contextFactory;

    @ArquillianResource
    InitialContext initialctx;

    @ArquillianResource
    Provisioner provisioner;

    static class JmsQueueSetup implements ServerSetupTask {

        private JMSOperations jmsAdminOperations;

        @Override
        public void setup(ManagementClient managementClient, String containerId) throws Exception {
            jmsAdminOperations = JMSOperationsProvider.getInstance(managementClient);
            jmsAdminOperations.createJmsQueue(QUEUE_NAME, QUEUE_JNDI_NAME);
        }

        @Override
        public void tearDown(ManagementClient managementClient, String containerId) throws Exception {
            if (jmsAdminOperations != null) {
                jmsAdminOperations.removeJmsQueue(QUEUE_NAME);
                jmsAdminOperations.close();
            }
        }
    }

    @Deployment
    public static JavaArchive createdeployment() {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "camel-jms-tests");
        archive.addClasses(ProvisionerSupport.class);
        archive.setManifest(new Asset() {
            @Override
            public InputStream openStream() {
                ManifestBuilder builder = new ManifestBuilder();
                builder.addManifestHeader("Dependencies", "org.apache.camel,org.jboss.as.controller-client,org.jboss.gravia,org.wildfly.camel,javax.jms.api");
                return builder.openStream();
            }
        });
        return archive;
    }

    @Test
    public void testSendMessage() throws Exception {
        ProvisionerSupport provisionerSupport = new ProvisionerSupport(provisioner);
        List<ResourceHandle> reshandles = provisionerSupport.installCapabilities(IdentityNamespace.IDENTITY_NAMESPACE, "camel.jms.feature");
        try {
            // Create the CamelContext
            CamelContext camelctx = contextFactory.createWildflyCamelContext(getClass().getClassLoader());
            camelctx.addRoutes(new RouteBuilder() {
                @Override
                public void configure() throws Exception {
                    from("jms:queue:" + QUEUE_NAME + "?connectionFactory=ConnectionFactory").
                    transform(body().prepend("Hello ")).to("direct:end");
                }
            });
            camelctx.start();

            // Send a message to the queue
            ConnectionFactory cfactory = (ConnectionFactory) initialctx.lookup("java:/ConnectionFactory");
            Connection connection = cfactory.createConnection();
            sendMessage(connection, QUEUE_JNDI_NAME, "Kermit");

            String result = consumeRouteMessage(camelctx);
            Assert.assertEquals("Hello Kermit", result);

            connection.close();
            camelctx.stop();
        } finally {
            for (ResourceHandle handle : reshandles) {
                handle.uninstall();
            }
        }
    }

    @Test
    public void testReceiveMessage() throws Exception {
        ProvisionerSupport provisionerSupport = new ProvisionerSupport(provisioner);
        List<ResourceHandle> reshandles = provisionerSupport.installCapabilities(IdentityNamespace.IDENTITY_NAMESPACE, "camel.jms.feature");
        try {
            // Create the CamelContext
            CamelContext camelctx = contextFactory.createWildflyCamelContext(getClass().getClassLoader());
            camelctx.addRoutes(new RouteBuilder() {
                @Override
                public void configure() throws Exception {
                    from("direct:start").
                    transform(body().prepend("Hello ")).
                    to("jms:queue:" + QUEUE_NAME + "?connectionFactory=ConnectionFactory");
                }
            });
            camelctx.start();

            final StringBuffer result = new StringBuffer();
            final CountDownLatch latch = new CountDownLatch(1);

            // Get the message from the queue
            ConnectionFactory cfactory = (ConnectionFactory) initialctx.lookup("java:/ConnectionFactory");
            Connection connection = cfactory.createConnection();
            receiveMessage(connection, QUEUE_JNDI_NAME, new MessageListener() {
                @Override
                public void onMessage(Message message) {
                    TextMessage text = (TextMessage) message;
                    try {
                        result.append(text.getText());
                    } catch (JMSException ex) {
                        result.append(ex.getMessage());
                    }
                    latch.countDown();
                }
            });

            ProducerTemplate producer = camelctx.createProducerTemplate();
            producer.asyncSendBody("direct:start", "Kermit");

            Assert.assertTrue(latch.await(10, TimeUnit.SECONDS));
            Assert.assertEquals("Hello Kermit", result.toString());

            connection.close();
            camelctx.stop();
        } finally {
            for (ResourceHandle handle : reshandles) {
                handle.uninstall();
            }
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

    private void receiveMessage(Connection connection, String jndiName, MessageListener listener) throws Exception {
        Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        Destination destination = (Destination) initialctx.lookup(jndiName);
        MessageConsumer consumer = session.createConsumer(destination);
        consumer.setMessageListener(listener);
        connection.start();
    }

    private String consumeRouteMessage(CamelContext camelctx) throws Exception {
        ConsumerTemplate consumer = camelctx.createConsumerTemplate();
        consumer.start();
        return consumer.receiveBody("direct:end", String.class);
    }
}
