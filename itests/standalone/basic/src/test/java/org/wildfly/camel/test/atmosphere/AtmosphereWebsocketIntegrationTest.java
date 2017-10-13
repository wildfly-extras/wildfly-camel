/*
 * #%L
 * Wildfly Camel :: Testsuite
 * %%
 * Copyright (C) 2013 - 2017 RedHat
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
package org.wildfly.camel.test.atmosphere;

import java.net.URI;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.websocket.ClientEndpoint;
import javax.websocket.ContainerProvider;
import javax.websocket.MessageHandler;
import javax.websocket.Session;
import javax.websocket.WebSocketContainer;

import org.apache.camel.CamelContext;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.camel.test.atmosphere.subA.WebSocketRouteBuilder;
import org.wildfly.extension.camel.CamelAware;
import org.wildfly.extension.camel.CamelContextRegistry;

@CamelAware
@RunWith(Arquillian.class)
public class AtmosphereWebsocketIntegrationTest {

    @ArquillianResource
    private CamelContextRegistry camelContextRegistry;

    @Deployment
    public static WebArchive createDeployment() {
        return ShrinkWrap.create(WebArchive.class, "camel-atmosphere-websocket-tests.war")
            .addAsWebInfResource("atmosphere/web.xml", "web.xml")
            .addClass(WebSocketRouteBuilder.class);
    }

    @Test
    public void testAtmosphereWebsocketComponent() throws Exception {
        CamelContext camelctx = camelContextRegistry.getCamelContext("camel-websocket-context");
        Assert.assertNotNull("Expected camel-websocker-context to not be null", camelctx);

        SimpleMessageHandler handler = new SimpleMessageHandler(new CountDownLatch(1));

        WebSocketContainer webSocketContainer = ContainerProvider.getWebSocketContainer();
        Session session = webSocketContainer.connectToServer(HelloClient.class, new URI("ws://localhost:8080/camel-atmosphere-websocket-tests/services/hello"));
        session.addMessageHandler(handler);
        session.getBasicRemote().sendText("Kermit");

        Assert.assertTrue("Gave up waiting for web socket response", handler.awaitMessage());
        Assert.assertEquals("Hello Kermit", handler.getResult());
    }

    @ClientEndpoint
    private static final class HelloClient {
    }

    private static final class SimpleMessageHandler implements MessageHandler.Whole<String> {

        private CountDownLatch latch;
        private String result;

        public SimpleMessageHandler(CountDownLatch latch) {
            this.latch = latch;
        }

        @Override
        public void onMessage(String message) {
            latch.countDown();
            result = message;
        }

        public boolean awaitMessage() throws InterruptedException {
            return latch.await(5, TimeUnit.SECONDS);
        }

        public String getResult() {
            return result;
        }
    }
}
