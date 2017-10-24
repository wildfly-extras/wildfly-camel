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

package org.wildfly.camel.test.xslt;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.builder.xml.Namespaces;
import org.apache.camel.impl.DefaultCamelContext;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.camel.test.common.utils.TestUtils;
import org.wildfly.camel.test.xslt.subA.OrderBean;
import org.wildfly.extension.camel.CamelAware;

@CamelAware
@RunWith(Arquillian.class)
public class XSLTIntegrationTest {

    @Deployment
    public static JavaArchive deployment() {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "camel-xslt-tests");
        archive.addAsResource("xslt/customer-nons.xml", "customer-nons.xml");
        archive.addAsResource("xslt/customer.xml", "customer.xml");
        archive.addAsResource("xslt/transform.xsl", "transform.xsl");
        archive.addClasses(OrderBean.class, TestUtils.class);
        return archive;
    }

    @Test
    public void testQueryTransform() throws Exception {

        CamelContext camelctx = new DefaultCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                Namespaces ns = new Namespaces("ns", "http://org/wildfly/test/jaxb/model/Customer");
                from("direct:start").transform().xquery("/ns:customer/ns:firstName", String.class, ns)
                .to("mock:result");
            }
        });

        camelctx.start();
        try {
            //System.out.println(readCustomerXml());
            ProducerTemplate producer = camelctx.createProducerTemplate();
            String customer = producer.requestBody("direct:start", readCustomerXml("/customer.xml"), String.class);
            Assert.assertEquals("John", customer);
        } finally {
            camelctx.stop();
        }
    }

    @Test
    public void testParameterBinding() throws Exception {

        CamelContext camelctx = new DefaultCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:start").bean(OrderBean.class);
            }
        });

        camelctx.start();
        try {
            ProducerTemplate producer = camelctx.createProducerTemplate();
            String customer = producer.requestBody("direct:start", readCustomerXml("/customer-nons.xml"), String.class);
            Assert.assertEquals("John Doe", customer);
        } finally {
            camelctx.stop();
        }
    }

    @Test
    public void testXSLTDefaultTransformer() throws Exception {
        TransformerFactory factory = TransformerFactory.newInstance();
        Transformer transformer = factory.newTransformer(new StreamSource(getClass().getResourceAsStream("/transform.xsl")));
        StreamSource source = new StreamSource(getClass().getResourceAsStream("/customer.xml"));
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        transformer.transform(source, new StreamResult(baos));
        Assert.assertEquals("John Doe", new String(baos.toByteArray()));
    }

    @Test
    public void testXSLTTransform() throws Exception {

        CamelContext camelctx = new DefaultCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:start").to("xslt:transform.xsl?allowStAX=false");
            }
        });

        camelctx.start();
        try {
            ProducerTemplate producer = camelctx.createProducerTemplate();
            String customer = producer.requestBody("direct:start", readCustomerXml("/customer.xml"), String.class);
            Assert.assertEquals("John Doe", customer);
        } finally {
            camelctx.stop();
        }
    }

    @Test
    public void testXSLTSaxonTransform() throws Exception {

        CamelContext camelctx = new DefaultCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:start").to("xslt:transform.xsl?saxon=true");
            }
        });

        camelctx.start();
        try {
            ProducerTemplate producer = camelctx.createProducerTemplate();
            String customer = producer.requestBody("direct:start", readCustomerXml("/customer.xml"), String.class);
            Assert.assertEquals("John Doe", customer);
        } finally {
            camelctx.stop();
        }
    }

    private String readCustomerXml(String xmlpath) throws IOException {
        return TestUtils.getResourceValue(getClass(), xmlpath);
    }
}
