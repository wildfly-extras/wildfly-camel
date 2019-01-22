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
package org.wildfly.camel.test.plain.simple;

import java.lang.management.ManagementFactory;
import java.util.Collections;
import java.util.Set;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.apache.camel.CamelContext;
import org.apache.camel.CamelException;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.impl.DefaultCamelContext;
import org.junit.Assert;
import org.junit.Test;

public class SimpleManagementTest {

    @Test
    public void testAllGood() throws Exception {

        CamelContext camelctx = new DefaultCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:start").transform(body().prepend("Hello "));
            }
        });

        MBeanServer server = ManagementFactory.getPlatformMBeanServer();
        Set<ObjectName> onames = server.queryNames(new ObjectName("org.apache.camel:*"), null);
        Assert.assertEquals(Collections.emptySet(), onames);

        camelctx.start();
        try {
            onames = server.queryNames(new ObjectName("org.apache.camel:*"), null);
            Assert.assertTrue(onames.size() > 0);

            ProducerTemplate producer = camelctx.createProducerTemplate();
            String result = producer.requestBody("direct:start", "Kermit", String.class);
            Assert.assertEquals("Hello Kermit", result);
        } finally {
            camelctx.stop();
        }

        onames = server.queryNames(new ObjectName("org.apache.camel:*"), null);
        Assert.assertEquals(Collections.emptySet(), onames);
    }

    @Test
    public void testStartupFailure() throws Exception {

        CamelContext camelctx = new DefaultCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("invalid:start");
            }
        });

        MBeanServer server = ManagementFactory.getPlatformMBeanServer();
        Set<ObjectName> onames = server.queryNames(new ObjectName("org.apache.camel:*"), null);
        Assert.assertEquals(Collections.emptySet(), onames);

        try {
            camelctx.start();
            Assert.fail("Startup failure expected");
        } catch (CamelException ex) {
            System.out.println(">>>>>>> Startup Exception: " + ex);
            // expected
        }

        onames = server.queryNames(new ObjectName("org.apache.camel:*"), null);
        Assert.assertEquals(Collections.emptySet(), onames);
    }
}
