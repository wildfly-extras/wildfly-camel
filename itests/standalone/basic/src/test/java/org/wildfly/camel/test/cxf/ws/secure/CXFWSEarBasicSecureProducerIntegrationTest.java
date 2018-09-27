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
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.camel.test.common.security.BasicSecurityDomainASetup;
import org.wildfly.camel.test.common.security.BasicSecurityDomainBSetup;
import org.wildfly.camel.test.common.security.SecurityUtils;
import org.wildfly.camel.test.common.utils.EnvironmentUtils;
import org.wildfly.camel.test.cxf.ws.secure.subA.Application;
import org.wildfly.camel.test.cxf.ws.secure.subA.GreetingService;
import org.wildfly.camel.test.cxf.ws.secure.subA.GreetingsProcessor;
import org.wildfly.extension.camel.CamelAware;

/**
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 */
@CamelAware
@RunAsClient
@RunWith(Arquillian.class)
@ServerSetup({BasicSecurityDomainASetup.class, BasicSecurityDomainBSetup.class})
public class CXFWSEarBasicSecureProducerIntegrationTest {

    private static final Path WILDFLY_HOME = EnvironmentUtils.getWildFlyHome();
    private static final String WS_MESSAGE_TEMPLATE_B = "<Envelope xmlns=\"http://schemas.xmlsoap.org/soap/envelope/\">"
            + "<Body>"
            + "<greet xmlns=\"http://subB.secure.ws.cxf.test.camel.wildfly.org/\">"
            + "<message xmlns=\"\">%s</message>"
            + "<name xmlns=\"\">%s</name>"
            + "</greet>"
            + "</Body>"
            + "</Envelope>";

    private static final Map<String, String> PATH_ROLE_MAP_A = new LinkedHashMap<String, String>() {
        private static final long serialVersionUID = 1L;
        {
            try {
                put("//" + new URI(Application.CXF_ENDPOINT_URI).getPath(),
                        BasicSecurityDomainASetup.APPLICATION_ROLE);
                put("//" + new URI(Application.CXF_ENDPOINT_SUB_URI).getPath(),
                        BasicSecurityDomainASetup.APPLICATION_ROLE_SUB);
            } catch (URISyntaxException e) {
                throw new RuntimeException(e);
            }
        }
    };

