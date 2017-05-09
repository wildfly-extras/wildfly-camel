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
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.camel.test.common.http.HttpRequest;
import org.wildfly.camel.test.common.http.HttpRequest.HttpResponse;
import org.wildfly.extension.camel.CamelAware;

@CamelAware
@RunWith(Arquillian.class)
public class RestDslMultipleVerbsIntegrationTest {

    @Deployment
    public static JavaArchive createDeployment() {
        return ShrinkWrap.create(JavaArchive.class, "camel-rest-dsl-verbs")
            .addClasses(HttpRequest.class);
    }

    @Test
    public void testRestDslMultipleVerbs() throws Exception {
        CamelContext camelctx = new DefaultCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                restConfiguration()
                    .component("undertow")
                    .contextPath("camel/rest")
                    .host("localhost")
                    .port(8080);

                rest("/hello")
                    .get("/{name}")
                        .to("direct:get")
                            .post("/{name}")
                            .to("direct:post")
                    .put("/{name}")
                        .to("direct:put");

                from("direct:get").setBody(simple("GET ${header.name}"));
                from("direct:post").setBody(simple("POST ${header.name}"));
                from("direct:put").setBody(simple("PUT ${header.name}"));
            }
        });

        camelctx.start();
        try {
            HttpResponse result = HttpRequest.get("http://localhost:8080/camel/rest/hello/Kermit").getResponse();
            Assert.assertEquals("GET Kermit", result.getBody());

            result = HttpRequest.post("http://localhost:8080/camel/rest/hello/Kermit").getResponse();
            Assert.assertEquals("POST Kermit", result.getBody());

            result = HttpRequest.put("http://localhost:8080/camel/rest/hello/Kermit").getResponse();
            Assert.assertEquals("PUT Kermit", result.getBody());
        } finally {
            camelctx.stop();
        }
    }
}
