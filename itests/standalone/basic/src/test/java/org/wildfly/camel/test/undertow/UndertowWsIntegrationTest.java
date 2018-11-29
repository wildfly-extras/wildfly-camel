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
package org.wildfly.camel.test.undertow;

import java.io.InputStream;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.Processor;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.component.undertow.UndertowConstants;
import org.apache.camel.component.undertow.UndertowConstants.EventType;
import org.apache.camel.converter.IOConverter;
import org.apache.camel.impl.DefaultCamelContext;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.camel.test.common.http.HttpRequest;
import org.wildfly.extension.camel.CamelAware;

/**
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 */
@RunWith(Arquillian.class)
@CamelAware
public class UndertowWsIntegrationTest {

    private static Logger log = Logger.getLogger(UndertowWsIntegrationTest.class);

    @Deployment
    public static WebArchive createDeployment() {
        return ShrinkWrap.create(WebArchive.class, "UndertowWsIntegrationTest.war").addClasses(HttpRequest.class,
                TestClient.class);
    }

    private static final String BROADCAST_MESSAGE_PREFIX = "broadcast ";

    private int getPort() {
        return 8080;
    }

    @Test
    public void wsClientSingleText() throws Exception {
        CamelContext camelctx = new DefaultCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                final int port = getPort();
                from("undertow:ws://localhost:" + port + "/app1")
                        .log(">>> Message received from WebSocket Client : ${body}").to("mock:result1");
            }

        });

        camelctx.start();
        try {
            TestClient websocket = new TestClient("ws://localhost:" + getPort() + "/app1").connect();

            MockEndpoint result = camelctx.getEndpoint("mock:result1", MockEndpoint.class);
            result.expectedBodiesReceived("Test");

            websocket.sendTextMessage("Test");

            result.await(60, TimeUnit.SECONDS);
            result.assertIsSatisfied();

            websocket.close();
        } finally {
            camelctx.stop();
        }

    }

    @Test
    public void wsClientSingleTextStreaming() throws Exception {
        CamelContext camelctx = new DefaultCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                final int port = getPort();
                from("undertow:ws://localhost:" + port + "/app2?useStreaming=true").to("mock:result2");
            }
        });

        camelctx.start();
        try {
            TestClient websocket = new TestClient("ws://localhost:" + getPort() + "/app2").connect();

            MockEndpoint result = camelctx.getEndpoint("mock:result2", MockEndpoint.class);
            result.expectedMessageCount(1);

            websocket.sendTextMessage("Test");

            result.await(60, TimeUnit.SECONDS);
            List<Exchange> exchanges = result.getReceivedExchanges();
            Assert.assertEquals(1, exchanges.size());
            Object body = result.getReceivedExchanges().get(0).getIn().getBody();
            Assert.assertTrue("body is " + body.getClass().getName(), body instanceof Reader);
            Reader r = (Reader) body;
            Assert.assertEquals("Test", IOConverter.toString(r));

            websocket.close();
        } finally {
            camelctx.stop();
        }
    }

    @Test
    public void wsClientSingleBytes() throws Exception {
        CamelContext camelctx = new DefaultCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                final int port = getPort();
                from("undertow:ws://localhost/app1").log(">>> Message received from WebSocket Client : ${body}")
                        .to("mock:result1");
            }
        });

        camelctx.start();
        try {
            TestClient websocket = new TestClient("ws://localhost:" + getPort() + "/app1").connect();

            MockEndpoint result = camelctx.getEndpoint("mock:result1", MockEndpoint.class);
            final byte[] testmessage = "Test".getBytes("utf-8");
            result.expectedBodiesReceived(testmessage);

            websocket.sendBytesMessage(testmessage);

            result.assertIsSatisfied();

            websocket.close();
        } finally {
            camelctx.stop();
        }
    }

    @Test
    public void wsClientSingleBytesStreaming() throws Exception {
        CamelContext camelctx = new DefaultCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                final int port = getPort();
                from("undertow:ws://localhost:" + port + "/app2?useStreaming=true").to("mock:result2");
            }
        });

        camelctx.start();
        try {
            TestClient websocket = new TestClient("ws://localhost:" + getPort() + "/app2").connect();

            MockEndpoint result = camelctx.getEndpoint("mock:result2", MockEndpoint.class);
            result.expectedMessageCount(1);

            final byte[] testmessage = "Test".getBytes("utf-8");
            websocket.sendBytesMessage(testmessage);

            result.await(60, TimeUnit.SECONDS);
            List<Exchange> exchanges = result.getReceivedExchanges();
            Assert.assertEquals(1, exchanges.size());
            Object body = result.getReceivedExchanges().get(0).getIn().getBody();
            Assert.assertTrue("body is " + body.getClass().getName(), body instanceof InputStream);
            InputStream in = (InputStream) body;
            Assert.assertArrayEquals(testmessage, IOConverter.toBytes(in));

            websocket.close();
        } finally {
            camelctx.stop();
        }
    }

    @Test
    public void wsClientMultipleText() throws Exception {
        CamelContext camelctx = new DefaultCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                final int port = getPort();
                from("undertow:ws://localhost:" + port + "/app1")
                        .log(">>> Message received from WebSocket Client : ${body}").to("mock:result1");
            }

        });

        camelctx.start();
        try {
            TestClient websocket1 = new TestClient("ws://localhost:" + getPort() + "/app1").connect();
            TestClient websocket2 = new TestClient("ws://localhost:" + getPort() + "/app1").connect();

            MockEndpoint result = camelctx.getEndpoint("mock:result1", MockEndpoint.class);
            result.expectedMessageCount(2);

            websocket1.sendTextMessage("Test1");
            websocket2.sendTextMessage("Test2");

            result.await(60, TimeUnit.SECONDS);
            result.assertIsSatisfied();
            List<Exchange> exchanges = result.getReceivedExchanges();
            Set<String> actual = new HashSet<>();
            actual.add(exchanges.get(0).getIn().getBody(String.class));
            actual.add(exchanges.get(1).getIn().getBody(String.class));
            Assert.assertEquals(new HashSet<String>(Arrays.asList("Test1", "Test2")), actual);

            websocket1.close();
            websocket2.close();
        } finally {
            camelctx.stop();
        }
    }

    @Test
    public void producer() throws Exception {
        CamelContext camelctx = new DefaultCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:shop").log(">>> Message received from Shopping center : ${body}")
                        .to("undertow:ws://localhost:" + getPort() + "/shop");
            }

        });

        camelctx.start();
        try {

            TestClient websocket = new TestClient("ws://localhost:" + getPort() + "/shop", 1).connect();

            // Send message to the direct endpoint
            ProducerTemplate producer = camelctx.createProducerTemplate();
            producer.sendBodyAndHeader("direct:shop", "Beer on stock at Apache Mall", UndertowConstants.SEND_TO_ALL,
                    "true");

            Assert.assertTrue(websocket.await(10));

            List<Object> received = websocket.getReceived();
            Assert.assertEquals(1, received .size());
            Object r = received.get(0);
            Assert.assertTrue(r instanceof String);
            Assert.assertEquals("Beer on stock at Apache Mall", r);

            websocket.close();
        } finally {
            camelctx.stop();
        }

    }

    @Test
    public void echo() throws Exception {
        CamelContext camelctx = new DefaultCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                final int port = getPort();
                from("undertow:ws://localhost:" + port + "/app3").to("undertow:ws://localhost:" + port + "/app3");
            }

        });

        camelctx.start();
        try {
            TestClient wsclient1 = new TestClient("ws://localhost:" + getPort() + "/app3", 2);
            wsclient1.connect();

            wsclient1.sendTextMessage("Test1");
            wsclient1.sendTextMessage("Test2");

            Assert.assertTrue(wsclient1.await(60));

            Assert.assertEquals(Arrays.asList("Test1", "Test2"), wsclient1.getReceived(String.class));

            wsclient1.close();
        } finally {
            camelctx.stop();
        }
    }

    @Test
    public void echoMulti() throws Exception {
        CamelContext camelctx = new DefaultCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                final int port = getPort();
                from("undertow:ws://localhost:" + port + "/app3").to("undertow:ws://localhost:" + port + "/app3");
            }

        });

        camelctx.start();
        try {
            TestClient wsclient1 = new TestClient("ws://localhost:" + getPort() + "/app3", 1);
            TestClient wsclient2 = new TestClient("ws://localhost:" + getPort() + "/app3", 1);
            wsclient1.connect();
            wsclient2.connect();

            wsclient1.sendTextMessage("Gambas");
            wsclient2.sendTextMessage("Calamares");

            Assert.assertTrue(wsclient1.await(10));
            Assert.assertTrue(wsclient2.await(10));

            Assert.assertEquals(Arrays.asList("Gambas"), wsclient1.getReceived(String.class));
            Assert.assertEquals(Arrays.asList("Calamares"), wsclient2.getReceived(String.class));

            wsclient1.close();
            wsclient2.close();
        } finally {
            camelctx.stop();
        }
    }

    @Test
    public void sendToAll() throws Exception {
        CamelContext camelctx = new DefaultCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                final int port = getPort();
                /* sendToAll */
                from("undertow:ws://localhost:" + port + "/app4") //
                        .to("undertow:ws://localhost:" + port + "/app4?sendToAll=true");
            }

        });

        camelctx.start();
        try {
            TestClient wsclient1 = new TestClient("ws://localhost:" + getPort() + "/app4", 2);
            TestClient wsclient2 = new TestClient("ws://localhost:" + getPort() + "/app4", 2);
            wsclient1.connect();
            wsclient2.connect();

            wsclient1.sendTextMessage("Gambas");
            wsclient2.sendTextMessage("Calamares");

            Assert.assertTrue(wsclient1.await(10));
            Assert.assertTrue(wsclient2.await(10));

            List<String> received1 = wsclient1.getReceived(String.class);
            Assert.assertEquals(2, received1.size());

            Assert.assertTrue(received1.contains("Gambas"));
            Assert.assertTrue(received1.contains("Calamares"));

            List<String> received2 = wsclient2.getReceived(String.class);
            Assert.assertEquals(2, received2.size());
            Assert.assertTrue(received2.contains("Gambas"));
            Assert.assertTrue(received2.contains("Calamares"));

            wsclient1.close();
            wsclient2.close();
        } finally {
            camelctx.stop();
        }
    }

    @Test
    public void fireWebSocketChannelEvents() throws Exception {
        CamelContext camelctx = new DefaultCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                final int port = getPort();
                /* sendToAll */
                from("undertow:ws://localhost:" + port + "/app5?fireWebSocketChannelEvents=true") //
                        .to("mock:result5") //
                        .to("undertow:ws://localhost:" + port + "/app5");
            }

        });

        camelctx.start();
        try {

            MockEndpoint result = camelctx.getEndpoint("mock:result5", MockEndpoint.class);
            result.expectedMessageCount(6);

            TestClient wsclient1 = new TestClient("ws://localhost:" + getPort() + "/app5", 2);
            TestClient wsclient2 = new TestClient("ws://localhost:" + getPort() + "/app5", 2);
            wsclient1.connect();
            wsclient2.connect();

            wsclient1.sendTextMessage("Gambas");
            wsclient2.sendTextMessage("Calamares");

            wsclient1.close();
            wsclient2.close();

            result.await(60, TimeUnit.SECONDS);

            final List<Exchange> exchanges = result.getReceivedExchanges();
            final Map<String, List<String>> connections = new HashMap<>();
            for (Exchange exchange : exchanges) {
                final Message in = exchange.getIn();
                final String key = (String) in.getHeader(UndertowConstants.CONNECTION_KEY);
                Assert.assertNotNull(key);
                List<String> messages = connections.get(key);
                if (messages == null) {
                    messages = new ArrayList<String>();
                    connections.put(key, messages);
                }
                String body = in.getBody(String.class);
                if (body != null) {
                    messages.add(body);
                } else {
                    messages.add(in.getHeader(UndertowConstants.EVENT_TYPE_ENUM, EventType.class).name());
                }
            }

            final List<String> expected1 = Arrays.asList(EventType.ONOPEN.name(), "Gambas", EventType.ONCLOSE.name());
            final List<String> expected2 = Arrays.asList(EventType.ONOPEN.name(), "Calamares",
                    EventType.ONCLOSE.name());

            Assert.assertEquals(2, connections.size());
            final Iterator<List<String>> it = connections.values().iterator();
            final List<String> actual1 = it.next();
            Assert.assertTrue("actual " + actual1, actual1.equals(expected1) || actual1.equals(expected2));
            final List<String> actual2 = it.next();
            Assert.assertTrue("actual " + actual2, actual2.equals(expected1) || actual2.equals(expected2));
        } finally {
            camelctx.stop();
        }

    }

    @Test
    public void connectionKeyList() throws Exception {
        CamelContext camelctx = new DefaultCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                final int port = getPort();

                /* fireWebSocketChannelEvents */
                from("undertow:ws://localhost:" + port + "/app6?fireWebSocketChannelEvents=true") //
                        .process(new Processor() {
                            private final Set<String> connectionKeys = new LinkedHashSet<>();

                            public void process(final Exchange exchange) throws Exception {
                                final Message in = exchange.getIn();
                                final String connectionKey = in.getHeader(UndertowConstants.CONNECTION_KEY,
                                        String.class);
                                final EventType eventType = in.getHeader(UndertowConstants.EVENT_TYPE_ENUM,
                                        EventType.class);
                                final String body = in.getBody(String.class);
                                if (eventType == EventType.ONOPEN) {
                                    connectionKeys.add(connectionKey);
                                    in.setBody("connected " + connectionKey);
                                } else if (eventType == EventType.ONCLOSE) {
                                    connectionKeys.remove(connectionKey);
                                } else if (body != null) {
                                    if (body.startsWith(BROADCAST_MESSAGE_PREFIX)) {
                                        List<String> keys = Arrays
                                                .asList(body.substring(BROADCAST_MESSAGE_PREFIX.length()).split(" "));
                                        in.setHeader(UndertowConstants.CONNECTION_KEY_LIST, keys);
                                    }
                                }
                            }
                        })//
                        .to("undertow:ws://localhost:" + port + "/app6");
            }

        });

        camelctx.start();
        try {

            TestClient wsclient1 = new TestClient("ws://localhost:" + getPort() + "/app6", 1);
            TestClient wsclient2 = new TestClient("ws://localhost:" + getPort() + "/app6", 1);
            TestClient wsclient3 = new TestClient("ws://localhost:" + getPort() + "/app6", 1);
            wsclient1.connect();
            wsclient2.connect();
            wsclient3.connect();

            wsclient1.await(10);
            final String connectionKey1 = assertConnected(wsclient1);
            Assert.assertNotNull(connectionKey1);
            wsclient2.await(10);
            final String connectionKey2 = assertConnected(wsclient2);
            wsclient3.await(10);
            final String connectionKey3 = assertConnected(wsclient3);

            wsclient1.reset(1);
            wsclient2.reset(1);
            wsclient3.reset(1);
            final String broadcastMsg = BROADCAST_MESSAGE_PREFIX + connectionKey2 + " " + connectionKey3;
            wsclient1.sendTextMessage(broadcastMsg); // this one should go to wsclient2 and wsclient3
            wsclient1.sendTextMessage("private"); // this one should go to wsclient1 only

            wsclient2.await(10);
            Assert.assertEquals(broadcastMsg, wsclient2.getReceived(String.class).get(0));
            wsclient3.await(10);
            Assert.assertEquals(broadcastMsg, wsclient3.getReceived(String.class).get(0));
            wsclient1.await(10);
            Assert.assertEquals("private", wsclient1.getReceived(String.class).get(0));

            wsclient1.close();
            wsclient2.close();
            wsclient3.close();
        } finally {
            camelctx.stop();
        }

    }

    private String assertConnected(TestClient wsclient1) {
        final String msg0 = wsclient1.getReceived(String.class).get(0);
        Assert.assertTrue("'" + msg0 + "' should start with 'connected '", msg0.startsWith("connected "));
        return msg0.substring("connected ".length());
    }

}
