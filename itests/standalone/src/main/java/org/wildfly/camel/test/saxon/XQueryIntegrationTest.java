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

package org.wildfly.camel.test.saxon;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.builder.xml.Namespaces;
import org.apache.camel.impl.DefaultCamelContext;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.gravia.utils.IOUtils;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.camel.test.saxon.subA.OrderBean;

@RunWith(Arquillian.class)
public class XQueryIntegrationTest {

    @Deployment
    public static JavaArchive deployment() {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "xquery-tests");
        archive.addAsResource("jaxb/customer-nons.xml", "customer-nons.xml");
        archive.addAsResource("jaxb/customer.xml", "customer.xml");
        archive.addClasses(OrderBean.class);
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

    private String readCustomerXml(String xmlpath) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        IOUtils.copyStream(getClass().getResourceAsStream(xmlpath), out);
        return new String(out.toByteArray());
    }
}
