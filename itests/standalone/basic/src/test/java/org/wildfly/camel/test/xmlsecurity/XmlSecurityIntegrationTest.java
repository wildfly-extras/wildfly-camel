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

package org.wildfly.camel.test.xmlsecurity;

import org.apache.camel.CamelContext;
import org.apache.camel.Message;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.xmlsecurity.api.KeyAccessor;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.support.jndi.JndiBeanRepository;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.w3c.dom.Node;
import org.wildfly.camel.test.common.utils.EnvironmentUtils;
import org.wildfly.extension.camel.CamelAware;

import javax.naming.InitialContext;
import javax.xml.crypto.KeySelector;
import javax.xml.crypto.dsig.keyinfo.KeyInfo;
import javax.xml.crypto.dsig.keyinfo.KeyInfoFactory;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.SecureRandom;

@CamelAware
@RunWith(Arquillian.class)
public class XmlSecurityIntegrationTest {

    private static String XML_PAYLOAD = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            + "<root xmlns=\"http://test/test\"><test>Hello Kermit</test></root>";

    private KeyPair keyPair;

    @ArquillianResource
    private InitialContext initialContext;

    @Deployment
    public static WebArchive createDeployment() {
        final WebArchive archive = ShrinkWrap.create(WebArchive.class, "camel-test.war");
        archive.addClasses(EnvironmentUtils.class);
        return archive;
    }

    @Before
    public void setUp() throws Exception {
        keyPair = setUpKeys();
        initialContext.bind("accessor", getKeyAccessor(keyPair.getPrivate()));
        initialContext.bind("selector", KeySelector.singletonKeySelector(keyPair.getPublic()));
    }

    @After
    public void tearDown() throws Exception {
        initialContext.unbind("accessor");
        initialContext.unbind("selector");
    }

    @Test
    public void testXmlSigning() throws Exception {

        CamelContext camelctx = new DefaultCamelContext(new JndiBeanRepository());
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:start")
                    .to("xmlsecurity-sign://enveloping?keyAccessor=#accessor&schemaResourceUri=");
            }
        });

        try {
            camelctx.start();

            ProducerTemplate producer = camelctx.createProducerTemplate();
            String signedXml = producer.requestBody("direct:start", XML_PAYLOAD, String.class);

            // Make sure the XML was signed
            Assert.assertTrue(signedXml.contains("ds:SignatureValue"));
        } finally {
            camelctx.close();
        }
    }

    @Test
    public void testXmlVerifySigning() throws Exception {

        CamelContext camelctx = new DefaultCamelContext(new JndiBeanRepository());
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:start")
                    .to("xmlsecurity-sign://enveloping?keyAccessor=#accessor&schemaResourceUri=")
                    .to("xmlsecurity-verify://enveloping?keySelector=#selector");
            }
        });

        try {
            camelctx.start();

            ProducerTemplate producer = camelctx.createProducerTemplate();
            String verifiedXml = producer.requestBody("direct:start", XML_PAYLOAD, String.class);

            // Make sure the XML was unsigned
            Assert.assertEquals(XML_PAYLOAD, verifiedXml);
        } finally {
            camelctx.close();
        }
    }

    private KeyPair setUpKeys() {
        KeyPairGenerator keyGen;
        try {
            keyGen = KeyPairGenerator.getInstance("RSA");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        keyGen.initialize(1024, new SecureRandom());
        return keyGen.generateKeyPair();
    }

    private static KeyAccessor getKeyAccessor(final PrivateKey privateKey) {
        KeyAccessor accessor = new KeyAccessor() {

            @Override
            public KeySelector getKeySelector(Message message) throws Exception {
                return KeySelector.singletonKeySelector(privateKey);
            }

            @Override
            public KeyInfo getKeyInfo(Message mess, Node messageBody, KeyInfoFactory keyInfoFactory) throws Exception {
                return null;
            }
        };
        return accessor;
    }
}
