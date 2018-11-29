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
package org.wildfly.camel.test.netty;

import javax.naming.InitialContext;

import org.apache.camel.CamelContext;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.support.jndi.JndiBeanRepository;
import org.apache.camel.support.jsse.KeyManagersParameters;
import org.apache.camel.support.jsse.KeyStoreParameters;
import org.apache.camel.support.jsse.SSLContextParameters;
import org.apache.camel.support.jsse.TrustManagersParameters;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(Arquillian.class)
public class SecureNettyIntegrationTest {

    private static final String KEYSTORE = "keystore.jks";
    private static final String KEYSTORE_PASSWORD="secret";
    private static final String SOCKET_HOST = "localhost";
    private static final int SOCKET_PORT = 7999;

    @ArquillianResource
    private InitialContext initialContext;

    @Before
    public void setUp() throws Exception {
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

        InitialContext context = new InitialContext();
        context.bind("sslContextParameters", scp);
    }

    @After
    public void tearDown() throws Exception {
        initialContext.unbind("sslContextParameters");
    }

    @Deployment
    public static JavaArchive createdeployment() {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "camel-netty-test.jar");
        archive.addAsManifestResource("netty/secure-netty-camel-context.xml");
        archive.addAsResource("netty/" + KEYSTORE, KEYSTORE);
        return archive;
    }

    @Test
    public void testNettySecureTcpSocket() throws Exception {
        CamelContext camelctx = new DefaultCamelContext(new JndiBeanRepository());
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:start")
                .to("netty4:tcp://" + SOCKET_HOST + ":" + SOCKET_PORT + "?textline=true&ssl=true&sslContextParameters=#sslContextParameters");
            }
        });

        camelctx.start();
        try {
            String result = camelctx.createProducerTemplate().requestBody("direct:start", "Kermit", String.class);
            Assert.assertEquals("Hello Kermit", result);
        } finally {
            camelctx.stop();
        }
    }

}
