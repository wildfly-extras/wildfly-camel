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
package org.wildfly.camel.test.crypto.cms;

import java.io.InputStream;

import javax.naming.InitialContext;

import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.crypto.cms.crypt.DefaultKeyTransRecipientInfo;
import org.apache.camel.component.crypto.cms.sig.DefaultSignerInfo;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.util.jsse.KeyStoreParameters;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assume;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.camel.test.common.utils.EnvironmentUtils;
import org.wildfly.extension.camel.CamelAware;

@CamelAware
@RunWith(Arquillian.class)
public class CryptoCmsIntegrationTest {

    @ArquillianResource
    private InitialContext context;

    @Deployment
    public static JavaArchive createDeployment() {
        return ShrinkWrap.create(JavaArchive.class, "camel-crypto-cms-tests.jar")
            .addClass(EnvironmentUtils.class)
            .addAsResource("crypto/cms/crypto.keystore", "crypto.keystore")
            .addAsResource("crypto/cms/signed.bin", "signed.bin");
    }

    @Test
    public void testCryptoCmsSignEncryptDecryptVerify() throws Exception {
        Assume.assumeFalse("[#2241] CryptoCmsIntegrationTest fails on IBM JDK", EnvironmentUtils.isIbmJDK());

        CamelContext camelctx = new DefaultCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:start")
                .to("crypto-cms:sign://testsign?signer=#signer1&signer=#signer2&includeContent=true")
                .to("crypto-cms:encrypt://testencrpyt?toBase64=true&recipient=#recipient1&contentEncryptionAlgorithm=DESede/CBC/PKCS5Padding&secretKeyLength=128")
                .to("crypto-cms:decrypt://testdecrypt?fromBase64=true&keyStoreParameters=#keyStoreParameters")
                .to("crypto-cms:verify://testverify?keyStoreParameters=#keyStoreParameters")
                .to("mock:result");
            }
        });

        KeyStoreParameters keyStoreParameters = new KeyStoreParameters();
        keyStoreParameters.setType("JCEKS");
        keyStoreParameters.setResource("/crypto.keystore");
        keyStoreParameters.setPassword("Abcd1234");

        DefaultKeyTransRecipientInfo recipient = new DefaultKeyTransRecipientInfo();
        recipient.setCertificateAlias("rsa");
        recipient.setKeyStoreParameters(keyStoreParameters);

        DefaultSignerInfo signerInfo = new DefaultSignerInfo();
        signerInfo.setIncludeCertificates(true);
        signerInfo.setSignatureAlgorithm("SHA256withRSA");
        signerInfo.setPrivateKeyAlias("rsa");
        signerInfo.setKeyStoreParameters(keyStoreParameters);

        DefaultSignerInfo signerInfo2 = new DefaultSignerInfo();
        signerInfo2.setSignatureAlgorithm("SHA256withDSA");
        signerInfo2.setPrivateKeyAlias("dsa");
        signerInfo2.setKeyStoreParameters(keyStoreParameters);

        MockEndpoint mockEndpoint = camelctx.getEndpoint("mock:result", MockEndpoint.class);
        mockEndpoint.expectedBodiesReceived("Testmessage");

        context.bind("keyStoreParameters", keyStoreParameters);
        context.bind("signer1", signerInfo);
        context.bind("signer2", signerInfo2);
        context.bind("recipient1", recipient);

        camelctx.start();
        try {
            ProducerTemplate template = camelctx.createProducerTemplate();
            template.sendBody("direct:start", "Testmessage");

            mockEndpoint.assertIsSatisfied();
        } finally {
            camelctx.stop();
            context.unbind("keyStoreParameters");
            context.unbind("signer1");
            context.unbind("signer2");
            context.unbind("recipient1");
        }
    }

    @Test
    public void testCryptoCmsDecryptVerifyBinary() throws Exception {
        Assume.assumeFalse("[#2241] CryptoCmsIntegrationTest fails on IBM JDK", EnvironmentUtils.isIbmJDK());

        CamelContext camelctx = new DefaultCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:start")
                .to("crypto-cms:decrypt://testdecrypt?fromBase64=true&keyStoreParameters=#keyStoreParameters")
                .to("crypto-cms:verify://testverify?keyStoreParameters=#keyStoreParameters")
                .to("mock:result");
            }
        });

        KeyStoreParameters keyStoreParameters = new KeyStoreParameters();
        keyStoreParameters.setType("JCEKS");
        keyStoreParameters.setResource("/crypto.keystore");
        keyStoreParameters.setPassword("Abcd1234");
        context.bind("keyStoreParameters", keyStoreParameters);

        MockEndpoint mockEndpoint = camelctx.getEndpoint("mock:result", MockEndpoint.class);
        mockEndpoint.expectedBodiesReceived("Testmessage");

        camelctx.start();
        try {
            InputStream input = CryptoCmsIntegrationTest.class.getResourceAsStream("/signed.bin");

            ProducerTemplate template = camelctx.createProducerTemplate();
            template.sendBody("direct:start", input);

            mockEndpoint.assertIsSatisfied();
        } finally {
            camelctx.stop();
            context.unbind("keyStoreParameters");
        }
    }
}
