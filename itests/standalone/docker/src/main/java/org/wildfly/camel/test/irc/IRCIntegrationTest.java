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

package org.wildfly.camel.test.irc;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.irc.IrcComponent;
import org.apache.camel.component.irc.IrcEndpoint;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.impl.DefaultCamelContext;
import org.arquillian.cube.CubeController;
import org.arquillian.cube.docker.impl.requirement.RequiresDocker;
import org.arquillian.cube.requirement.ArquillianConditionalRunner;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.schwering.irc.lib.IRCConnection;
import org.schwering.irc.lib.IRCEventListener;
import org.schwering.irc.lib.IRCModeParser;
import org.schwering.irc.lib.IRCUser;
import org.wildfly.camel.test.common.utils.TestUtils;
import org.wildfly.extension.camel.CamelAware;

@CamelAware
@RunWith(ArquillianConditionalRunner.class)
@RequiresDocker
public class IRCIntegrationTest {

    private static final String CONTAINER_IRCD = "ircd";

    @ArquillianResource
    private CubeController cubeController;

    @Deployment
    public static JavaArchive createDeployment() {
        return ShrinkWrap.create(JavaArchive.class, "camel-irc-tests")
            .addClass(TestUtils.class);
    }

    @Before
    public void setUp() {
        cubeController.create(CONTAINER_IRCD);
        cubeController.start(CONTAINER_IRCD);
    }

    @After
    public void tearDown() {
        cubeController.stop(CONTAINER_IRCD);
        cubeController.destroy(CONTAINER_IRCD);
    }

    @Test
    public void testIRCComponent() throws Exception {
        String uri = "irc:kermit@" + TestUtils.getDockerHost() + "/#wfctest";

        CamelContext camelctx = new DefaultCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from(uri)
                .to("mock:messages");
            }
        });

        MockEndpoint endpoint = camelctx.getEndpoint("mock:messages", MockEndpoint.class);
        endpoint.expectedMessageCount(3);

        // Expect a JOIN message for each user connection, followed by the actual IRC message
        endpoint.expectedBodiesReceived("JOIN", "JOIN", "Hello Kermit!");

        CountDownLatch latch = new CountDownLatch(1);

        IrcComponent component = camelctx.getComponent("irc", IrcComponent.class);
        IrcEndpoint ircEndpoint = camelctx.getEndpoint(uri, IrcEndpoint.class);

        IRCConnection ircConnection = component.getIRCConnection(ircEndpoint.getConfiguration());
        ircConnection.addIRCEventListener(new ChannelJoinListener(latch));

        camelctx.start();
        try {
            Assert.assertTrue("Gave up waiting for user to join IRC channel", latch.await(15, TimeUnit.SECONDS));

            ProducerTemplate template = camelctx.createProducerTemplate();
            template.sendBody("irc:piggy@" + TestUtils.getDockerHost() + "/#wfctest", "Hello Kermit!");

            endpoint.assertIsSatisfied(10000);
        } finally {
            camelctx.stop();
        }
    }

    private class ChannelJoinListener implements IRCEventListener {

        private final CountDownLatch latch;

        private ChannelJoinListener(CountDownLatch latch) {
            this.latch = latch;
        }

        @Override
        public void onJoin(String chan, IRCUser user) {
            latch.countDown();
        }

        @Override
        public void onRegistered() {
        }

        @Override
        public void onDisconnected() {
        }

        @Override
        public void onError(String msg) {
        }

        @Override
        public void onError(int num, String msg) {
        }

        @Override
        public void onInvite(String chan, IRCUser user, String passiveNick) {
        }

        @Override
        public void onKick(String chan, IRCUser user, String passiveNick, String msg) {
        }

        @Override
        public void onMode(String chan, IRCUser user, IRCModeParser modeParser) {
        }

        @Override
        public void onMode(IRCUser user, String passiveNick, String mode) {
        }

        @Override
        public void onNick(IRCUser user, String newNick) {
        }

        @Override
        public void onNotice(String target, IRCUser user, String msg) {
        }

        @Override
        public void onPart(String chan, IRCUser user, String msg) {
        }

        @Override
        public void onPing(String ping) {
        }

        @Override
        public void onPrivmsg(String target, IRCUser user, String msg) {
        }

        @Override
        public void onQuit(IRCUser user, String msg) {
        }

        @Override
        public void onReply(int num, String value, String msg) {
        }

        @Override
        public void onTopic(String chan, IRCUser user, String topic) {
        }

        @Override
        public void unknown(String prefix, String command, String middle, String trailing) {
        }
    }
}
