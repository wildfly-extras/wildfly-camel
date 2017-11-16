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
package org.wildfly.camel.test.directvm;

import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.ServiceStatus;
import org.apache.camel.component.mock.MockEndpoint;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.extension.camel.CamelAware;
import org.wildfly.extension.camel.CamelContextRegistry;

@CamelAware
@RunWith(Arquillian.class)
public class DirectVMSpringIntegrationTest {

    @ArquillianResource
    private CamelContextRegistry contextRegistry;

    @Deployment
    public static JavaArchive createDeployment() {
        return ShrinkWrap.create(JavaArchive.class, "camel-direct-vm-spring-tests.jar")
            .addAsResource("directvm/direct-vm-a-camel-context.xml", "direct-vm-a-camel-context.xml")
            .addAsResource("directvm/direct-vm-b-camel-context.xml", "direct-vm-b-camel-context.xml");
    }

    @Test
    public void testSpringDirectVMComponent() throws Exception {
        CamelContext camelctxA = contextRegistry.getCamelContext("direct-vm-context-a");
        Assert.assertEquals(ServiceStatus.Started, camelctxA.getStatus());

        CamelContext camelctxB = contextRegistry.getCamelContext("direct-vm-context-b");
        Assert.assertEquals(ServiceStatus.Started, camelctxB.getStatus());

        MockEndpoint mockEndpoint = camelctxB.getEndpoint("mock:result", MockEndpoint.class);
        mockEndpoint.expectedBodiesReceived("Hello Kermit");

        ProducerTemplate template = camelctxA.createProducerTemplate();
        template.sendBody("direct:start", "Kermit");

        mockEndpoint.assertIsSatisfied();
    }
}
