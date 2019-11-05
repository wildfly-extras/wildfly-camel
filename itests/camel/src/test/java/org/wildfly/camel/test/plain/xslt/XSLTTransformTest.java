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

package org.wildfly.camel.test.plain.xslt;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.impl.DefaultCamelContext;
import org.junit.Assert;
import org.junit.Test;
import org.wildfly.camel.utils.IOUtils;

public class XSLTTransformTest {

    @Test
    public void testXSLTTransform() throws Exception {

        CamelContext camelctx = new DefaultCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:start").to("xslt-saxon:xslt/transform.xsl");
            }
        });

        camelctx.start();
        try {
            ProducerTemplate producer = camelctx.createProducerTemplate();
            String customer = producer.requestBody("direct:start", readCustomerXml("/xslt/customer.xml"), String.class);
            Assert.assertEquals("John Doe", customer);
        } finally {
            camelctx.close();
        }
    }

    @Test
    public void testXSLTSaxonTransform() throws Exception {

        CamelContext camelctx = new DefaultCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:start").to("xslt-saxon:xslt/transform.xsl");
            }
        });

        camelctx.start();
        try {
            ProducerTemplate producer = camelctx.createProducerTemplate();
            String customer = producer.requestBody("direct:start", readCustomerXml("/xslt/customer.xml"), String.class);
            Assert.assertEquals("John Doe", customer);
        } finally {
            camelctx.close();
        }
    }

    @Test
    public void testXSLTDefaultTransformer() throws Exception {
        TransformerFactory factory = TransformerFactory.newInstance();
        Transformer transformer = factory.newTransformer(new StreamSource(getClass().getResourceAsStream("/xslt/transform.xsl")));
        StreamSource source = new StreamSource(getClass().getResourceAsStream("/xslt/customer.xml"));
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        transformer.transform(source, new StreamResult(baos));
        Assert.assertEquals("John Doe", new String(baos.toByteArray()));
    }

    @Test
    public void testXSLTSaxonTransformer() throws Exception {
        TransformerFactory factory = TransformerFactory.newInstance("net.sf.saxon.TransformerFactoryImpl", getClass().getClassLoader());
        Transformer transformer = factory.newTransformer(new StreamSource(getClass().getResourceAsStream("/xslt/transform.xsl")));
        StreamSource source = new StreamSource(getClass().getResourceAsStream("/xslt/customer.xml"));
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        transformer.transform(source, new StreamResult(baos));
        Assert.assertEquals("John Doe", new String(baos.toByteArray()));
    }

    private String readCustomerXml(String xmlpath) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        IOUtils.copyStream(getClass().getResourceAsStream(xmlpath), out);
        return new String(out.toByteArray());
    }
}
