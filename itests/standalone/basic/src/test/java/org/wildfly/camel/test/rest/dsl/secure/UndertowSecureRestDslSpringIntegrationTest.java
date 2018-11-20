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

import java.io.File;
import java.util.Map;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
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

/**
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 */
@RunAsClient
@RunWith(Arquillian.class)
@ServerSetup(BasicSecurityDomainASetup.class)
public class UndertowSecureRestDslSpringIntegrationTest extends AbstractUndertowSecureRestDslClientTest {
    private static final String APP_1 = "UndertowSecureRestDslSpringIntegrationTest1.war";
    private static final String APP_2 = "UndertowSecureRestDslSpringIntegrationTest2.war";
    private static final String APP_3 = "UndertowSecureRestDslSpringIntegrationTest3.war";
    private static final String APP_4 = "UndertowSecureRestDslSpringIntegrationTest4.war";

    private static WebArchive app(String war, String springContextXml, Map<String, String> pathRoleMap) {
        final WebArchive archive = ShrinkWrap.create(WebArchive.class, war).addClasses(BasicSecurityDomainASetup.class,
                CXFRSSecureUtils.class, TestClient.class, AbstractUndertowSecureRestDslClientTest.class);
        SecurityUtils.enhanceArchive(archive, BasicSecurityDomainASetup.SECURITY_DOMAIN,
                BasicSecurityDomainASetup.AUTH_METHOD, pathRoleMap);
        archive.addAsWebInfResource("rest/" + springContextXml, springContextXml);
        return archive;
    }

    @Deployment(name = APP_1, managed = false)
    public static WebArchive app1() {
        return app(APP_1, "secure-rest1-camel-context.xml", PATH_ROLE_MAP_1);
    }

    @Deployment(name = APP_2, managed = false)
    public static WebArchive app2() {
        return app(APP_2, "secure-rest2-camel-context.xml", PATH_ROLE_MAP_2);
    }

    @Deployment(name = APP_3, managed = false)
    public static WebArchive app3() {
        return app(APP_3, "secure-rest3-camel-context.xml", PATH_ROLE_MAP_3);
    }

    @Deployment(name = APP_4, managed = false)
    public static WebArchive app4() {
        return app(APP_4, "secure-rest4-camel-context.xml", PATH_ROLE_MAP_4);
    }

    @Deployment
    public static WebArchive dummy() {
        return ShrinkWrap.create(WebArchive.class, "UndertowSecureRestDslSpringIntegrationTest.war");
    }

    @Test
    public void pathConflicts() throws Exception {
        pathConflicts(APP_1, APP_2, APP_3);
    }

    @Test
    public void swagger() throws Exception {
        swagger(APP_4);
    }


}
