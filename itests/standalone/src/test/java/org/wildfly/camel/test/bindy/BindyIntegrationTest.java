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

package org.wildfly.camel.test.bindy;

import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.dataformat.bindy.csv.BindyCsvDataFormat;
import org.apache.camel.spi.DataFormat;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.camel.test.bindy.model.Customer;
import org.wildfly.extension.camel.CamelContextFactory;

@RunWith(Arquillian.class)
public class BindyIntegrationTest {

    @ArquillianResource
    CamelContextFactory contextFactory;
    
    @Deployment
    public static JavaArchive deployment() {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "bindy-tests");
        archive.addPackage(Customer.class.getPackage());
        return archive;
    }

    @Test
    public void testUnmarshal() throws Exception {
        
        ClassLoader classLoader = getClass().getClassLoader();
        CamelContext camelctx = contextFactory.createCamelContext(classLoader);
        
        final DataFormat bindy = new BindyCsvDataFormat(Customer.class);
        
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:start")
                .unmarshal(bindy)
                .to("mock:result");
            }
        });
        camelctx.start();

        ProducerTemplate producer = camelctx.createProducerTemplate();
        Customer customer = (Customer) producer.requestBody("direct:start", "John,Doe");
        Assert.assertEquals("John", customer.getFirstName());
        Assert.assertEquals("Doe", customer.getLastName());
    }
}
