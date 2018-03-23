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
package org.wildfly.camel.test.spring;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.camel.CamelContext;
import org.apache.camel.ServiceStatus;
import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.camel.test.spring.subE.ejb.CamelSpringContextReporterEjb;
import org.wildfly.camel.test.spring.subE.service.DelayedBinderService;
import org.wildfly.camel.test.spring.subE.service.DelayedBinderServiceActivator;
import org.wildfly.camel.test.spring.subE.servlet.MultipleResourceInjectionServlet;
import org.wildfly.extension.camel.CamelAware;
import org.wildfly.extension.camel.CamelContextRegistry;

@RunWith(Arquillian.class)
@CamelAware
public class SpringContextBindingJarTest {

    private static final String SIMPLE_JAR = "simple.jar";

    @ArquillianResource
    private Deployer deployer;

    @Deployment
    public static JavaArchive createDeployment() {
        return ShrinkWrap.create(JavaArchive.class, "camel-spring-binding-tests.jar");
    }

    @Deployment(managed = false, testable = false, name = SIMPLE_JAR)
    public static JavaArchive createEjbDeployment() {
        return ShrinkWrap.create(JavaArchive.class, SIMPLE_JAR)
            .addClasses(DelayedBinderServiceActivator.class, DelayedBinderService.class,
                MultipleResourceInjectionServlet.class, CamelSpringContextReporterEjb.class)
            .addAsResource("spring/jndi-delayed-bindings-camel-context.xml", "camel-context.xml")
            .addAsManifestResource(new StringAsset(DelayedBinderServiceActivator.class.getName()), "services/org.jboss.msc.service.ServiceActivator");
    }

    @ArquillianResource
    private CamelContextRegistry contextRegistry;

    @Test
    public void testEjbCamelJndiBindingsInjectable() throws Exception {
        try {
            deployer.deploy(SIMPLE_JAR);

            CamelContext camelctx = contextRegistry.getCamelContext("jndi-delayed-binding-spring-context");
            Assert.assertNotNull("Expected jndi-delayed-binding-spring-context to not be null", camelctx);
            Assert.assertEquals(ServiceStatus.Started, camelctx.getStatus());

            String dataDir = System.getProperty("jboss.server.data.dir");
            Path filePath = Paths.get(dataDir, "camel-context-status.txt");

            String result = new String(Files.readAllBytes(filePath));
            Assert.assertEquals("camelctxA,camelctxB,camelctxC", result);
        } finally {
            deployer.undeploy(SIMPLE_JAR);
        }
    }
}
