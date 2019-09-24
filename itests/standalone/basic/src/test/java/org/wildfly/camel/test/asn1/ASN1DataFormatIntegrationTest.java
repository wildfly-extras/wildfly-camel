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
package org.wildfly.camel.test.asn1;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.dataformat.asn1.ASN1DataFormat;
import org.apache.camel.impl.DefaultCamelContext;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.camel.utils.IOUtils;
import org.wildfly.extension.camel.CamelAware;

@CamelAware
@RunWith(Arquillian.class)
public class ASN1DataFormatIntegrationTest {

    @Deployment
    public static JavaArchive createDeployment() {
        JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "camel-asn1-tests.jar");
        archive.addAsResource("asn1/SMS_SINGLE.tt");
        archive.addClasses(IOUtils.class);
        return archive;
    }

    @Test
    public void testUnmarshalMarshal() throws Exception {

        CamelContext camelctx = new DefaultCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            public void configure() throws Exception {
                ASN1DataFormat asn1 = new ASN1DataFormat();
                from("direct:start").unmarshal(asn1).marshal(asn1).to("mock:marshal");
            }
        });

        MockEndpoint mockMarshal = camelctx.getEndpoint("mock:marshal", MockEndpoint.class);
        mockMarshal.expectedMessageCount(1);

        camelctx.start();
        try {
            byte[] byteArray = getByteArrayPayload("asn1/SMS_SINGLE.tt");

            ProducerTemplate template = camelctx.createProducerTemplate();
            byte[] response = (byte[]) template.requestBody("direct:start", new ByteArrayInputStream(byteArray));

            Assert.assertTrue(Arrays.equals(byteArray, response));
            mockMarshal.assertIsSatisfied();
        } finally {
            camelctx.close();
        }
    }

    public byte[] getByteArrayPayload(String resName) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (InputStream ins = getClass().getClassLoader().getResourceAsStream(resName)) {
            IOUtils.copyStream(ins, baos);
        }
        return baos.toByteArray();
    }
}
