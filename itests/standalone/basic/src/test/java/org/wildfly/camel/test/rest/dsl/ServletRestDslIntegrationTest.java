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
package org.wildfly.camel.test.rest.dsl;

import org.apache.camel.CamelContext;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.impl.DefaultCamelContext;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.runner.RunWith;
import org.wildfly.camel.test.common.http.HttpRequest;

@RunWith(Arquillian.class)
public class ServletRestDslIntegrationTest extends AbstractRestDslIntegrationTest {

    @Deployment
    public static WebArchive createDeployment() {
        return ShrinkWrap.create(WebArchive.class, "camel-servlet-rest-dsl-tests.war")
            .addAsWebInfResource("servlet/web.xml", "web.xml")
            .addClasses(HttpRequest.class, AbstractRestDslIntegrationTest.class);
    }

    @Override
    protected String getComponentName() {
        return "servlet";
    }

    @Override
    protected String getDefaultContextPath() {
        return "/camel-servlet-rest-dsl-tests/services/";
    }

    @Override
    public void testRestDslWithContextPath() throws Exception {
        CamelContext camelctx = new DefaultCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                restConfiguration()
                    .component("servlet")
                    .host("localhost")
                    .port(8080)
                    .contextPath("/foo/bar");

                rest("/test")
                    .get("/")
                        .route()
                            .setBody(constant("GET: /foo/bar/test"))
                        .endRest();
            }
        });

        camelctx.start();
        try {
            // Custom context paths make no sense for camel-servlet so verify no endpoint is reachable
            int statusCode = HttpRequest.get("http://localhost:8080/foo/bar/test").throwExceptionOnFailure(false).getResponse().getStatusCode();
            Assert.assertEquals(404, statusCode);
        } finally {
            camelctx.stop();
        }
    }
}
