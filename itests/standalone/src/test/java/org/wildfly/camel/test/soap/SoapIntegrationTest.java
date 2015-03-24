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

package org.wildfly.camel.test.soap;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.dataformat.soap.SoapJaxbDataFormat;
import org.apache.camel.impl.DefaultCamelContext;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.gravia.utils.IOUtils;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.w3c.dom.Element;
import org.wildfly.camel.test.jaxb.model.Customer;

@RunWith(Arquillian.class)
public class SoapIntegrationTest {

    @Deployment
    public static JavaArchive deployment() {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "soap-dataformat-tests");
        archive.addPackage(Customer.class.getPackage());
        archive.addAsResource(new StringAsset("Customer"), "org/wildfly/camel/test/jaxb/model/jaxb.index");
        archive.addAsResource("soap/envelope.xml", "envelope.xml");
        return archive;
    }

    @Test
    public void testSoapMarshal() throws Exception {

        final SoapJaxbDataFormat format = new SoapJaxbDataFormat();
        format.setContextPath("org.wildfly.camel.test.jaxb.model");

        CamelContext camelctx = new DefaultCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:start")
                .marshal(format);
            }
        });

        camelctx.start();
        try {
            ProducerTemplate producer = camelctx.createProducerTemplate();
            Customer customer = new Customer("John", "Doe");
            String customerXML = producer.requestBody("direct:start", customer, String.class);
            Assert.assertEquals(readEnvelopeXml(), customerXML);
        } finally {
            camelctx.stop();
        }
    }

    @Test
    public void testJaxbUnmarshal() throws Exception {

        final SoapJaxbDataFormat format = new SoapJaxbDataFormat();
        format.setContextPath("org.wildfly.camel.test.jaxb.model");

        CamelContext camelctx = new DefaultCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:start")
                .unmarshal(format);
            }
        });

        camelctx.start();
        try {
            ProducerTemplate producer = camelctx.createProducerTemplate();
            Element response = producer.requestBody("direct:start", readEnvelopeXml(), Element.class);
            Assert.assertEquals("Customer", response.getLocalName());
        } finally {
            camelctx.stop();
        }
    }

	private String readEnvelopeXml() throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
    	IOUtils.copyStream(getClass().getResourceAsStream("/envelope.xml"), out);
    	return new String(out.toByteArray());
	}
}
