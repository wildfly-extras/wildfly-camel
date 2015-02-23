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

package org.wildfly.camel.test.rest;

import org.apache.camel.CamelContext;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.impl.DefaultCamelContext;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.camel.test.HttpRequest;

import java.util.concurrent.TimeUnit;

@RunWith(Arquillian.class)
public class RestDslIntegrationTest {

    @Deployment
    public static WebArchive createDeployment() {
        final WebArchive archive = ShrinkWrap.create(WebArchive.class, "camel.war");
        archive.addAsWebInfResource("rest/web.xml", "web.xml");
        archive.addClass(HttpRequest.class);
        return archive;
    }

    @Test
    public void testRestDsl() throws Exception {
        CamelContext camelctx = new DefaultCamelContext();

        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                restConfiguration().component("servlet").contextPath("camel/rest").port(8080);

                rest("/hello")
                    .get("/{name}")
                        .to("direct:hello");

                from("direct:hello")
                        .transform(simple("Hello ${header.name}"));
            }
        });

        camelctx.start();

        String result = HttpRequest.get("http://localhost:8080/camel/rest/hello/Kermit", 10, TimeUnit.SECONDS);
        Assert.assertEquals("Hello Kermit", result);

        camelctx.stop();
    }
}
