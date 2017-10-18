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
import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.camel.test.common.http.HttpRequest;
import org.wildfly.camel.test.common.http.HttpRequest.HttpResponse;
import org.wildfly.camel.test.rest.dsl.subA.ContextPathPrintingServlet;
import org.wildfly.extension.camel.CamelAware;

@CamelAware
@RunWith(Arquillian.class)
public class RestDslRootContextTest {

    private static final String SIMPLE_WAR = "simple.war";

    @ArquillianResource
    private Deployer deployer;

    @Deployment
    public static WebArchive createDeployment() {
        return ShrinkWrap.create(WebArchive.class, "camel-rest-dsl-root-context-tests.war")
            .addClasses(HttpRequest.class);
    }

    @Deployment(testable = false, name = SIMPLE_WAR, managed = false)
    public static WebArchive createDeployment2() {
        return ShrinkWrap.create(WebArchive.class, SIMPLE_WAR)
            .addClasses(ContextPathPrintingServlet.class);
    }

    @Test
    public void testRestDslRootContext() throws Exception {
        CamelContext camelctx = new DefaultCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                restConfiguration()
                    .component("undertow")
                    .contextPath("/")
                    .host("localhost")
                    .port(8080);

                rest()
                    .get()
                        .route()
                            .setBody(constant("GET: /"))
                        .endRest();
            }
        });

        camelctx.start();
        try {
            deployer.deploy(SIMPLE_WAR);

            // Verify root context path
            HttpResponse response = HttpRequest.get("http://localhost:8080/").getResponse();
            Assert.assertEquals(200, response.getStatusCode());
            Assert.assertTrue(response.getBody().contains("GET: /"));

            // Verify other deployed context paths
            response = HttpRequest.get("http://localhost:8080/simple").getResponse();
            Assert.assertEquals(200, response.getStatusCode());
            Assert.assertTrue(response.getBody().contains("GET: /simple"));

        } finally {
            deployer.undeploy(SIMPLE_WAR);
            camelctx.stop();
        }
    }
}
