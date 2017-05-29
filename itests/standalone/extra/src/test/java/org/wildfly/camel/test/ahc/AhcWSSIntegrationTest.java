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

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.PollingConsumer;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.ahc.ws.WsComponent;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.util.jsse.KeyManagersParameters;
import org.apache.camel.util.jsse.KeyStoreParameters;
import org.apache.camel.util.jsse.SSLContextParameters;
import org.apache.camel.util.jsse.TrustManagersParameters;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.camel.test.ahc.subA.WebSocketServerEndpoint;
import org.wildfly.extension.camel.CamelAware;

/*
    $ keytool -genkey -alias server -keyalg RSA -keystore application.keystore -validity 10950
    Enter keystore password: password
*/

@CamelAware
@RunWith(Arquillian.class)
public class AhcWSSIntegrationTest {

    private static final String WEBSOCKET_ENDPOINT = "localhost:8443/ahc-wss-test/echo";

    private static final String KEYSTORE = "application.keystore";
    private static final String KEYSTORE_PASSWORD="password";

    @Deployment
    public static WebArchive createdeployment() {
        WebArchive archive = ShrinkWrap.create(WebArchive.class, "ahc-wss-test.war");
        archive.addClasses(WebSocketServerEndpoint.class);
        archive.addAsResource("ahc/application.keystore", "application.keystore");
        archive.addAsWebResource("ahc/websocket.js", "websocket.js");
        archive.addAsWebResource("ahc/index.jsp", "index.jsp");
        archive.addAsWebInfResource("ahc/web.xml", "web.xml");
        return archive;
    }

    @Test
    public void testAsyncWssRoute() throws Exception {

        CamelContext camelctx = new DefaultCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:start").to("ahc-wss:" + WEBSOCKET_ENDPOINT);
                from("ahc-wss:" + WEBSOCKET_ENDPOINT).to("seda:end");
            }
        });

        WsComponent wsComponent = (WsComponent) camelctx.getComponent("ahc-wss");
        wsComponent.setSslContextParameters(defineSSLContextClientParameters());
        
        PollingConsumer consumer = camelctx.getEndpoint("seda:end").createPollingConsumer();
        consumer.start();
        
        camelctx.start();
        try {
            ProducerTemplate producer = camelctx.createProducerTemplate();
            producer.sendBody("direct:start", "Kermit");

            Exchange exchange = consumer.receive(1000);
            Assert.assertEquals("Hello Kermit", exchange.getIn().getBody(String.class));

        } finally {
            camelctx.stop();
        }
    }

    private static SSLContextParameters defineSSLContextClientParameters() {
        
        KeyStoreParameters ksp = new KeyStoreParameters();
        ksp.setResource("/" + KEYSTORE);
        ksp.setPassword(KEYSTORE_PASSWORD);

        KeyManagersParameters kmp = new KeyManagersParameters();
        kmp.setKeyPassword(KEYSTORE_PASSWORD);
        kmp.setKeyStore(ksp);

        TrustManagersParameters tmp = new TrustManagersParameters();
        tmp.setKeyStore(ksp);

        SSLContextParameters scp = new SSLContextParameters();
        scp.setKeyManagers(kmp);
        scp.setTrustManagers(tmp);

        return scp;
    }
}
