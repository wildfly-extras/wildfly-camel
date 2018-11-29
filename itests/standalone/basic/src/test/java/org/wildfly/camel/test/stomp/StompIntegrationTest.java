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

import org.apache.camel.CamelContext;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.impl.DefaultCamelContext;
import org.fusesource.stomp.client.BlockingConnection;
import org.fusesource.stomp.client.Stomp;
import org.fusesource.stomp.codec.StompFrame;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.camel.test.common.utils.UserManager;
import org.wildfly.camel.test.common.utils.WildFlyCli;
import org.wildfly.extension.camel.CamelAware;

@CamelAware
@RunWith(Arquillian.class)
@ServerSetup({StompIntegrationTest.StompConnectorSetupTask.class})
@Ignore("[#2674] Intermittent failure of StompIntegrationTest")
public class StompIntegrationTest {

    private static final int STOMP_PORT = 61613;
    private static final String BROKER_URL = "tcp://localhost:" + STOMP_PORT;
    private static final String QUEUE_NAME = "wfc";
    private static final String USERNAME = "stomp-user";
    private static final String PASSWORD = "stomp-password";

    static class StompConnectorSetupTask implements ServerSetupTask {
        @Override
        public void setup(ManagementClient managementClient, String containerId) throws Exception {
            String cliScript = "/subsystem=messaging-activemq/server=default/acceptor=stomp-acceptor"
                + ":add(factory-class=org.apache.activemq.artemis.core.remoting.impl.netty.NettyAcceptorFactory,"
                + "params={protocols=STOMP, port=" + STOMP_PORT + "})\n"
                + "reload";

            new WildFlyCli().run(cliScript).assertSuccess();

            try (UserManager userManager = UserManager.forStandaloneApplicationRealm()) {
                userManager.addUser(USERNAME, PASSWORD);
                userManager.addRole(USERNAME, "guest");
            }
        }

        @Override
        public void tearDown(ManagementClient managementClient, String containerId) throws Exception {
            String cliScript = "/subsystem=messaging-activemq/server=default/acceptor=stomp-acceptor:remove\n"
                + "reload";
            new WildFlyCli().run(cliScript).assertSuccess();

            try (UserManager userManager = UserManager.forStandaloneApplicationRealm()) {
                userManager.removeUser(USERNAME);
                userManager.removeRole(USERNAME, "guest");
            }
        }
    }

    @Deployment
    public static JavaArchive createDeployment() {
        return ShrinkWrap.create(JavaArchive.class, "camel-stomp-tests.jar");
    }

    @Test
    public void testMessageConsumerRoute() throws Exception {
        CamelContext camelctx = new DefaultCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                fromF("stomp:%s?login=%s&passcode=%s&brokerURL=%s", QUEUE_NAME, USERNAME, PASSWORD, BROKER_URL)
                .transform(body().prepend("Hello "))
                .to("mock:result");
            }
        });

        camelctx.start();

        MockEndpoint mockEndpoint = camelctx.getEndpoint("mock:result", MockEndpoint.class);
        mockEndpoint.expectedBodiesReceived("Hello Kermit");
        mockEndpoint.setResultWaitTime(30000);

        Stomp stomp = new Stomp(BROKER_URL);
        stomp.setLogin(USERNAME);
        stomp.setPasscode(PASSWORD);

        final BlockingConnection producerConnection = stomp.connectBlocking();
        try {
            StompFrame outFrame = new StompFrame(SEND);
            outFrame.addHeader(DESTINATION, StompFrame.encodeHeader(QUEUE_NAME));
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
