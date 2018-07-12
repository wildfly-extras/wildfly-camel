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
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.camel.test.common.security.BasicSecurityDomainASetup;
import org.wildfly.camel.test.common.security.SecurityUtils;
import org.wildfly.camel.test.cxf.ws.secure.subA.GreetingService;
import org.wildfly.camel.test.cxf.ws.secure.subA.GreetingsProcessor;
import org.wildfly.extension.camel.CamelAware;

/**
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 */
@CamelAware
@RunAsClient
@RunWith(Arquillian.class)
@ServerSetup(BasicSecurityDomainASetup.class)
public class CXFWSSpringBasicSecureProducerIntegrationTest {
    static final Path WILDFLY_HOME = Paths.get(System.getProperty("jbossHome"));
    private static final Map<String, String> PATH_ROLE_MAP = new LinkedHashMap<String, String>() {{
        try {
            put("//" + new URI(SecurityUtils.SPRING_CONSUMER_ENDPOINT_ADDRESS).getPath(), BasicSecurityDomainASetup.APPLICATION_ROLE);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }};

    @Deployment
    public static Archive<?> deployment() {
        final WebArchive archive = ShrinkWrap
                .create(WebArchive.class, CXFWSSpringBasicSecureProducerIntegrationTest.class.getSimpleName() + ".war")
                .addClasses(BasicSecurityDomainASetup.class, CXFWSSecureUtils.class, GreetingService.class,
                        GreetingsProcessor.class);
        SecurityUtils.addSpringXml(archive);
        SecurityUtils.enhanceArchive(archive, BasicSecurityDomainASetup.SECURITY_DOMAIN,
                BasicSecurityDomainASetup.AUTH_METHOD, PATH_ROLE_MAP);
        return archive;
    }

    @Test
    public void greetAnonymous() throws Exception {
        CXFWSSecureUtils.assertGreet(WILDFLY_HOME, SecurityUtils.SPRING_CONSUMER_ENDPOINT_ADDRESS, null, null, 401, null);
    }

    @Test
    public void greetBasicGoodUser() throws Exception {
        CXFWSSecureUtils.assertGreet(WILDFLY_HOME, SecurityUtils.SPRING_CONSUMER_ENDPOINT_ADDRESS,
                BasicSecurityDomainASetup.APPLICATION_USER, BasicSecurityDomainASetup.APPLICATION_PASSWORD, 200,
                "Hi Joe");
    }

    @Test
    public void greetBasicBadUser() throws Exception {
        CXFWSSecureUtils.assertGreet(WILDFLY_HOME, SecurityUtils.SPRING_CONSUMER_ENDPOINT_ADDRESS,
                BasicSecurityDomainASetup.APPLICATION_USER_SUB, BasicSecurityDomainASetup.APPLICATION_PASSWORD_SUB, 403,
                null);
    }

}
