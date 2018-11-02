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
import static org.wildfly.camel.test.common.security.BasicSecurityDomainASetup.APPLICATION_PASSWORD_REL;
import static org.wildfly.camel.test.common.security.BasicSecurityDomainASetup.APPLICATION_PASSWORD_SUB;
import static org.wildfly.camel.test.common.security.BasicSecurityDomainASetup.APPLICATION_ROLE;
import static org.wildfly.camel.test.common.security.BasicSecurityDomainASetup.APPLICATION_ROLE_REL;
import static org.wildfly.camel.test.common.security.BasicSecurityDomainASetup.APPLICATION_ROLE_SUB;
import static org.wildfly.camel.test.common.security.BasicSecurityDomainASetup.APPLICATION_USER;
import static org.wildfly.camel.test.common.security.BasicSecurityDomainASetup.APPLICATION_USER_REL;
import static org.wildfly.camel.test.common.security.BasicSecurityDomainASetup.APPLICATION_USER_SUB;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.http.HttpEntity;
import org.apache.http.ParseException;
import org.apache.http.util.EntityUtils;
import org.jboss.arquillian.container.spi.client.container.DeploymentException;
import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.junit.Assert;
import org.junit.runner.RunWith;

/**
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 */
@RunWith(Arquillian.class)
public abstract class AbstractUndertowSecureRestDslClientTest {

    static final Map<String, String> PATH_ROLE_MAP_1 = new LinkedHashMap<String, String>() {
        private static final long serialVersionUID = 1L;
        {
            put("///test/*", APPLICATION_ROLE);
        }
    };
    static final Map<String, String> PATH_ROLE_MAP_2 = new LinkedHashMap<String, String>() {
        private static final long serialVersionUID = 1L;
        {
            put("///test/sub/*", APPLICATION_ROLE_SUB);
            put("///foo/*", APPLICATION_ROLE_SUB);
        }
    };
    static final Map<String, String> PATH_ROLE_MAP_3 = new LinkedHashMap<String, String>() {
        private static final long serialVersionUID = 1L;
        {
            put("///test/*", APPLICATION_ROLE_REL);
            put("///app3/*", APPLICATION_ROLE_REL);
        }
    };
    static final Map<String, String> PATH_ROLE_MAP_4 = new LinkedHashMap<String, String>() {
        private static final long serialVersionUID = 1L;
        {
            put("///api/endpoint1/*", APPLICATION_ROLE);
            put("///api/swagger/*", APPLICATION_ROLE_SUB);
        }
    };

    @ArquillianResource
    Deployer deployer;

    public void swagger(String app1) throws Exception {

        try (TestClient c = new TestClient()) {

            /* None of these endpoints may work before the start of app1 */
            c.assertResponse("/api/endpoint1", "GET", APPLICATION_USER, APPLICATION_PASSWORD, 404);
            c.assertResponse("/api/endpoint1", "GET", APPLICATION_USER_SUB, APPLICATION_PASSWORD_SUB, 404);
            c.assertResponse("/api/endpoint1", "GET", null, null, 404);

            c.assertResponse("/api/swagger", "GET", APPLICATION_USER, APPLICATION_PASSWORD, 404);
            c.assertResponse("/api/swagger", "GET", APPLICATION_USER_SUB, APPLICATION_PASSWORD_SUB, 404);
            c.assertResponse("/api/swagger", "GET", null, null, 404);

            deployer.deploy(app1);
            try {
                c.assertResponse("/api/endpoint1", "GET", APPLICATION_USER, APPLICATION_PASSWORD, 200);
                c.assertResponse("/api/endpoint1", "GET", APPLICATION_USER_SUB, APPLICATION_PASSWORD_SUB, 403);
                c.anonymousUnauthorized("/api/endpoint1", "GET");

                c.assertResponse("/api/swagger", "GET", APPLICATION_USER, APPLICATION_PASSWORD, 403);
                c.assertResponse("/api/swagger", "GET", APPLICATION_USER_SUB, APPLICATION_PASSWORD_SUB, response -> {
                    final int actualCode = response.getStatusLine().getStatusCode();
                    Assert.assertEquals(200, actualCode);
                    final HttpEntity entity = response.getEntity();
                    try {
                        final String body = EntityUtils.toString(entity, StandardCharsets.UTF_8);
                        Assert.assertTrue(body.contains("\"summary\" : \"A test endpoint1\""));
                        Assert.assertTrue(body.contains("\"operationId\" : \"endpoint1\""));
                    } catch (ParseException | IOException e) {
                        throw new RuntimeException(e);
                    }
                });
                c.anonymousUnauthorized("/api/swagger", "GET");
            } finally {
                deployer.undeploy(app1);
            }

            /* Must not work after of app1 was undeployed */
            c.assertResponse("/api/endpoint1", "GET", APPLICATION_USER, APPLICATION_PASSWORD, 404);
            c.assertResponse("/api/endpoint1", "GET", APPLICATION_USER_SUB, APPLICATION_PASSWORD_SUB, 404);
            c.assertResponse("/api/endpoint1", "GET", null, null, 404);

            c.assertResponse("/api/swagger", "GET", APPLICATION_USER, APPLICATION_PASSWORD, 404);
            c.assertResponse("/api/swagger", "GET", APPLICATION_USER_SUB, APPLICATION_PASSWORD_SUB, 404);
            c.assertResponse("/api/swagger", "GET", null, null, 404);

        }
    }

