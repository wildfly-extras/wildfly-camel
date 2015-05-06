/*
 * #%L
 * Wildfly Camel :: Testsuite
 * %%
 * Copyright (C) 2013 - 2015 RedHat
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
package org.wildfly.camel.test.cxf.rs;

import java.net.MalformedURLException;

import org.apache.camel.CamelContext;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.cxf.jaxrs.CxfRsComponent;
import org.apache.camel.component.cxf.jaxrs.CxfRsEndpoint;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.camel.test.cxf.rs.subA.GreetingService;
import org.wildfly.extension.camel.CamelAware;

@CamelAware
@RunWith(Arquillian.class)
public class RestConsumerIntegrationTest {

    @Deployment
    public static JavaArchive deployment() {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "jaxrs-consumer-tests");
        archive.addClasses(GreetingService.class);
        return archive;
    }

    @Test
    public void testCxfRsConsumer() throws Exception {

        CamelContext camelctx = new DefaultCamelContext();
        CxfRsComponent component = new CxfRsComponent();
        Bus defaultBus = BusFactory.getDefaultBus(true);
        String uri = "cxfrs://" + getEndpointAddress("/simple");

        final CxfRsEndpoint cxfRsEndpoint = new CxfRsEndpoint(uri, component);
        cxfRsEndpoint.setCamelContext(camelctx);
        cxfRsEndpoint.setBus(defaultBus);
        cxfRsEndpoint.setDefaultBus(true);
        cxfRsEndpoint.setResourceClasses(GreetingService.class);

        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from(cxfRsEndpoint).to("direct:end");
            }
        });

        try {
            camelctx.start();
            Assert.fail("Expected RuntimeCamelException to be thrown but it was not");
        } catch (RuntimeException e) {
            String message = e.getMessage();
            Assert.assertTrue("Message equals: " + message, message.equals("CXF consumer endpoint not supported"));
        }
    }

    private String getEndpointAddress(String contextPath) throws MalformedURLException {
        return "http://localhost:8080" + contextPath + "/rest/greet/hello/Kermit";
    }
}
