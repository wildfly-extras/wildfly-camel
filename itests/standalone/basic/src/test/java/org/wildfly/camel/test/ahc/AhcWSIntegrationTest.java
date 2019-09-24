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

package org.wildfly.camel.test.ahc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.PollingConsumer;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.impl.DefaultCamelContext;
import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClient;
import org.asynchttpclient.ws.WebSocket;
import org.asynchttpclient.ws.WebSocketListener;
import org.asynchttpclient.ws.WebSocketUpgradeHandler;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wildfly.camel.test.ahc.subA.WebSocketServerEndpoint;
import org.wildfly.extension.camel.CamelAware;

@CamelAware
@RunWith(Arquillian.class)
public class AhcWSIntegrationTest {
    private static final Logger log = LoggerFactory.getLogger(AhcWSIntegrationTest.class);

    private static final String WEBSOCKET_ENDPOINT = "localhost:8080/ahc-ws-test/echo";

    @Deployment
    public static WebArchive createdeployment() {
        WebArchive archive = ShrinkWrap.create(WebArchive.class, "ahc-ws-test.war");
        archive.addClasses(WebSocketServerEndpoint.class);
        return archive;
    }

    @Test
    public void testAsyncHttpClient() throws Exception {

        final List<String> messages = new ArrayList<>();
        final CountDownLatch latch = new CountDownLatch(1);

        WebSocketListener listener = new WebSocketListener() {
            @Override
            public void onOpen(WebSocket websocket) {
                log.info("[ws] opened");
            }

            @Override
            public void onClose(WebSocket websocket, int code, String reason) {
                log.info("[ws] closed, code " + code + " reason " + reason);
            }

            @Override
            public void onError(Throwable t) {
                log.error("[ws] error", t);
            }

            @Override
            public void onBinaryFrame(byte[] message, boolean finalFragment, int rsv) {
                log.info("[ws] received bytes --> " + Arrays.toString(message));
            }

            @Override
            public void onTextFrame(String message, boolean finalFragment, int rsv) {
                log.info("[ws] received --> " + message);
                messages.add(message);
                latch.countDown();
            }

        };

        try (AsyncHttpClient client = new DefaultAsyncHttpClient()) {
            WebSocketUpgradeHandler handler = new WebSocketUpgradeHandler.Builder().addWebSocketListener(listener).build();
            WebSocket websocket = client.prepareGet("ws://" + WEBSOCKET_ENDPOINT).execute(handler).get();
            websocket.sendTextFrame("Kermit");

            Assert.assertTrue(latch.await(1, TimeUnit.SECONDS));
            Assert.assertEquals("Hello Kermit", messages.get(0));
        }
    }

    @Test
    public void testAsyncWsRoute() throws Exception {

        CamelContext camelctx = new DefaultCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:start").to("ahc-ws:" + WEBSOCKET_ENDPOINT);
                from("ahc-ws:" + WEBSOCKET_ENDPOINT).to("seda:end");
            }
        });

        PollingConsumer consumer = camelctx.getEndpoint("seda:end").createPollingConsumer();
        consumer.start();

        camelctx.start();
        try {
            ProducerTemplate producer = camelctx.createProducerTemplate();
            producer.sendBody("direct:start", "Kermit");

            Exchange exchange = consumer.receive(1000);
            Assert.assertEquals("Hello Kermit", exchange.getIn().getBody(String.class));

        } finally {
            camelctx.close();
        }
    }
}
