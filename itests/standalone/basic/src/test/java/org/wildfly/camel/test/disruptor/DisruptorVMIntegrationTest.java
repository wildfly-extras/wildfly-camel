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
package org.wildfly.camel.test.disruptor;

import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.impl.DefaultCamelContext;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.extension.camel.CamelAware;

@CamelAware
@RunWith(Arquillian.class)
public class DisruptorVMIntegrationTest {

    @Deployment
    public static JavaArchive createDeployment() {
        return ShrinkWrap.create(JavaArchive.class, "camel-disruptor-vm-tests.jar");
    }

    @Test
    public void testDisruptorVMComponent() throws Exception {
        CamelContext camelctxA = new DefaultCamelContext();
        camelctxA.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("disruptor-vm:contextA")
                .to("mock:result");
            }
        });

        CamelContext camelctxB = new DefaultCamelContext();
        camelctxB.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:start")
                .to("disruptor-vm:contextA");
            }
        });

        MockEndpoint mockEndpoint = camelctxA.getEndpoint("mock:result", MockEndpoint.class);
        mockEndpoint.expectedBodiesReceived("Hello Kermit");

        camelctxA.start();
        camelctxB.start();
        try {
            ProducerTemplate template = camelctxB.createProducerTemplate();
            template.sendBody("direct:start", "Hello Kermit");

            mockEndpoint.assertIsSatisfied();
        } finally {
            camelctxB.stop();
            camelctxA.stop();
        }
    }
}