    private static final Map<String, String> PATH_ROLE_MAP_B = new LinkedHashMap<String, String>() {
        private static final long serialVersionUID = 1L;
        {
        try {
            put("//" + new URI(CXFWSSecureUtils.SPRING_CONSUMER_ENDPOINT_ADDRESS).getPath(), BasicSecurityDomainBSetup.APPLICATION_ROLE);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }};

    @Deployment
    public static Archive<?> deployment() {
        final JavaArchive jar = ShrinkWrap
                .create(JavaArchive.class, CXFWSEarBasicSecureProducerIntegrationTest.class.getSimpleName() + ".jar")
                .addClasses(BasicSecurityDomainASetup.class, CXFWSSecureUtils.class, EnvironmentUtils.class)
        ;
        final WebArchive warA = ShrinkWrap
                .create(WebArchive.class, CXFWSEarBasicSecureProducerIntegrationTest.class.getSimpleName() + "-a.war")
                .addPackage(Application.class.getPackage())
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml")
        ;
        SecurityUtils.enhanceArchive(warA, BasicSecurityDomainASetup.SECURITY_DOMAIN,
                BasicSecurityDomainASetup.AUTH_METHOD, PATH_ROLE_MAP_A);

        final WebArchive warB = ShrinkWrap
                .create(WebArchive.class, CXFWSSpringBasicSecureProducerIntegrationTest.class.getSimpleName() + ".war")
                .addAsWebInfResource("cxf/secure/spring/cxfws-camel-context.xml")
                .addClasses(BasicSecurityDomainBSetup.class, CXFWSSecureUtils.class, GreetingService.class,
                        GreetingsProcessor.class);
        SecurityUtils.addSpringXmlWs(warB, CXFWSSecureUtils.SPRING_CONSUMER_ENDPOINT_ADDRESS);
        SecurityUtils.enhanceArchive(warB, BasicSecurityDomainBSetup.SECURITY_DOMAIN,
                BasicSecurityDomainBSetup.AUTH_METHOD, PATH_ROLE_MAP_B);


        final EnterpriseArchive ear = ShrinkWrap
                .create(EnterpriseArchive.class, CXFWSEarBasicSecureProducerIntegrationTest.class.getSimpleName() + ".ear")
                .addAsLibrary(jar)
                .addAsModule(warA)
                .addAsModule(warB)
        ;
        return ear;
    }

    @Test
    public void greetAAnonymous() throws Exception {
        CXFWSSecureUtils.assertGreet(WILDFLY_HOME, Application.CXF_ENDPOINT_URI, null, null, 401, null);
    }

    @Test
    public void greetAAnonymousSub() throws Exception {
        CXFWSSecureUtils.assertGreet(WILDFLY_HOME, Application.CXF_ENDPOINT_SUB_URI, null, null, 401,
                null);
    }

    @Test
    public void greetABasicBadUser() throws Exception {
        CXFWSSecureUtils.assertGreet(WILDFLY_HOME, Application.CXF_ENDPOINT_URI,
                BasicSecurityDomainASetup.APPLICATION_USER_SUB, BasicSecurityDomainASetup.APPLICATION_PASSWORD_SUB, 403,
                null);
    }

    @Test
    public void greetABasicUserFromB() throws Exception {
        CXFWSSecureUtils.assertGreet(WILDFLY_HOME, Application.CXF_ENDPOINT_URI,
                BasicSecurityDomainBSetup.APPLICATION_USER, BasicSecurityDomainBSetup.APPLICATION_PASSWORD, 401,
                null);
    }

    @Test
    public void greetABasicGoodUser() throws Exception {
        CXFWSSecureUtils.assertGreet(WILDFLY_HOME, Application.CXF_ENDPOINT_URI,
                BasicSecurityDomainASetup.APPLICATION_USER, BasicSecurityDomainASetup.APPLICATION_PASSWORD, 200,
                "Hi Joe");
    }

    @Test
    public void greetABasicSubBadUser() throws Exception {
        CXFWSSecureUtils.assertGreet(WILDFLY_HOME, Application.CXF_ENDPOINT_SUB_URI,
                BasicSecurityDomainASetup.APPLICATION_USER, BasicSecurityDomainASetup.APPLICATION_PASSWORD, 403, null);
    }

    @Test
    public void greetABasicSubGoodUser() throws Exception {
        CXFWSSecureUtils.assertGreet(WILDFLY_HOME, Application.CXF_ENDPOINT_SUB_URI,
                BasicSecurityDomainASetup.APPLICATION_USER_SUB, BasicSecurityDomainASetup.APPLICATION_PASSWORD_SUB, 200,
                "Hi Joe");
    }


    @Test
    public void greetBAnonymous() throws Exception {
        CXFWSSecureUtils.assertGreet(WILDFLY_HOME, CXFWSSecureUtils.SPRING_CONSUMER_ENDPOINT_ADDRESS, null, null, 401, null);
    }

    @Test
    public void greetBBasicGoodUser() throws Exception {
        CXFWSSecureUtils.assertGreet(WILDFLY_HOME, CXFWSSecureUtils.SPRING_CONSUMER_ENDPOINT_ADDRESS,
                BasicSecurityDomainBSetup.APPLICATION_USER, BasicSecurityDomainBSetup.APPLICATION_PASSWORD, 200,
                "Hi Joe");
    }

    @Test
    public void greetBBasicBadUser() throws Exception {
        CXFWSSecureUtils.assertGreet(WILDFLY_HOME, CXFWSSecureUtils.SPRING_CONSUMER_ENDPOINT_ADDRESS,
                BasicSecurityDomainBSetup.APPLICATION_USER_SUB, BasicSecurityDomainBSetup.APPLICATION_PASSWORD_SUB, 403,
                null);
    }

    public void greetBBasicUserFromA() throws Exception {
        CXFWSSecureUtils.assertGreet(WILDFLY_HOME, CXFWSSecureUtils.SPRING_CONSUMER_ENDPOINT_ADDRESS,
                BasicSecurityDomainASetup.APPLICATION_USER, BasicSecurityDomainASetup.APPLICATION_PASSWORD, 403,
                null);
    }
}
