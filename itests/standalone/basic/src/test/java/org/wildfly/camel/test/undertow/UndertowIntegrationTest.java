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

package org.wildfly.camel.test.undertow;

import java.net.HttpURLConnection;

import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.impl.DefaultCamelContext;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.camel.test.common.http.HttpRequest;
import org.wildfly.camel.test.common.http.HttpRequest.HttpResponse;
import org.wildfly.extension.camel.CamelAware;

@RunWith(Arquillian.class)
@CamelAware
public class UndertowIntegrationTest {

    @Deployment
    public static JavaArchive createDeployment() {
        return ShrinkWrap.create(JavaArchive.class, "undertow-tests")
            .addClasses(HttpRequest.class);
    }

    @Test
    public void testUndertowConsumer() throws Exception {
        CamelContext camelctx = new DefaultCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("undertow:http://localhost/myapp/serviceA")
                .setBody(simple("Hello ${header.name}"));
            }
        });

        camelctx.start();
        try {
            HttpResponse response = HttpRequest.get("http://localhost:8080/myapp/serviceA?name=Kermit").getResponse();
            Assert.assertEquals(HttpURLConnection.HTTP_OK, response.getStatusCode());
            Assert.assertEquals("Hello Kermit", response.getBody());
        } finally {
            camelctx.stop();
        }
    }

    @Test
    @Ignore("[#1809] Reenable undertow consumer prefix paths")
    public void testUndertowConsumerPrefixPath() throws Exception {
        CamelContext camelctx = new DefaultCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("undertow:http://localhost/foo/bar?matchOnUriPrefix=true")
                .to("mock:result");
            }
        });
        try {
            camelctx.start();

            MockEndpoint endpoint = camelctx.getEndpoint("mock:result", MockEndpoint.class);
            endpoint.setExpectedMessageCount(3);

            HttpRequest.get("http://localhost:8080/foo").throwExceptionOnFailure(false).getResponse();
            HttpRequest.get("http://localhost:8080/foo/bar").getResponse();
            HttpRequest.get("http://localhost:8080/foo/bar/hello").getResponse();
            HttpRequest.get("http://localhost:8080/foo/bar/hello/world").getResponse();

            endpoint.assertIsSatisfied();
        } finally {
            camelctx.stop();
        }
    }

    @Test
    public void testUndertowProducer() throws Exception {
        CamelContext camelctx = new DefaultCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("undertow:http://localhost/myapp/serviceB")
                .setBody(simple("Hello ${header.name}"));
            }
        });

        camelctx.start();
        try {
            ProducerTemplate template = camelctx.createProducerTemplate();
            String result = template.requestBody("undertow:http://localhost:8080/myapp/serviceB?name=Kermit", null, String.class);
            Assert.assertEquals("Hello Kermit", result);
        } finally {
            camelctx.stop();
        }
    }
}
