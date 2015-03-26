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

package org.wildfly.camel.test.converter;

import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.impl.DefaultCamelContext;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.camel.test.core.subA.Customer;

@RunWith(Arquillian.class)
public class CustomConverterTest {

    @Deployment
    public static JavaArchive deployment() {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "converter-tests");
        archive.addClasses(Customer.class, CustomConverter.class);
        archive.addAsServiceProvider("org/apache/camel/TypeConverter", CustomConverter.class.getPackage().getName());
        return archive;
    }

    @Test
    public void testMarshal() throws Exception {

        CamelContext camelctx = new DefaultCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:start").convertBodyTo(Map.class);
            }
        });

        camelctx.start();
        try {
            ProducerTemplate producer = camelctx.createProducerTemplate();
            Map<?, ?> result = producer.requestBody("direct:start", new Customer("John", "Doe"), Map.class);
            Assert.assertEquals("{firstName=John, lastName=Doe}", result.toString());
        } finally {
            camelctx.stop();
        }
    }

    @Test
    public void testUnmarshal() throws Exception {

        CamelContext camelctx = new DefaultCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:start").convertBodyTo(Customer.class);
            }
        });

        Map<String, String> input = new LinkedHashMap<String, String>();
        input.put("firstName", "John");
        input.put("lastName", "Doe");

        camelctx.start();
        try {
            ProducerTemplate producer = camelctx.createProducerTemplate();
            Customer result = producer.requestBody("direct:start", input, Customer.class);
            Assert.assertEquals("John", result.getFirstName());
            Assert.assertEquals("Doe", result.getLastName());
        } finally {
            camelctx.stop();
        }
    }
}
