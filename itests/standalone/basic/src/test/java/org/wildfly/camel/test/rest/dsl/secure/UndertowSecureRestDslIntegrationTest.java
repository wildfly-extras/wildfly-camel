/*
 * #%L
 * Wildfly Camel :: Testsuite
 * %%
 * Copyright (C) 2013 - 2018 RedHat
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
package org.wildfly.camel.test.rest.dsl.secure;

import static org.wildfly.camel.test.common.security.BasicSecurityDomainASetup.APPLICATION_PASSWORD;
import static org.wildfly.camel.test.common.security.BasicSecurityDomainASetup.APPLICATION_PASSWORD_SUB;
import static org.wildfly.camel.test.common.security.BasicSecurityDomainASetup.APPLICATION_ROLE;
import static org.wildfly.camel.test.common.security.BasicSecurityDomainASetup.APPLICATION_ROLE_SUB;
import static org.wildfly.camel.test.common.security.BasicSecurityDomainASetup.APPLICATION_USER;
import static org.wildfly.camel.test.common.security.BasicSecurityDomainASetup.APPLICATION_USER_SUB;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.camel.CamelContext;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.spi.RestConfiguration;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.camel.test.common.security.BasicSecurityDomainASetup;
import org.wildfly.camel.test.common.security.SecurityUtils;
import org.wildfly.camel.test.cxf.rs.secure.CXFRSSecureUtils;
import org.wildfly.extension.camel.CamelAware;

/**
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 */
@CamelAware
@RunWith(Arquillian.class)
@ServerSetup(BasicSecurityDomainASetup.class)
public class UndertowSecureRestDslIntegrationTest {
    private static final Map<String, String> PATH_ROLE_MAP = new LinkedHashMap<String, String>() {
        private static final long serialVersionUID = 1L;
        {
            put("///test/*", APPLICATION_ROLE);
            put("///foo/*", APPLICATION_ROLE_SUB);
        }
    };

    private static final String UNDERTOW_COMPONENT = "undertow";

    @Deployment
    public static WebArchive createDeployment() {
        final WebArchive archive = ShrinkWrap.create(WebArchive.class, "UndertowSecureRestDslIntegrationTest.war")
                .addClasses(BasicSecurityDomainASetup.class, CXFRSSecureUtils.class, TestClient.class);
        SecurityUtils.enhanceArchive(archive, BasicSecurityDomainASetup.SECURITY_DOMAIN,
                BasicSecurityDomainASetup.AUTH_METHOD, PATH_ROLE_MAP);
        return archive;
    }

    private RestConfiguration createRestConfiguration(String contextPath) {
        RestConfiguration configuration = new RestConfiguration();
        configuration.setComponent(UNDERTOW_COMPONENT);
        configuration.setHost(TestClient.HOST);
        configuration.setPort(TestClient.PORT);
        configuration.setContextPath(contextPath);
        return configuration;
    }

