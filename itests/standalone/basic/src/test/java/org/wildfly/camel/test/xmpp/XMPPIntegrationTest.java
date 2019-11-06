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
package org.wildfly.camel.test.xmpp;

import java.net.InetAddress;
import java.security.KeyStore;
import java.security.SecureRandom;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.impl.DefaultCamelContext;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.tcp.XMPPTCPConnectionConfiguration;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.jxmpp.jid.impl.JidCreate;
import org.wildfly.camel.test.common.utils.AvailablePortFinder;
import org.wildfly.camel.test.xmpp.subA.EmbeddedXMPPServer;
import org.wildfly.extension.camel.CamelAware;

@CamelAware
@RunWith(Arquillian.class)
@ServerSetup({XMPPIntegrationTest.ServerSetup.class})
@Ignore("[#2896] SSLHandshakeException with XMPP")
public class XMPPIntegrationTest {

    static class ServerSetup implements ServerSetupTask {

        private EmbeddedXMPPServer server;

        @Override
        public void setup(ManagementClient managementClient, String containerId) throws Exception {
            int port = AvailablePortFinder.getNextAvailable();
            AvailablePortFinder.storeServerData("xmpp-port", port);

            server = new EmbeddedXMPPServer(port);
            server.start();
        }

        @Override
        public void tearDown(ManagementClient managementClient, String containerId) throws Exception {
            if (server != null) {
                server.stop();
            }
        }
    }

    @ArquillianResource
    private InitialContext context;

    @Deployment
    public static JavaArchive createDeployment() {
        return ShrinkWrap.create(JavaArchive.class, "camel-xmpp-tests.jar")
            .addClasses(EmbeddedXMPPServer.class, AvailablePortFinder.class)
            .addAsResource("xmpp/server.jks", "server.jks");
    }

    @Before
    public void setUp() throws Exception {
        KeyStore keyStore = KeyStore.getInstance("JKS");
        keyStore.load(XMPPIntegrationTest.class.getResourceAsStream("/server.jks"), "secret".toCharArray());

        TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        trustManagerFactory.init(keyStore);

        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, trustManagerFactory.getTrustManagers(), new SecureRandom());

        String port = AvailablePortFinder.readServerData("xmpp-port");

        ConnectionConfiguration connectionConfig = XMPPTCPConnectionConfiguration.builder()
            .setXmppDomain(JidCreate.domainBareFrom("apache.camel"))
            .setHostAddress(InetAddress.getLocalHost())
            .setPort(Integer.parseInt(port))
            .setCustomSSLContext(sslContext)
            .setHostnameVerifier((hostname, session) -> true)
            .build();

        context.bind("customConnectionConfig", connectionConfig);
    }

    @After
    public void tearDown() {
        try {
            context.unbind("customConnectionConfig");
        } catch (NamingException e) {
            // Ignore
        }
    }

    @Test
    public void testXMPPComponent() throws Exception {
        String port = AvailablePortFinder.readServerData("xmpp-port");

        String consumerURI = "xmpp://localhost:%s/consumer@wildfly.camel?connectionConfig=#customConnectionConfig&room=camel-test-consumer@conference.apache.camel&user=consumer&password=secret&serviceName=apache.camel";

        CamelContext camelctx = new DefaultCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                fromF(consumerURI, port)
                .to("mock:result");
            }
        });

        String body = "Hello Kermit";

        MockEndpoint consumerResult = camelctx.getEndpoint("mock:result", MockEndpoint.class);
        consumerResult.expectedBodiesReceived(body);

        camelctx.start();
        try {
            ProducerTemplate template = camelctx.createProducerTemplate();
            template.sendBody(consumerURI, body);

            consumerResult.assertIsSatisfied();
        } finally {
            camelctx.close();
        }
    }
}
