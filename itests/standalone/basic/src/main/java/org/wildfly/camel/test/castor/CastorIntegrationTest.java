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

package org.wildfly.camel.test.castor;

import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.impl.DefaultCamelContext;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.camel.test.common.types.Customer;
import org.wildfly.camel.test.common.utils.EnvironmentUtils;
import org.wildfly.extension.camel.CamelAware;

@CamelAware
@RunWith(Arquillian.class)
public class CastorIntegrationTest {

    private static final String CUSTOMER_XML = "<customer><last-name>Doe</last-name><first-name>John</first-name></customer>";

    @Deployment
    public static JavaArchive createdeployment() {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "castor-dataformat-tests");
        archive.addClasses(Customer.class, EnvironmentUtils.class);
        archive.addAsResource("castor/castor-mapping.xml", "castor-mapping.xml");
        return archive;
    }

    @Test
    public void testMarshal() throws Exception {

        Assume.assumeFalse("[ENTESB-6586] CastorIntegrationTest fails on AIX", EnvironmentUtils.isAIX());
        
        CamelContext camelctx = new DefaultCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:start")
                .marshal().castor("castor-mapping.xml");
            }
        });

        camelctx.start();
        try {
            ProducerTemplate producer = camelctx.createProducerTemplate();
            String result = producer.requestBody("direct:start", new Customer("John", "Doe"), String.class);
            Assert.assertTrue("Ends with: " + result, result.endsWith(CUSTOMER_XML));
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
                from("direct:start")
                .unmarshal().castor("castor-mapping.xml");
            }
        });

        camelctx.start();
        try {
            ProducerTemplate producer = camelctx.createProducerTemplate();
            Customer result = producer.requestBody("direct:start", CUSTOMER_XML, Customer.class);
            Assert.assertEquals("John", result.getFirstName());
            Assert.assertEquals("Doe", result.getLastName());
        } finally {
            camelctx.stop();
        }
    }
}
