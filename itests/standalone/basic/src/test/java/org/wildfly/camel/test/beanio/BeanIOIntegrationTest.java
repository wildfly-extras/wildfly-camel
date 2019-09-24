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

package org.wildfly.camel.test.beanio;

import java.util.List;

import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.dataformat.beanio.BeanIODataFormat;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.spi.DataFormat;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.camel.test.beanio.subA.Customer;
import org.wildfly.extension.camel.CamelAware;

@CamelAware
@RunWith(Arquillian.class)
public class BeanIOIntegrationTest {

    private static final String MAPPINGS_XML = "beanio-mappings.xml";

    @Deployment
    public static JavaArchive createdeployment() {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "camel-beanio-tests");
        archive.addClasses(Customer.class);
        archive.addAsResource("beanio/" + MAPPINGS_XML, MAPPINGS_XML);
        return archive;
    }

    @Test
    public void testMarshal() throws Exception {

        DataFormat beanio = new BeanIODataFormat(MAPPINGS_XML, "customerStream");
        CamelContext camelctx = new DefaultCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:start").marshal(beanio);
            }
        });

        camelctx.start();
        try {
            ProducerTemplate producer = camelctx.createProducerTemplate();
            Customer customer = new Customer("Peter", "Post", "Street", "12345");
            String result = producer.requestBody("direct:start", customer, String.class);
            Assert.assertEquals("Peter,Post,Street,12345", result.trim());
        } finally {
            camelctx.close();
        }
    }

    @Test
    public void testUnmarshal() throws Exception {

        DataFormat beanio = new BeanIODataFormat(MAPPINGS_XML, "customerStream");
        CamelContext camelctx = new DefaultCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:start").unmarshal(beanio);
            }
        });

        camelctx.start();
        try {
            ProducerTemplate producer = camelctx.createProducerTemplate();
            List<?> result = producer.requestBody("direct:start", "Peter,Post,Street,12345", List.class);
            Assert.assertEquals(1, result.size());
            Customer customer = (Customer) result.get(0);
            Assert.assertEquals("Peter", customer.getFirstName());
            Assert.assertEquals("Post", customer.getLastName());
            Assert.assertEquals("Street", customer.getStreet());
            Assert.assertEquals("12345", customer.getZip());
        } finally {
            camelctx.close();
        }
    }
}
