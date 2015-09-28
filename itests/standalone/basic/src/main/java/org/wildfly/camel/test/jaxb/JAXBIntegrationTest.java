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

import java.io.InputStream;

import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.model.dataformat.JaxbDataFormat;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.gravia.resource.ManifestBuilder;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.camel.test.common.XMLUtils;
import org.wildfly.camel.test.jaxb.model.Customer;
import org.wildfly.extension.camel.CamelAware;

@CamelAware
@RunWith(Arquillian.class)
public class JAXBIntegrationTest {

    @Deployment
    public static WebArchive deployment() {
        final WebArchive archive = ShrinkWrap.create(WebArchive.class, "jaxb-integration-tests.war");
        archive.addPackage(Customer.class.getPackage());
        archive.addClasses(XMLUtils.class);
        archive.addAsResource(new StringAsset("Customer"), "org/wildfly/camel/test/jaxb/model/jaxb.index");
        archive.addAsResource("jaxb/customer.xml", "customer.xml");
        archive.setManifest(new Asset() {
            @Override
            public InputStream openStream() {
                ManifestBuilder builder = new ManifestBuilder();
                builder.addManifestHeader("Dependencies", "org.jdom");
                return builder.openStream();
            }
        });
        return archive;
    }

    @Test
    public void testJaxbMarshal() throws Exception {

        final JaxbDataFormat format = new JaxbDataFormat();
        format.setContextPath("org.wildfly.camel.test.jaxb.model");

        CamelContext camelctx = new DefaultCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:start")
                .marshal(format);
            }
        });

        InputStream input = getClass().getResourceAsStream("/customer.xml");
        String expected = XMLUtils.compactXML(input);

        camelctx.start();
        try {
            ProducerTemplate producer = camelctx.createProducerTemplate();
            Customer customer = new Customer("John", "Doe");
            String result = producer.requestBody("direct:start", customer, String.class);
            Assert.assertEquals(expected, XMLUtils.compactXML(result));
        } finally {
            camelctx.stop();
        }
    }

    @Test
    public void testJaxbUnmarshal() throws Exception {

        final JaxbDataFormat format = new JaxbDataFormat();
        format.setContextPath("org.wildfly.camel.test.jaxb.model");

        CamelContext camelctx = new DefaultCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:start")
                .unmarshal(format);
            }
        });

        InputStream input = getClass().getResourceAsStream("/customer.xml");

        camelctx.start();
        try {
            ProducerTemplate producer = camelctx.createProducerTemplate();
            Customer customer = producer.requestBody("direct:start", input, Customer.class);
            Assert.assertEquals("John", customer.getFirstName());
            Assert.assertEquals("Doe", customer.getLastName());
        } finally {
            camelctx.stop();
        }
    }
}
