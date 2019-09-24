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

import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.impl.DefaultCamelContext;
import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.wildfly.camel.test.common.http.HttpRequest;
import org.wildfly.camel.test.common.http.HttpRequest.HttpResponse;
import org.wildfly.extension.camel.CamelAware;
import static java.net.HttpURLConnection.HTTP_OK;

@RunWith(Arquillian.class)
@CamelAware
public class UndertowIntegrationTest {

    private static final String TEST_SERVLET_WAR = "test-servlet.war";

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @ArquillianResource
    private Deployer deployer;

    @Deployment
    public static WebArchive createDeployment() {
        return ShrinkWrap.create(WebArchive.class, "UndertowIntegrationTest.war")
            .addClasses(HttpRequest.class);
    }

    @Deployment(testable = false, managed = false, name = TEST_SERVLET_WAR)
    public static WebArchive createTestServletDeployment() {
        return ShrinkWrap.create(WebArchive.class, TEST_SERVLET_WAR)
            .addClass(HttpRequest.class);
    }

    @Test
    public void testUndertowConsumer() throws Exception {
        CamelContext camelctx = new DefaultCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("undertow:http://localhost/myapp")
                .setBody(simple("Hello ${header.name} from /"));

                from("undertow:http://localhost/myapp/a")
                .setBody(simple("Hello ${header.name} from /a"));

                from("undertow:http://localhost/myapp/a/b")
                .setBody(simple("Hello ${header.name} from /a/b"));

                from("undertow:http://localhost/myapp/a/b/c")
                .setBody(simple("Hello ${header.name} from /a/b/c"));

                from("undertow:http://localhost/myapp/b")
                .setBody(simple("Hello ${header.name} from /b"));

                from("undertow:http://localhost/myapp/c")
                .setBody(simple("Hello ${header.name} from /c"));
            }
        });

        camelctx.start();
        try {
            HttpResponse response = HttpRequest.get("http://localhost:8080/myapp?name=Kermit").getResponse();
            Assert.assertEquals(HTTP_OK, response.getStatusCode());
            Assert.assertEquals("Hello Kermit from /", response.getBody());

            response = HttpRequest.get("http://localhost:8080/myapp/a?name=Kermit").getResponse();
            Assert.assertEquals(HTTP_OK, response.getStatusCode());
            Assert.assertEquals("Hello Kermit from /a", response.getBody());

            response = HttpRequest.get("http://localhost:8080/myapp/a/b?name=Kermit").getResponse();
            Assert.assertEquals(HTTP_OK, response.getStatusCode());
            Assert.assertEquals("Hello Kermit from /a/b", response.getBody());

            response = HttpRequest.get("http://localhost:8080/myapp/a/b/c?name=Kermit").getResponse();
            Assert.assertEquals(HTTP_OK, response.getStatusCode());
            Assert.assertEquals("Hello Kermit from /a/b/c", response.getBody());

            response = HttpRequest.get("http://localhost:8080/myapp/b?name=Kermit").getResponse();
            Assert.assertEquals(HTTP_OK, response.getStatusCode());
            Assert.assertEquals("Hello Kermit from /b", response.getBody());

            response = HttpRequest.get("http://localhost:8080/myapp/c?name=Kermit").getResponse();
            Assert.assertEquals(HTTP_OK, response.getStatusCode());
            Assert.assertEquals("Hello Kermit from /c", response.getBody());
        } finally {
            camelctx.close();
        }
    }

    @Test
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
            camelctx.close();
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
            camelctx.close();
        }
    }

    @Test
    public void overwriteCamelUndertowContextPathTest() throws Exception {
        CamelContext camelctx = new DefaultCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("undertow:http://localhost/test-servlet")
                .setBody(constant("Hello Kermit"));
            }
        });

        camelctx.start();
        try {
            // WAR deployment should fail as the context path is already registered by camel-undertow
            expectedException.expect(RuntimeException.class);
            deployer.deploy(TEST_SERVLET_WAR);
        } finally {
            camelctx.close();
            deployer.undeploy(TEST_SERVLET_WAR);
        }
    }

    @Test
    public void overwriteDeploymentContextPathTest() throws Exception {
        CamelContext camelctx = new DefaultCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("undertow:http://localhost/test-servlet")
                .setBody(constant("Hello Kermit"));
            }
        });

        deployer.deploy(TEST_SERVLET_WAR);
        try {
            // Context start should fail as the undertow consumer path is already registered by test-servlet.war
            expectedException.expect(IllegalStateException.class);
            camelctx.start();
        } finally {
            camelctx.close();
            deployer.undeploy(TEST_SERVLET_WAR);
        }
    }
}
