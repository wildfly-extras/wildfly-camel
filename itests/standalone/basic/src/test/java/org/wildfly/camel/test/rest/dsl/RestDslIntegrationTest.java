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

import org.apache.camel.CamelContext;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.impl.DefaultCamelContext;
import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.camel.test.common.http.HttpRequest;
import org.wildfly.extension.camel.CamelAware;

@CamelAware
@RunWith(Arquillian.class)
public class RestDslIntegrationTest {

    private static final String CAMEL_REST_SPRING_JAR = "camel-rest-spring.jar";

    @ArquillianResource
    private Deployer deployer;

    @Deployment
    public static JavaArchive createDeployment() {
        return ShrinkWrap.create(JavaArchive.class, "camel-rest-dsl-tests")
            .addClasses(HttpRequest.class);
    }

    @Deployment(testable = false, managed = false, name = CAMEL_REST_SPRING_JAR)
    public static JavaArchive camelRestSpringDeployment() {
        return ShrinkWrap.create(JavaArchive.class, CAMEL_REST_SPRING_JAR)
            .addAsResource("rest/rest-camel-context.xml", "camel-context.xml")
            .addClass(HttpRequest.class);
    }

    @Test
    public void testRestDsl() throws Exception {
        CamelContext camelctx = new DefaultCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                restConfiguration()
                    .component("undertow")
                    .host("localhost")
                    .port(8080);

                rest()
                    .get("/test")
                        .route()
                            .setBody(constant("GET: /test"))
                        .endRest()

                    .get("/foo/bar")
                        .route()
                           .setBody(constant("GET: /foo/bar"))
                        .endRest()

                    .get("/test/{id}")
                        .route()
                            .setBody(simple("GET: /test/${header.id}"))
                        .endRest()

                    .post("/test")
                        .route()
                            .setBody(constant("POST: /test"))
                        .endRest()

                    .put("/test/{id}")
                        .route()
                            .setBody(simple("PUT: /test/${header.id}"))
                        .endRest()

                    .delete("/test/{id}")
                        .route()
                            .setBody(simple("DELETE: /test/${header.id}"))
                        .endRest();
            }
        });

        camelctx.start();
        try {
            String body = HttpRequest.get("http://localhost:8080/test").getResponse().getBody();
            Assert.assertEquals("GET: /test", body);

            body = HttpRequest.get("http://localhost:8080/foo/bar").getResponse().getBody();
            Assert.assertEquals("GET: /foo/bar", body);

            body = HttpRequest.get("http://localhost:8080/test/1").getResponse().getBody();
            Assert.assertEquals("GET: /test/1", body);

            body = HttpRequest.post("http://localhost:8080/test").getResponse().getBody();
            Assert.assertEquals("POST: /test", body);

            body = HttpRequest.put("http://localhost:8080/test/1").getResponse().getBody();
            Assert.assertEquals("PUT: /test/1", body);

            body = HttpRequest.delete("http://localhost:8080/test/1").getResponse().getBody();
            Assert.assertEquals("DELETE: /test/1", body);

            int statusCode = HttpRequest.get("http://localhost:8080/test/foo/bar").throwExceptionOnFailure(false).getResponse().getStatusCode();
            Assert.assertEquals(404, statusCode);
        } finally {
            camelctx.stop();
        }
    }

    @Test
    public void testRestDslWithContextPath() throws Exception {
        CamelContext camelctx = new DefaultCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                restConfiguration()
                    .component("undertow")
                    .contextPath("camel-rest-dsl-tests")
                    .host("localhost")
                    .port(8080);

                rest("/test")
                    .get("/")
                        .route()
                            .setBody(constant("GET: /test"))
                        .endRest()

                    .get("/foo/bar")
                        .route()
                            .setBody(constant("GET: /test/foo/bar"))
                        .endRest()

                    .get("/{id}")
                        .route()
                            .setBody(simple("GET: /test/${header.id}"))
                        .endRest()

                    .post("/")
                        .route()
                            .setBody(constant("POST: /test"))
                        .endRest()

                    .put("/{id}")
                        .route()
                            .setBody(simple("PUT: /test/${header.id}"))
                        .endRest()

                    .delete("/{id}")
                        .route()
                            .setBody(simple("DELETE: /test/${header.id}"))
                        .endRest();
            }
        });

        camelctx.start();
        try {
            String body = HttpRequest.get("http://localhost:8080/camel-rest-dsl-tests/test").getResponse().getBody();
            Assert.assertEquals("GET: /test", body);

            body = HttpRequest.get("http://localhost:8080/camel-rest-dsl-tests/test/foo/bar").getResponse().getBody();
            Assert.assertEquals("GET: /test/foo/bar", body);

            body = HttpRequest.get("http://localhost:8080/camel-rest-dsl-tests/test/1").getResponse().getBody();
            Assert.assertEquals("GET: /test/1", body);

            body = HttpRequest.post("http://localhost:8080/camel-rest-dsl-tests/test").getResponse().getBody();
            Assert.assertEquals("POST: /test", body);

            body = HttpRequest.put("http://localhost:8080/camel-rest-dsl-tests/test/1").getResponse().getBody();
            Assert.assertEquals("PUT: /test/1", body);

            body = HttpRequest.delete("http://localhost:8080/camel-rest-dsl-tests/test/1").getResponse().getBody();
            Assert.assertEquals("DELETE: /test/1", body);

            int statusCode = HttpRequest.get("http://localhost:8080/camel-rest-dsl-tests/foo/bar").throwExceptionOnFailure(false).getResponse().getStatusCode();
            Assert.assertEquals(404, statusCode);
        } finally {
            camelctx.stop();
        }
    }

    @Test
    public void testRestDslRequestWithInvalidMethod() throws Exception {
        CamelContext camelctx = new DefaultCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                restConfiguration()
                    .component("undertow")
                    .host("localhost")
                    .port(8080);

                rest()
                    .get("/foo/bar")
                        .route()
                            .setBody(constant("GET: /test"))
                        .endRest();
            }
        });

        camelctx.start();
        try {
            int statusCode = HttpRequest.delete("http://localhost:8080/foo/bar").throwExceptionOnFailure(false).getResponse().getStatusCode();
            Assert.assertEquals(405, statusCode);
        } finally {
            camelctx.stop();
        }
    }

    @Test(expected = IllegalStateException.class)
    public void testRestDslOverlappingPaths() throws Exception {
        CamelContext camelctx = new DefaultCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                restConfiguration()
                    .component("undertow")
                    .host("localhost")
                    .port(8080);

                rest()
                    .get("/say/hello")
                        .route()
                          .setBody(constant("GET: /say/hello"))
                        .endRest();
            }
        });

        deployer.deploy(CAMEL_REST_SPRING_JAR);
        try {
            camelctx.start();
        } finally {
            camelctx.stop();
            deployer.undeploy(CAMEL_REST_SPRING_JAR);
        }
    }

    @Test
    public void testRestDslHandlerUnregistered() throws Exception {
        CamelContext camelctx = new DefaultCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                restConfiguration()
                    .component("undertow")
                    .host("localhost")
                    .port(8080);

                rest()
                    .get("/test")
                        .route()
                            .setBody(constant("GET: /test"))
                        .endRest();
            }
        });

        camelctx.start();
        try {
            int statusCode = HttpRequest.get("http://localhost:8080/test").getResponse().getStatusCode();
            Assert.assertEquals(200, statusCode);
        } finally {
            camelctx.stop();
        }

        int statusCode = HttpRequest.get("http://localhost:8080/test").throwExceptionOnFailure(false).getResponse().getStatusCode();
        Assert.assertEquals(404, statusCode);
    }
}