    public void pathConflicts(String app1, String app2, String app3) throws Exception {

        try (TestClient c = new TestClient()) {

            /* None of these endpoints may work before the start of app1 and app2 */
            c.assertResponse("/test", "GET", APPLICATION_USER, APPLICATION_PASSWORD, 404);
            c.assertResponse("/test", "GET", APPLICATION_USER_SUB, APPLICATION_PASSWORD_SUB, 404);
            c.assertResponse("/test", "GET", null, null, 404);

            c.assertResponse("/test/sub", "GET", APPLICATION_USER, APPLICATION_PASSWORD, 404);
            c.assertResponse("/test/sub", "GET", APPLICATION_USER_SUB, APPLICATION_PASSWORD_SUB, 404);
            c.assertResponse("/test/sub", "GET", null, null, 404);

            c.assertResponse("/foo/bar", "GET", APPLICATION_USER, APPLICATION_PASSWORD, 404);
            c.assertResponse("/foo/bar", "GET", APPLICATION_USER_SUB, APPLICATION_PASSWORD_SUB, 404);
            c.assertResponse("/foo/bar", "GET", null, null, 404);

            c.assertResponse("/app3", "GET", APPLICATION_USER, APPLICATION_PASSWORD, 404);
            c.assertResponse("/app3", "GET", APPLICATION_USER_SUB, APPLICATION_PASSWORD_SUB, 404);
            c.assertResponse("/app3", "GET", APPLICATION_USER_REL, APPLICATION_PASSWORD_REL, 404);
            c.assertResponse("/app3", "GET", null, null, 404);

            c.assertResponse("/test", "POST", APPLICATION_USER, APPLICATION_PASSWORD, 404);
            c.assertResponse("/test", "POST", APPLICATION_USER_SUB, APPLICATION_PASSWORD_SUB, 404);
            c.assertResponse("/test", "POST", APPLICATION_USER_REL, APPLICATION_PASSWORD_REL, 404);
            c.assertResponse("/test", "POST", null, null, 404);

            deployer.deploy(app1);
            try {
                c.assertResponse("/test", "GET", APPLICATION_USER, APPLICATION_PASSWORD, 200);
                c.assertResponse("/test", "GET", APPLICATION_USER_SUB, APPLICATION_PASSWORD_SUB, 403);
                c.anonymousUnauthorized("/test", "GET");

                c.assertResponse("/test", "POST", APPLICATION_USER, APPLICATION_PASSWORD, 405);
                c.assertResponse("/test", "POST", APPLICATION_USER_SUB, APPLICATION_PASSWORD_SUB, 403);
                c.assertResponse("/test", "POST", APPLICATION_USER_REL, APPLICATION_PASSWORD_REL, 403);
                c.anonymousUnauthorized("/test", "POST");

                deployer.deploy(app2);
                try {
                    c.assertResponse("/foo/bar", "POST", APPLICATION_USER, APPLICATION_PASSWORD, 403);
                    c.assertResponse("/foo/bar", "POST", APPLICATION_USER_SUB, APPLICATION_PASSWORD_SUB, 200);
                    c.anonymousUnauthorized("/foo/bar", "POST");

                    c.assertResponse("/test/sub", "GET", APPLICATION_USER, APPLICATION_PASSWORD, 403);
                    c.assertResponse("/test/sub", "GET", APPLICATION_USER_SUB, APPLICATION_PASSWORD_SUB, 200);
                    c.anonymousUnauthorized("/test/sub", "GET");

                } finally {
                    deployer.undeploy(app2);
                }

                /* Endpoints from app2 must not work after we stopped app2 */
                c.assertResponse("/foo/bar", "GET", APPLICATION_USER, APPLICATION_PASSWORD, 404);
                c.assertResponse("/foo/bar", "GET", APPLICATION_USER_SUB, APPLICATION_PASSWORD_SUB, 404);
                c.assertResponse("/foo/bar", "GET", null, null, 404);

                /* Endpoints from app1 must keep working after we stopped app2 */
                c.assertResponse("/test", "GET", APPLICATION_USER, APPLICATION_PASSWORD, 200);
                c.assertResponse("/test", "GET", APPLICATION_USER_SUB, APPLICATION_PASSWORD_SUB, 403);
                c.anonymousUnauthorized("/test", "GET");

                /* app1 still running, let's deploy app3 that contains a conflicting handler for /test */
                try {
                    deployer.deploy(app3);
                    Assert.fail("Expected "+ DeploymentException.class.getName());
                } catch (Exception e) {
                    /* Funny enough Deployer.deploy() does not declare throwing the checked DeploymentException
                     * catch (DeploymentException e) would thus not compile
                     * but in spite of that DeploymentException is thrown at runtime */
                    if (e instanceof DeploymentException) {
                        /* expected */
                    } else {
                        Assert.fail("Expected "+ DeploymentException.class.getName());
                    }
                }

                /* Nothing may have changed for /test because app3 was not deployed */
                c.assertResponse("/test", "GET", APPLICATION_USER, APPLICATION_PASSWORD, 200);
                c.assertResponse("/test", "GET", APPLICATION_USER_SUB, APPLICATION_PASSWORD_SUB, 403);
                c.anonymousUnauthorized("/test", "GET");

                /* Nothing from app3 may work */
                c.assertResponse("/test", "POST", APPLICATION_USER, APPLICATION_PASSWORD, 405);
                c.assertResponse("/test", "POST", APPLICATION_USER_SUB, APPLICATION_PASSWORD_SUB, 403);
                c.assertResponse("/test", "POST", APPLICATION_USER_REL, APPLICATION_PASSWORD_REL, 403);
                c.anonymousUnauthorized("/test", "POST");

                /* Even the non-conflicting paths of app3 must not work */
                c.assertResponse("/app3", "GET", APPLICATION_USER, APPLICATION_PASSWORD, 404);
                c.assertResponse("/app3", "GET", APPLICATION_USER_SUB, APPLICATION_PASSWORD_SUB, 404);
                c.assertResponse("/app3", "GET", APPLICATION_USER_REL, APPLICATION_PASSWORD_REL, 404);
                c.assertResponse("/app3", "GET", null, null, 404);

            } finally {
                deployer.undeploy(app1);
            }

            /* Endpoints from app1 must not work after app1 was stopped */
            c.assertResponse("/test", "GET", APPLICATION_USER, APPLICATION_PASSWORD, 404);
            c.assertResponse("/test", "GET", APPLICATION_USER_SUB, APPLICATION_PASSWORD_SUB, 404);
            c.assertResponse("/test", "GET", null, null, 404);

        }
    }

}
