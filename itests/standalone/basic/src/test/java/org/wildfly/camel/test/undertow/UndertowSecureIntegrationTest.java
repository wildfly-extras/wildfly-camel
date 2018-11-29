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

package org.wildfly.camel.test.undertow;

import static org.wildfly.camel.test.common.security.BasicSecurityDomainASetup.APPLICATION_PASSWORD;
import static org.wildfly.camel.test.common.security.BasicSecurityDomainASetup.APPLICATION_PASSWORD_REL;
import static org.wildfly.camel.test.common.security.BasicSecurityDomainASetup.APPLICATION_PASSWORD_SUB;
import static org.wildfly.camel.test.common.security.BasicSecurityDomainASetup.APPLICATION_ROLE;
import static org.wildfly.camel.test.common.security.BasicSecurityDomainASetup.APPLICATION_ROLE_REL;
import static org.wildfly.camel.test.common.security.BasicSecurityDomainASetup.APPLICATION_ROLE_SUB;
import static org.wildfly.camel.test.common.security.BasicSecurityDomainASetup.APPLICATION_USER;
import static org.wildfly.camel.test.common.security.BasicSecurityDomainASetup.APPLICATION_USER_REL;
import static org.wildfly.camel.test.common.security.BasicSecurityDomainASetup.APPLICATION_USER_SUB;

import java.util.LinkedHashMap;
import java.util.Map;

import org.jboss.arquillian.container.spi.client.container.DeploymentException;
import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.camel.test.common.security.BasicSecurityDomainASetup;
import org.wildfly.camel.test.common.security.SecurityUtils;
import org.wildfly.camel.test.rest.dsl.secure.TestClient;
import org.wildfly.camel.test.undertow.subA.UndertowSecureRoutes1;
import org.wildfly.camel.test.undertow.subA.UndertowSecureRoutes2;
import org.wildfly.camel.test.undertow.subA.UndertowSecureRoutes3;

/**
 * [ENTESB-9342] Support securing Undertow endpoints with Elytron
 *
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 */
@RunAsClient
@RunWith(Arquillian.class)
@ServerSetup(BasicSecurityDomainASetup.class)
public class UndertowSecureIntegrationTest {

    private static final String APP_1 = "UndertowSecureIntegrationTest1.war";
    private static final String APP_2 = "UndertowSecureIntegrationTest2.war";
    private static final String APP_3 = "UndertowSecureIntegrationTest3.war";

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

    @ArquillianResource
    Deployer deployer;

    @Deployment
    public static WebArchive dummy() {
        return ShrinkWrap.create(WebArchive.class, "UndertowSecureIntegrationTest.war");
    }

    @Deployment(name = APP_1, managed = false)
    public static WebArchive app1() {
        return app(APP_1, UndertowSecureRoutes1.class, PATH_ROLE_MAP_1);
    }

    @Deployment(name = APP_2, managed = false)
    public static WebArchive app2() {
        return app(APP_2, UndertowSecureRoutes2.class, PATH_ROLE_MAP_2);
    }

    @Deployment(name = APP_3, managed = false)
    public static WebArchive app3() {
        return app(APP_3, UndertowSecureRoutes3.class, PATH_ROLE_MAP_3);
    }

    @Test
    public void pathConflicts() throws Exception {

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

            deployer.deploy(APP_1);
            try {
                c.assertResponse("/test", "GET", APPLICATION_USER, APPLICATION_PASSWORD, 200);
                c.assertResponse("/test", "GET", APPLICATION_USER_SUB, APPLICATION_PASSWORD_SUB, 403);
                c.anonymousUnauthorized("/test", "GET");

                c.assertResponse("/test", "POST", APPLICATION_USER, APPLICATION_PASSWORD, 405);
                c.assertResponse("/test", "POST", APPLICATION_USER_SUB, APPLICATION_PASSWORD_SUB, 403);
                c.assertResponse("/test", "POST", APPLICATION_USER_REL, APPLICATION_PASSWORD_REL, 403);
                c.anonymousUnauthorized("/test", "POST");

                deployer.deploy(APP_2);
                try {
                    c.assertResponse("/foo/bar", "POST", APPLICATION_USER, APPLICATION_PASSWORD, 403);
                    c.assertResponse("/foo/bar", "POST", APPLICATION_USER_SUB, APPLICATION_PASSWORD_SUB, 200);
                    c.anonymousUnauthorized("/foo/bar", "POST");

                    c.assertResponse("/test/sub", "GET", APPLICATION_USER, APPLICATION_PASSWORD, 403);
                    c.assertResponse("/test/sub", "GET", APPLICATION_USER_SUB, APPLICATION_PASSWORD_SUB, 200);
                    c.anonymousUnauthorized("/test/sub", "GET");

                } finally {
                    deployer.undeploy(APP_2);
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
                    deployer.deploy(APP_3);
                    Assert.fail("Expected " + DeploymentException.class.getName());
                } catch (Exception e) {
                    /*
                     * Funny enough Deployer.deploy() does not declare throwing the checked DeploymentException catch
                     * (DeploymentException e) would thus not compile but in spite of that DeploymentException is thrown
                     * at runtime
                     */
                    if (!(e instanceof DeploymentException)) {
                        Assert.fail("Expected " + DeploymentException.class.getName());
                    }
                } finally {
                    deployer.undeploy(APP_3);
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
                deployer.undeploy(APP_1);
            }

            /* Endpoints from app1 must not work after app1 was stopped */
            c.assertResponse("/test", "GET", APPLICATION_USER, APPLICATION_PASSWORD, 404);
            c.assertResponse("/test", "GET", APPLICATION_USER_SUB, APPLICATION_PASSWORD_SUB, 404);
            c.assertResponse("/test", "GET", null, null, 404);
        }
    }

    private static WebArchive app(String war, Class<?> routeBuilder, Map<String, String> pathRoleMap) {
        final WebArchive archive = ShrinkWrap.create(WebArchive.class, war)
                .addClasses(BasicSecurityDomainASetup.class, TestClient.class, routeBuilder)
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
        SecurityUtils.enhanceArchive(archive, BasicSecurityDomainASetup.SECURITY_DOMAIN,
                BasicSecurityDomainASetup.AUTH_METHOD, pathRoleMap);
        return archive;
    }
}
