/*
 * #%L
 * Wildfly Camel :: Testsuite
 * %%
 * Copyright (C) 2013 - 2015 RedHat
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

package org.wildfly.camel.test.crypto;

import java.io.InputStream;
import java.security.KeyStore;
import java.security.cert.Certificate;

import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.crypto.DigitalSignatureConstants;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.util.jsse.KeyStoreParameters;
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
public class CryptoComponentIntegrationTest {

    private static String ALIAS = "bob";
    private static String KEYSTORE = "ks.keystore";
    private static String KEYSTORE_PASSWORD="letmein";
    private static String PAYLOAD = "Dear Bob, Rest assured it's me, signed Kermit";

    @ArquillianResource
    private InitialContext initialContext;

    @Before
    public void setUp() throws Exception{
        KeyStore keystore = loadKeystore();
        Certificate cert = keystore.getCertificate(ALIAS);
        KeyStoreParameters keystoreParameters = new KeyStoreParameters();
        keystoreParameters.setPassword(KEYSTORE_PASSWORD);
        keystoreParameters.setResource("./" + KEYSTORE);

        initialContext.bind("signatureParams", keystoreParameters);
        initialContext.bind("keystore", keystore);
        initialContext.bind("myPublicKey", cert.getPublicKey());
        initialContext.bind("myCert", cert);
        initialContext.bind("myPrivateKey", keystore.getKey(ALIAS, KEYSTORE_PASSWORD.toCharArray()));
    }

    @After
    public void tearDown() throws NamingException {
        initialContext.unbind("signatureParams");
        initialContext.unbind("keystore");
        initialContext.unbind("myPublicKey");
        initialContext.unbind("myCert");
        initialContext.unbind("myPrivateKey");
    }

    @Deployment
    public static JavaArchive deployment() {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "crypto-tests");
        archive.addAsResource("crypto/" + KEYSTORE, KEYSTORE);
        return archive;
    }

    @Test
    public void testBasicSignatureRoute() throws Exception {

        CamelContext camelctx = new DefaultCamelContext();

        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:sign")
                .to("crypto:sign://basic?privateKey=#myPrivateKey")
                .to("direct:verify");

                from("direct:verify")
                .to("crypto:verify://basic?publicKey=#myPublicKey")
                .to("mock:result");
            }
        });

        camelctx.start();
        try {
            camelctx.createProducerTemplate().sendBody("direct:sign", PAYLOAD);

            MockEndpoint mockEndpoint = camelctx.getEndpoint("mock:result", MockEndpoint.class);
            Exchange e = mockEndpoint.getExchanges().get(0);
            Message result = e == null ? null : e.hasOut() ? e.getOut() : e.getIn();
            Assert.assertNull(result.getHeader(DigitalSignatureConstants.SIGNATURE));
        } finally {
            camelctx.stop();
        }
    }

    private KeyStore loadKeystore() throws Exception {
        KeyStore keystore = KeyStore.getInstance(KeyStore.getDefaultType());
        InputStream in = getClass().getResourceAsStream("/" + KEYSTORE);
        keystore.load(in, KEYSTORE_PASSWORD.toCharArray());
        return keystore;
    }
}
