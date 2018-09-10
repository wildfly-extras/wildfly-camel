/*
 * #%L
 * Wildfly Camel :: Testsuite
 * %%
 * Copyright (C) 2013 - 2017 RedHat
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
package org.wildfly.camel.test.soap;

import java.io.InputStream;

import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.dataformat.soap.SoapJaxbDataFormat;
import org.apache.camel.dataformat.soap.name.ServiceInterfaceStrategy;
import org.apache.camel.impl.DefaultCamelContext;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.camel.test.common.types.Customer;
import org.wildfly.camel.test.common.utils.ManifestBuilder;
import org.wildfly.camel.test.common.utils.XMLUtils;
import org.wildfly.camel.test.soap.subA.CustomerService;
import org.wildfly.extension.camel.CamelAware;

@CamelAware
@RunWith(Arquillian.class)
public class SOAPServiceInterfaceStrategyIntegrationTest {

    @Deployment
    public static WebArchive createDeployment() {
        return ShrinkWrap.create(WebArchive.class, "camel-soap-sis-tests.war")
            .addClasses(XMLUtils.class, Customer.class, CustomerService.class)
            .addAsResource(new StringAsset("Customer"), "org/wildfly/camel/test/common/types/jaxb.index")
            .addAsResource("soap/envelope-sis.xml", "envelope.xml")
            .setManifest(() -> {
                ManifestBuilder builder = new ManifestBuilder();
                builder.addManifestHeader("Dependencies", "org.jdom");
                return builder.openStream();
            });
    }

    @Test
    public void testSOAPServiceInterfaceStrategyMarshal() throws Exception {
        ServiceInterfaceStrategy strategy = new ServiceInterfaceStrategy(CustomerService.class, true);
        final SoapJaxbDataFormat format = new SoapJaxbDataFormat("org.wildfly.camel.test.common.types", strategy);

        CamelContext camelctx = new DefaultCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:start")
                .marshal(format);
            }
        });

        camelctx.start();
        try (InputStream input = getClass().getResourceAsStream("/envelope.xml")) {
            Customer customer = new Customer();
            customer.setFirstName("Kermit");
            customer.setLastName("The Frog");

            ProducerTemplate template = camelctx.createProducerTemplate();
            String result = template.requestBody("direct:start", customer, String.class);
            Assert.assertEquals(XMLUtils.compactXML(input), XMLUtils.compactXML(result));
        } finally {
            camelctx.stop();
        }
    }
}
