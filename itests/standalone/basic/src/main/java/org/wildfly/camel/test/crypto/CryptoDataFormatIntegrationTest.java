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

package org.wildfly.camel.test.crypto;

import javax.crypto.KeyGenerator;

import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.converter.crypto.CryptoDataFormat;
import org.apache.camel.converter.crypto.PGPDataFormat;
import org.apache.camel.impl.DefaultCamelContext;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(Arquillian.class)
public class CryptoDataFormatIntegrationTest {

    private static final String PUBRING_GPG = "pubring.gpg";
    private static final String SECRING_GPG = "secring.gpg";
    private static final String KEY_USERID = "sdude@nowhere.net";
    private static final String KEY_PASSWORD = "sdude";

    @Deployment
    public static JavaArchive deployment() {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "crypto-tests");
        archive.addAsResource("crypto/" + PUBRING_GPG, PUBRING_GPG);
        archive.addAsResource("crypto/" + SECRING_GPG, SECRING_GPG);
        return archive;
    }

    @Test
    public void testMarshalUnmarshallDes() throws Exception {

        final KeyGenerator generator = KeyGenerator.getInstance("DES");
        final CryptoDataFormat cryptoFormat = new CryptoDataFormat("DES", generator.generateKey());

        CamelContext camelctx = new DefaultCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:start")
                .marshal(cryptoFormat)
                .unmarshal(cryptoFormat);
            }
        });

        camelctx.start();
        try {
            ProducerTemplate producer = camelctx.createProducerTemplate();
            String result = producer.requestBody("direct:start", "password", String.class);
            Assert.assertEquals("password", result.trim());
        } finally {
            camelctx.stop();
        }
    }

    @Test
    public void testMarshallUnmarshallPgp() throws Exception {
        final PGPDataFormat encrypt = new PGPDataFormat();
        encrypt.setKeyFileName(PUBRING_GPG);
        encrypt.setKeyUserid(KEY_USERID);

        final PGPDataFormat decrypt = new PGPDataFormat();
        decrypt.setKeyFileName(SECRING_GPG);
        decrypt.setKeyUserid(KEY_USERID);
        decrypt.setPassword(KEY_PASSWORD);

        CamelContext camelctx = new DefaultCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:start")
                .marshal(encrypt)
                .unmarshal(decrypt);
            }
        });

        camelctx.start();
        try {
            ProducerTemplate producer = camelctx.createProducerTemplate();
            String result = producer.requestBody("direct:start", "password", String.class);
            Assert.assertEquals("password", result.trim());
        } finally {
            camelctx.stop();
        }
    }
}