    @Test
    public void testRestDsl() throws Exception {
        CamelContext ctx1 = new DefaultCamelContext();
        ctx1.setRestConfiguration(createRestConfiguration("/"));
        ctx1.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
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
                        .endRest()
                   ;
            }
        });

        CamelContext ctx2 = new DefaultCamelContext();
        ctx2.setRestConfiguration(createRestConfiguration("/"));
        ctx2.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                rest()
                    .post("/foo/bar")
                        .route()
                           .setBody(constant("POST: /foo/bar"))
                        .endRest()

                   ;
            }
        });
        try (TestClient c = new TestClient()) {

            /* Endpoints must not work before the start of ctx1 */
            c.assertResponse("/test", "GET", APPLICATION_USER, APPLICATION_PASSWORD, 404);
            c.assertResponse("/test", "GET", APPLICATION_USER_SUB, APPLICATION_PASSWORD_SUB, 404);
            c.assertResponse("/test", "GET", null, null, 404);

            c.assertResponse("/foo/bar", "GET", APPLICATION_USER, APPLICATION_PASSWORD, 404);
            c.assertResponse("/foo/bar", "GET", APPLICATION_USER_SUB, APPLICATION_PASSWORD_SUB, 404);
            c.assertResponse("/foo/bar", "GET", null, null, 404);

            ctx1.start();
            try {
                c.assertResponse("/test", "GET", APPLICATION_USER, APPLICATION_PASSWORD, 200);
                c.assertResponse("/test", "GET", APPLICATION_USER_SUB, APPLICATION_PASSWORD_SUB, 403);
                c.anonymousUnauthorized("/test", "GET");

                c.assertResponse("/test/1", "GET", APPLICATION_USER, APPLICATION_PASSWORD, 200);
                c.assertResponse("/test/1", "GET", APPLICATION_USER_SUB, APPLICATION_PASSWORD_SUB, 403);
                c.anonymousUnauthorized("/test/1", "GET");

                c.assertResponse("/test", "POST", APPLICATION_USER, APPLICATION_PASSWORD, 200);
                c.assertResponse("/test", "POST", APPLICATION_USER_SUB, APPLICATION_PASSWORD_SUB, 403);
                c.anonymousUnauthorized("/test", "POST");

                c.assertResponse("/test/1", "PUT", APPLICATION_USER, APPLICATION_PASSWORD, 200);
                c.assertResponse("/test/1", "PUT", APPLICATION_USER_SUB, APPLICATION_PASSWORD_SUB, 403);
                c.anonymousUnauthorized("/test/1", "PUT");

                c.assertResponse("/test/1", "DELETE", APPLICATION_USER, APPLICATION_PASSWORD, 200);
                c.assertResponse("/test/1", "DELETE", APPLICATION_USER_SUB, APPLICATION_PASSWORD_SUB, 403);
                c.anonymousUnauthorized("/test/1", "DELETE");


                c.assertResponse("/foo/bar", "GET", APPLICATION_USER, APPLICATION_PASSWORD, 403);
                c.assertResponse("/foo/bar", "GET", APPLICATION_USER_SUB, APPLICATION_PASSWORD_SUB, 200);
                c.anonymousUnauthorized("/foo/bar", "GET");

                c.assertResponse("/foo/bar", "POST", APPLICATION_USER, APPLICATION_PASSWORD, 403);
                c.assertResponse("/foo/bar", "POST", APPLICATION_USER_SUB, APPLICATION_PASSWORD_SUB, 405);
                c.anonymousUnauthorized("/foo/bar", "POST");

                ctx2.start();
                try {
                    c.assertResponse("/foo/bar", "POST", APPLICATION_USER, APPLICATION_PASSWORD, 403);
                    c.assertResponse("/foo/bar", "POST", APPLICATION_USER_SUB, APPLICATION_PASSWORD_SUB, 200);
                    c.anonymousUnauthorized("/foo/bar", "POST");
                } finally {
                    ctx2.stop();
                }

                /* Endpoints from ctx1 must keep working after we stopped ctx2 */
                c.assertResponse("/foo/bar", "GET", APPLICATION_USER, APPLICATION_PASSWORD, 403);
                c.assertResponse("/foo/bar", "GET", APPLICATION_USER_SUB, APPLICATION_PASSWORD_SUB, 200);
                c.anonymousUnauthorized("/foo/bar", "GET");

                c.assertResponse("/foo/bar", "POST", APPLICATION_USER, APPLICATION_PASSWORD, 403);
                c.assertResponse("/foo/bar", "POST", APPLICATION_USER_SUB, APPLICATION_PASSWORD_SUB, 405);
                c.anonymousUnauthorized("/foo/bar", "POST");

            } finally {
                ctx1.stop();
            }

            /* Endpoints must not work after ctx1 was stopped */
            c.assertResponse("/test", "GET", APPLICATION_USER, APPLICATION_PASSWORD, 404);
            c.assertResponse("/test", "GET", APPLICATION_USER_SUB, APPLICATION_PASSWORD_SUB, 404);
            c.assertResponse("/test", "GET", null, null, 404);

            c.assertResponse("/foo/bar", "GET", APPLICATION_USER, APPLICATION_PASSWORD, 404);
            c.assertResponse("/foo/bar", "GET", APPLICATION_USER_SUB, APPLICATION_PASSWORD_SUB, 404);
            c.assertResponse("/foo/bar", "GET", null, null, 404);

        }
    }

}
