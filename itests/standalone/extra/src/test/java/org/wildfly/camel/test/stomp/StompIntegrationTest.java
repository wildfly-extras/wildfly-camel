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

package org.wildfly.camel.test.stomp;

import static org.fusesource.hawtbuf.UTF8Buffer.utf8;
import static org.fusesource.stomp.client.Constants.DESTINATION;
import static org.fusesource.stomp.client.Constants.MESSAGE_ID;
import static org.fusesource.stomp.client.Constants.SEND;

import java.util.UUID;

import javax.naming.InitialContext;

import org.apache.camel.CamelContext;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.impl.DefaultCamelContext;
import org.fusesource.stomp.client.BlockingConnection;
import org.fusesource.stomp.client.Stomp;
import org.fusesource.stomp.codec.StompFrame;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.camel.test.common.utils.JMSUtils;
import org.wildfly.extension.camel.CamelAware;

/**
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 */
@CamelAware
@RunWith(Arquillian.class)
@ServerSetup({ StompIntegrationTest.JmsQueueSetup.class })
public class StompIntegrationTest {

    private static final int STOMP_PORT = 61613;
    static final String QUEUE_NAME = "stompq";
    static final String QUEUE_JNDI_NAME = "java:/" + QUEUE_NAME;
    static final String ACTIVEMQ_USER = "user1";
    static final String ACTIVEMQ_PASSWORD = "appl-pa$$wrd1";

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
        return ShrinkWrap.create(JavaArchive.class, "camel-stomp-tests");
    }

    @Test
    public void testMessageConsumerRoute() throws Exception {

        CamelContext camelctx = new DefaultCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                fromF("stomp:jms.queue.%s?brokerURL=tcp://127.0.0.1:%d&login=%s&passcode=%s", QUEUE_NAME, STOMP_PORT,
                        ACTIVEMQ_USER, ACTIVEMQ_PASSWORD) //
                                .transform(body().method("utf8").prepend("Hello "))//
                                .to("mock:result");
            }
        });

        camelctx.start();

        MockEndpoint mockEndpoint = camelctx.getEndpoint("mock:result", MockEndpoint.class);
        mockEndpoint.setExpectedCount(1);
        mockEndpoint.expectedBodiesReceived("Hello Kermit");

        Stomp stomp = new Stomp("tcp://127.0.0.1:" + STOMP_PORT);
        stomp.setLogin(ACTIVEMQ_USER);
        stomp.setPasscode(ACTIVEMQ_PASSWORD);
        final BlockingConnection producerConnection = stomp.connectBlocking();
        try {
            StompFrame outFrame = new StompFrame(SEND);
            outFrame.addHeader(DESTINATION, StompFrame.encodeHeader("jms.queue." + QUEUE_NAME));
            outFrame.addHeader(MESSAGE_ID, StompFrame
                    .encodeHeader("StompIntegrationTest.testMessageConsumerRoute" + UUID.randomUUID().toString()));
            outFrame.content(utf8("Kermit"));
            producerConnection.send(outFrame);

            mockEndpoint.assertIsSatisfied();
        } finally {
            if (producerConnection != null) {
                producerConnection.close();
            }
            camelctx.stop();
        }
    }

}
