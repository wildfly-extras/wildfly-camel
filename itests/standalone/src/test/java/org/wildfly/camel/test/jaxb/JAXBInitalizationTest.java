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

package org.wildfly.camel.test.jaxb;

import org.apache.camel.CamelContext;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.model.ModelHelper;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * This test shows that When JAXB context is initialized and jaxb-impl classes are loaded, TCCL must be present.
 * Otherwise we get <code>Caused by: java.lang.ClassNotFoundException: __redirected/__DatatypeFactory</code> exception
 */
@RunWith(Arquillian.class)
public class JAXBInitalizationTest {

    @Deployment
    public static WebArchive deployment() {
        return ShrinkWrap.create(WebArchive.class, "jaxb-initialization-tests.war");
    }

    @Test
    public void testDumpingCamelModel() throws Exception {

        CamelContext camelctx = new DefaultCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:start")
                        .routeId("route-1")
                        .to("log:test");
            }
        });

        // this will set TCCL anyway
        camelctx.start();

        ClassLoader tccl = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(null);
            String xml = ModelHelper.dumpModelAsXml(camelctx, camelctx.getRouteDefinition("route-1"));
            Assert.assertTrue(xml.contains("log:test"));
        } finally {
            Thread.currentThread().setContextClassLoader(tccl);
            camelctx.stop();
        }
    }

}
