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
package org.wildfly.camel.test.cxf.ws;

import org.apache.camel.CamelContext;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.impl.DefaultCamelContext;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.camel.test.cxf.ws.subA.Endpoint;
import org.wildfly.extension.camel.CamelAware;

/**
 * Test WebService endpoint access with the cxf component.
 *
 * @author thomas.diesler@jboss.com
 * @since 11-Jun-2013
 */
@CamelAware
@RunWith(Arquillian.class)
public class CXFWSConsumerIntegrationTest {

    @Deployment
    public static JavaArchive deployment() {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "cxf-ws-consumer-tests");
        archive.addClasses(Endpoint.class);
        return archive;
    }

    @Test
    public void testCxfConsumer() throws Exception {

        CamelContext camelctx = new DefaultCamelContext();
        final String uri = "cxf:/webservices/?serviceClass=" + Endpoint.class.getName();

        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from(uri).to("direct:end");
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
}
