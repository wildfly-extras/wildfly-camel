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

package org.wildfly.camel.test.jaxb;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.dataformat.JaxbDataFormat;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.gravia.utils.IOUtils;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.camel.test.jaxb.model.Customer;
import org.wildfly.extension.camel.CamelContextFactory;
import org.wildfly.extension.camel.WildFlyCamelContext;

@RunWith(Arquillian.class)
public class JAXBIntegrationTest {

    @ArquillianResource
    CamelContextFactory contextFactory;

    @Deployment
    public static JavaArchive deployment() {
        final StringAsset jaxbIndex = new StringAsset("Customer");
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "jaxb-integration-tests");
        archive.addPackage(Customer.class.getPackage());
        archive.addAsResource(jaxbIndex, "org/wildfly/camel/test/jaxb/model/jaxb.index");
        archive.addAsResource("jaxb/model/customer.xml", "customer.xml");
        return archive;
    }

    @Test
    public void testJaxbUnmarshal() throws Exception {
        
        // [FIXME #287] Usage of camel-jaxb depends on TCCL
        Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
        
        WildFlyCamelContext camelctx = contextFactory.createCamelContext();
        final JaxbDataFormat jaxb = new JaxbDataFormat();
        jaxb.setContextPath("org.wildfly.camel.test.jaxb.model");

        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:start")
                .unmarshal(jaxb);
            }
        });
        camelctx.start();

        ProducerTemplate producer = camelctx.createProducerTemplate();
        Customer customer = producer.requestBody("direct:start", readCustomerXml(), Customer.class);
        Assert.assertEquals("John", customer.getFirstName());
        Assert.assertEquals("Doe", customer.getLastName());

        camelctx.stop();
    }

    @Test
    public void testJaxbMarshal() throws Exception {

        final JaxbDataFormat jaxb = new JaxbDataFormat();
        jaxb.setContextPath("org.wildfly.camel.test.jaxb.model");

        // [FIXME #287] Usage of camel-jaxb depends on TCCL
        Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
        
        WildFlyCamelContext camelctx = contextFactory.createCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:start")
                .marshal(jaxb);
            }
        });
        camelctx.start();

        ProducerTemplate producer = camelctx.createProducerTemplate();
        Customer customer = new Customer("John", "Doe");
        String customerXML = producer.requestBody("direct:start", customer, String.class);
        Assert.assertEquals(readCustomerXml(), customerXML);

        camelctx.stop();
    }

	private String readCustomerXml() throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
    	IOUtils.copyStream(getClass().getResourceAsStream("/customer.xml"), out);
    	return new String(out.toByteArray());
	}
}
