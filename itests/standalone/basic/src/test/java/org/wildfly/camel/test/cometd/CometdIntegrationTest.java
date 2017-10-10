/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.wildfly.camel.test.cometd;

import java.util.List;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.cometd.CometdComponent;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.impl.DefaultCamelContext;
import org.cometd.bayeux.server.BayeuxServer;
import org.cometd.bayeux.server.SecurityPolicy;
import org.cometd.bayeux.server.ServerChannel;
import org.cometd.bayeux.server.ServerMessage;
import org.cometd.bayeux.server.ServerSession;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.camel.test.common.utils.AvailablePortFinder;
import org.wildfly.extension.camel.CamelAware;

@CamelAware
@RunWith(Arquillian.class)
public class CometdIntegrationTest {

    private static final String SHOOKHANDS_SESSION_HEADER = "Shookhands";
    private String uri;

    @Deployment
    public static JavaArchive createdeployment() {
        JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "camel-cometd-tests");
        archive.addClasses(AvailablePortFinder.class);
        return archive;
    }

    @Before
    public void setUp() throws Exception {
        int port = AvailablePortFinder.getNextAvailable(23500);
        uri = "cometd://127.0.0.1:" + port + "/service/test?baseResource=file:./target/test-classes/webapp&"
                + "timeout=240000&interval=0&maxInterval=30000&multiFrameInterval=1500&jsonCommented=true&sessionHeadersEnabled=true&logLevel=2";
    }

    @Test
    public void testProducer() throws Exception {
        Person person = new Person("David", "Greco");

        CamelContext camelctx = new DefaultCamelContext();
        camelctx.addRoutes(createRouteBuilder());

        camelctx.start();
        try {
            ProducerTemplate template = camelctx.createProducerTemplate();
            template.requestBodyAndHeader("direct:input", person, "testHeading", "value");

            //assert
            MockEndpoint ep = camelctx.getEndpoint("mock:test", MockEndpoint.class);
            List<Exchange> exchanges = ep.getReceivedExchanges();
            for (Exchange exchange : exchanges) {
                Message message = exchange.getIn();
                Person person1 = (Person) message.getBody();
                Assert.assertEquals("David", person1.getName());
                Assert.assertEquals("Greco", person1.getSurname());
            }
        } finally {
            camelctx.stop();
        }
    }

    private RouteBuilder createRouteBuilder() throws Exception {
        return new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                CometdComponent component = getContext().getComponent("cometd", CometdComponent.class);
                component.setSecurityPolicy(createTestSecurityPolicy());
                from("direct:input").to(uri);
                from(uri).to("mock:test");
            }
        };

    }

    private SecurityPolicy createTestSecurityPolicy() {
        return new SecurityPolicy() {

            @Override
            public boolean canSubscribe(BayeuxServer server, ServerSession session, ServerChannel channel, ServerMessage message) {
                session.setAttribute("Subscribed", true);
                return true;
            }

            @Override
            public boolean canPublish(BayeuxServer server, ServerSession session, ServerChannel channel, ServerMessage message) {
                return true;
            }

            @Override
            public boolean canHandshake(BayeuxServer server, ServerSession session, ServerMessage message) {
                session.setAttribute(SHOOKHANDS_SESSION_HEADER, true);
                return true;
            }

            @Override
            public boolean canCreate(BayeuxServer server, ServerSession session, String channelId, ServerMessage message) {
                return true;
            }
        };
    }

    static class Person {

        private String name;
        private String surname;

        Person(String name, String surname) {
            this.name = name;
            this.surname = surname;
        }

        public String getName() {
            return name;
        }

        public String getSurname() {
            return surname;
        }

        public void setName(String name) {
            this.name = name;
        }

        public void setSurname(String surname) {
            this.surname = surname;
        }
    }
}
