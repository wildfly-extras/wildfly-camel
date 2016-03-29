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

package org.wildfly.camel.test.swagger;

import java.lang.management.ManagementFactory;
import java.net.HttpURLConnection;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.ws.rs.core.MediaType;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.impl.DefaultCamelContext;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.camel.test.common.http.HttpRequest;
import org.wildfly.camel.test.swagger.subA.User;
import org.wildfly.extension.camel.CamelAware;

@CamelAware
@RunWith(Arquillian.class)
public class SwaggerIntegrationTest {

    @Deployment
    public static WebArchive createDeployment() {
        final WebArchive archive = ShrinkWrap.create(WebArchive.class, "swagger-tests.war");
        archive.addClasses(HttpRequest.class, User.class);
        return archive;
    }

    @Test
    public void testRestDsl() throws Exception {
        DefaultCamelContext camelctx = new DefaultCamelContext();
        camelctx.setName("swagger-test");
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                restConfiguration().component("undertow")
                    .contextPath("swagger-tests/rest")
                    .host("localhost")
                    .port(8080)
                    .apiContextPath("/api-doc")
                    .apiProperty("api.title", "User API").apiProperty("api.version", "1.2.3")
                    .apiProperty("cors", "true");
                rest("/hello")
                    .get("/{name}").description("A user object").outType(User.class).to("direct:hello")
                    .produces(MediaType.APPLICATION_JSON)
                    .consumes(MediaType.APPLICATION_JSON);
                from("direct:hello").transform(simple("Hello ${header.name}"));
            }
        });

        camelctx.start();
        try {
            HttpRequest.HttpResponse result = HttpRequest.get("http://localhost:8080/swagger-tests/rest/hello/Kermit").getResponse();
            Assert.assertEquals("Hello Kermit", result.getBody());

            MBeanServer server = ManagementFactory.getPlatformMBeanServer();
            for (ObjectName oname : server.queryNames(new ObjectName("*:type=context,*"), null)) {
                Object jmxret = server.invoke(oname, "dumpRestsAsXml", null, null);
                System.out.println(oname + ": " + jmxret);
            }

            result = HttpRequest.get("http://localhost:8080/swagger-tests/rest/api-doc").getResponse();
            Assert.assertEquals(HttpURLConnection.HTTP_OK, result.getStatusCode());
            Assert.assertTrue("Contains substr: " + result.getBody(), result.getBody().contains("\"name\" : \"hello\""));
        } finally {
            camelctx.stop();
        }
    }
}
