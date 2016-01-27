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

package org.wildfly.camel.test.dozer;

import java.util.Arrays;

import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.converter.dozer.DozerBeanMapperConfiguration;
import org.apache.camel.converter.dozer.DozerTypeConverterLoader;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.model.dataformat.JsonLibrary;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.camel.test.dozer.subA.CustomerA;
import org.wildfly.camel.test.dozer.subA.CustomerB;
import org.wildfly.extension.camel.CamelAware;

@CamelAware
@RunWith(Arquillian.class)
public class DozerIntegrationTest {

    private static final String DOZER_MAPPINGS_XML = "dozer-mappings.xml";

    @Deployment
    public static JavaArchive createdeployment() {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "camel-dozer-tests");
        archive.addClasses(CustomerA.class, CustomerB.class);
        archive.addAsResource("dozer/" + DOZER_MAPPINGS_XML, DOZER_MAPPINGS_XML);
        return archive;
    }

    @Test
    public void testBeanMapping() throws Exception {

        CamelContext camelctx = new DefaultCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:start").convertBodyTo(CustomerB.class);
            }
        });

        DozerBeanMapperConfiguration mconfig = new DozerBeanMapperConfiguration();
        mconfig.setMappingFiles(Arrays.asList(new String[] { DOZER_MAPPINGS_XML }));
        new DozerTypeConverterLoader(camelctx, mconfig);

        CustomerA customerA = new CustomerA("Peter", "Post", "Street", "12345");

        camelctx.start();
        try {
            ProducerTemplate producer = camelctx.createProducerTemplate();
            CustomerB result = producer.requestBody("direct:start", customerA, CustomerB.class);
            Assert.assertEquals(customerA.getFirstName(), result.getFirstName());
            Assert.assertEquals(customerA.getLastName(), result.getLastName());
            Assert.assertEquals(customerA.getStreet(), result.getAddress().getStreet());
            Assert.assertEquals(customerA.getZip(), result.getAddress().getZip());
        } finally {
            camelctx.stop();
        }
    }

    @Test
    public void testUnmarshalJson() throws Exception {

        CamelContext camelctx = new DefaultCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:start")
                .unmarshal().json(JsonLibrary.XStream, CustomerA.class);
            }
        });

        String input = "{'" + CustomerA.class.getName() + "':{'firstName':'John','lastName':'Doe','street':'Street','zip':1234}}";

        camelctx.start();
        try {
            ProducerTemplate producer = camelctx.createProducerTemplate();
            CustomerA customer = producer.requestBody("direct:start", input, CustomerA.class);
            Assert.assertEquals("John", customer.getFirstName());
            Assert.assertEquals("Doe", customer.getLastName());
            Assert.assertEquals("Street", customer.getStreet());
            Assert.assertEquals("1234", customer.getZip());
        } finally {
            camelctx.stop();
        }
    }

    @Test
    public void testMarshalJson() throws Exception {

        CamelContext camelctx = new DefaultCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:start")
                .marshal().json(JsonLibrary.XStream);
            }
        });

        String expected = "{'" + CustomerB.class.getName() + "':{'firstName':'John','lastName':'Doe','address':{'street':'Street','zip':1234}}}";

        camelctx.start();
        try {
            ProducerTemplate producer = camelctx.createProducerTemplate();
            String result = producer.requestBody("direct:start", new CustomerB("John", "Doe", new CustomerB.Address("Street", "1234")), String.class);
            Assert.assertEquals(expected.replace('\'', '"'), result);
        } finally {
            camelctx.stop();
        }
    }

}
