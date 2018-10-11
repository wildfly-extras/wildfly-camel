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

package org.wildfly.camel.test.servlet;

import java.nio.charset.StandardCharsets;

import static javax.xml.bind.DatatypeConverter.printBase64Binary;
import static org.wildfly.camel.test.common.http.HttpRequest.HttpResponse;
import static org.wildfly.camel.test.servlet.subA.SecurityDomainSetupTask.SECURITY_DOMAIN;

import org.apache.camel.CamelContext;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.impl.DefaultCamelContext;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.camel.test.common.http.HttpRequest;
import org.wildfly.camel.test.servlet.subA.SecurityDomainSetupTask;
import org.wildfly.extension.camel.CamelAware;

/**
 * Tests basic authentication with Elytron securing a camel-servlet REST DSL endpoint path
 */
@CamelAware
@RunWith(Arquillian.class)
@ServerSetup({SecurityDomainSetupTask.class})
public class SecureServletIntegrationTest {

    private static final String JBOSS_WEB = "<jboss-web><security-domain>" + SECURITY_DOMAIN + "</security-domain></jboss-web>";

    @Deployment
    public static WebArchive createDeployment() {
        return ShrinkWrap.create(WebArchive.class, "camel-secure-servlet.war")
            .addAsWebInfResource("servlet/secure-web.xml", "web.xml")
            .addAsWebInfResource(new StringAsset(JBOSS_WEB), "jboss-web.xml")
            .addClasses(HttpRequest.class, SecurityDomainSetupTask.class);
    }

    @Test
    public void testSecureServletRoute() throws Exception {
        CamelContext camelctx = new DefaultCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                restConfiguration()
                    .component("servlet");

                rest()
                    .get("/insecure")
                        .route()
                            .setBody(constant("GET: /insecure"))
                        .endRest()
                    .get("/secure")
                        .route()
                            .setBody(constant("GET: /secure"))
                        .endRest();
            }
        });

        camelctx.start();
        try {
            // Insecure should work without authentication
            HttpResponse response = HttpRequest.get("http://localhost:8080/camel-secure-servlet/services/insecure")
                .throwExceptionOnFailure(false)
                .getResponse();
            Assert.assertEquals(200, response.getStatusCode());
            Assert.assertEquals("GET: /insecure", response.getBody());

            // No credentials provided so expect HTTP 401
            int statusCode = HttpRequest.get("http://localhost:8080/camel-secure-servlet/services/secure")
                .throwExceptionOnFailure(false)
                .getResponse()
                .getStatusCode();
            Assert.assertEquals(401, statusCode);

            // Invalid credentials provided so expect HTTP 401
            statusCode = HttpRequest.get("http://localhost:8080/camel-secure-servlet/services/secure")
                .throwExceptionOnFailure(false)
                .header("Authorization", "Basic " + printBase64Binary("bad-user:bad-password".getBytes(StandardCharsets.UTF_8)))
                .getResponse()
                .getStatusCode();
            Assert.assertEquals(401, statusCode);

            // Pass valid credentials and expect HTTP 200
            response = HttpRequest.get("http://localhost:8080/camel-secure-servlet/services/secure")
                .throwExceptionOnFailure(false)
                .header("Authorization", "Basic " + printBase64Binary("camel-servlet-user:camel-servlet-password".getBytes(StandardCharsets.UTF_8)))
                .getResponse();
            Assert.assertEquals(200, response.getStatusCode());
            Assert.assertEquals("GET: /secure", response.getBody());
        } finally {
            camelctx.stop();
        }
    }
}
