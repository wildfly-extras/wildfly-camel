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
package org.wildfly.camel.test.cxf.ws.secure;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Map;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.camel.test.common.security.BasicSecurityDomainASetup;
import org.wildfly.camel.test.common.security.SecurityUtils;
import org.wildfly.camel.test.common.utils.EnvironmentUtils;
import org.wildfly.camel.test.cxf.ws.secure.subA.Application;
import org.wildfly.camel.test.cxf.ws.secure.subA.CxfWsRouteBuilder;
import org.wildfly.extension.camel.CamelAware;

/**
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 */
@CamelAware
@RunAsClient
@RunWith(Arquillian.class)
@ServerSetup(BasicSecurityDomainASetup.class)
public class CXFWSBasicSecureProducerIntegrationTest {

    public static final String APP_NAME = "CXFWSBasicSecureProducerIntegrationTest";
    private static final Path WILDFLY_HOME = EnvironmentUtils.getWildFlyHome();

    private static final Map<String, String> PATH_ROLE_MAP = new LinkedHashMap<String, String>() {
        private static final long serialVersionUID = 1L;
        {
            try {
                put("//" + new URI(Application.CXF_ENDPOINT_URI).getPath(),
                        BasicSecurityDomainASetup.APPLICATION_ROLE);
                put("//" + new URI(Application.CXF_ENDPOINT_SUB_URI).getPath(),
                        BasicSecurityDomainASetup.APPLICATION_ROLE_SUB);
                put(new URI(Application.CXF_ENDPOINT_REL_URI).getPath().substring(("/"+APP_NAME).length()),
                        BasicSecurityDomainASetup.APPLICATION_ROLE_REL);
            } catch (URISyntaxException e) {
                throw new RuntimeException(e);
            }
        }
    };

    @Deployment
    public static Archive<?> deployment() {
        final WebArchive archive = ShrinkWrap
                .create(WebArchive.class, APP_NAME + ".war")
                .addClasses(BasicSecurityDomainASetup.class, CXFWSSecureUtils.class)
                .addPackage(CxfWsRouteBuilder.class.getPackage())
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml")
        ;
        SecurityUtils.enhanceArchive(archive, BasicSecurityDomainASetup.SECURITY_DOMAIN,
                BasicSecurityDomainASetup.AUTH_METHOD, PATH_ROLE_MAP);
        return archive;
    }

    @Test
    public void greetAnonymous() throws Exception {
        CXFWSSecureUtils.assertGreet(WILDFLY_HOME, Application.CXF_ENDPOINT_URI, null, null, 401, null);
    }

    @Test
    public void greetAnonymousSub() throws Exception {
        CXFWSSecureUtils.assertGreet(WILDFLY_HOME, Application.CXF_ENDPOINT_SUB_URI, null, null, 401,
                null);
    }

    @Test
    public void greetBasicBadUser() throws Exception {
        CXFWSSecureUtils.assertGreet(WILDFLY_HOME, Application.CXF_ENDPOINT_URI,
                BasicSecurityDomainASetup.APPLICATION_USER_SUB, BasicSecurityDomainASetup.APPLICATION_PASSWORD_SUB, 403,
                null);
    }

    @Test
    public void greetBasicGoodUser() throws Exception {
        CXFWSSecureUtils.assertGreet(WILDFLY_HOME, Application.CXF_ENDPOINT_URI,
                BasicSecurityDomainASetup.APPLICATION_USER, BasicSecurityDomainASetup.APPLICATION_PASSWORD, 200,
                "Hi Joe");
    }

    @Test
    public void greetBasicSubBadUser() throws Exception {
        CXFWSSecureUtils.assertGreet(WILDFLY_HOME, Application.CXF_ENDPOINT_SUB_URI,
                BasicSecurityDomainASetup.APPLICATION_USER, BasicSecurityDomainASetup.APPLICATION_PASSWORD, 403, null);
    }

    @Test
    public void greetBasicSubGoodUser() throws Exception {
        CXFWSSecureUtils.assertGreet(WILDFLY_HOME, Application.CXF_ENDPOINT_SUB_URI,
                BasicSecurityDomainASetup.APPLICATION_USER_SUB, BasicSecurityDomainASetup.APPLICATION_PASSWORD_SUB, 200,
                "Hi Joe");
    }

    @Test
    public void greetBasicRelBadUser() throws Exception {
        CXFWSSecureUtils.assertGreet(WILDFLY_HOME, Application.CXF_ENDPOINT_REL_URI,
                BasicSecurityDomainASetup.APPLICATION_USER_SUB, BasicSecurityDomainASetup.APPLICATION_PASSWORD_SUB, 403, null);
    }

    @Test
    public void greetBasicRelGoodUser() throws Exception {
        CXFWSSecureUtils.assertGreet(WILDFLY_HOME, Application.CXF_ENDPOINT_REL_URI,
                BasicSecurityDomainASetup.APPLICATION_USER_REL, BasicSecurityDomainASetup.APPLICATION_PASSWORD_REL, 200,
                "Hi Joe");
    }
}
