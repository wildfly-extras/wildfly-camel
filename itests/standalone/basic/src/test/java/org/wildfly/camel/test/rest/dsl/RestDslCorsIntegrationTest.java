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

package org.wildfly.camel.test.rest.dsl;

import java.util.Map;

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
import org.wildfly.camel.test.common.http.HttpRequest;
import org.wildfly.extension.camel.CamelAware;

@CamelAware
@RunWith(Arquillian.class)
public class RestDslCorsIntegrationTest {

    private static final String[] CORS_HEADERS = { "Access-Control-Allow-Origin", "Access-Control-Allow-Methods",
        "Access-Control-Allow-Headers", "Access-Control-Max-Age" };

    @Deployment
    public static JavaArchive createDeployment() {
        return ShrinkWrap.create(JavaArchive.class, "camel-rest-dsl-cors")
            .addClasses(HttpRequest.class);
    }

    @Test
    public void testRestDslCorsEnabled() throws Exception {
        CamelContext camelctx = new DefaultCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                restConfiguration()
                    .component("undertow")
                    .host("localhost")
                    .port(8080)
                    .enableCORS(true);

                rest()
                    .get("/test")
                        .route()
                            .setBody(constant("GET: /test"))
                        .endRest();
            }
        });

        camelctx.start();
        try {
            Map<String, String> headers = HttpRequest.options("http://localhost:8080/test").getResponse().getHeaders();

            for (String header : CORS_HEADERS) {
                Assert.assertTrue(headers.containsKey(header));
            }
        } finally {
            camelctx.stop();
        }
    }


    @Test
    public void testRestDslCorsDisabled() throws Exception {
        CamelContext camelctx = new DefaultCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                restConfiguration()
                    .component("undertow")
                    .host("localhost")
                    .port(8080)
                    .enableCORS(false);

                rest()
                    .get("/test")
                        .route()
                            .setBody(constant("GET: /test"))
                        .endRest();
            }
        });

        camelctx.start();
        try {
            Map<String, String> headers = HttpRequest.options("http://localhost:8080/test").getResponse().getHeaders();

            for (String header : CORS_HEADERS) {
                Assert.assertFalse(headers.containsKey(header));
            }
        } finally {
            camelctx.stop();
        }
    }
}
