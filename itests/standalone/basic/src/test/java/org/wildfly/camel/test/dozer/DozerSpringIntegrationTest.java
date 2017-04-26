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

import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.ServiceStatus;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.camel.test.dozer.subA.CustomerA;
import org.wildfly.camel.test.dozer.subA.CustomerB;
import org.wildfly.extension.camel.CamelAware;
import org.wildfly.extension.camel.CamelContextRegistry;

@CamelAware
@RunWith(Arquillian.class)
public class DozerSpringIntegrationTest {

    @ArquillianResource
    CamelContextRegistry contextRegistry;

    @Deployment
    public static JavaArchive createdeployment() {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "camel-dozer-spring");
        archive.addClasses(CustomerA.class, CustomerB.class);
        archive.addAsResource("dozer/dozer-camel-context.xml", "dozer-camel-context.xml");
        archive.addAsResource("dozer/dozer-mappings.xml", "dozer-mappings.xml");
        return archive;
    }

    @Test
    public void testBeanMapping() throws Exception {

        CamelContext camelctx = contextRegistry.getCamelContext("dozer-spring-context");
        Assert.assertEquals(ServiceStatus.Started, camelctx.getStatus());

        String json = "{" + CustomerA.class.getName() + ":{firstName:John,lastName:Doe,street:Street,zip:1234}}";

        ProducerTemplate template = camelctx.createProducerTemplate();
        CustomerB customer = template.requestBody("direct:start", json, CustomerB.class);
        Assert.assertEquals("John", customer.getFirstName());
        Assert.assertEquals("Doe", customer.getLastName());
        Assert.assertEquals("Street", customer.getAddress().getStreet());
        Assert.assertEquals("1234", customer.getAddress().getZip());
    }
}
