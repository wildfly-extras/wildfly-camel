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

package org.wildfly.camel.test.spring;

import java.net.URL;

import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.ServiceStatus;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleClassLoader;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.modules.ModuleLoader;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.camel.test.spring.subA.HelloJeeBean;
import org.wildfly.extension.camel.CamelContextRegistry;

/**
 * [#319] Unable to locate Spring JEE Namespace Handler
 *
 * https://github.com/wildfly-extras/wildfly-camel/issues/319
 *
 * @author thomas.diesler@jboss.com
 * @since 24-Feb-2015
 */
@RunWith(Arquillian.class)
public class SpringJeeNamespaceTest {

    @ArquillianResource
    CamelContextRegistry contextRegistry;

    @Deployment
    public static JavaArchive createdeployment() {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "camel-jee-tests");
        archive.addAsResource("spring/jee-namespace-camel-context.xml");
        archive.addClasses(HelloJeeBean.class);
        return archive;
    }

    @Test
    public void testLoadHandlersFromSpringContext() throws Exception {
        ModuleLoader moduleLoader = Module.getCallerModuleLoader();
        ModuleIdentifier modid = ModuleIdentifier.create("org.springframework.context");
        ModuleClassLoader classLoader = moduleLoader.loadModule(modid).getClassLoader();
        URL resurl = classLoader.getResource("META-INF/spring.handlers");
        Assert.assertNotNull("URL not null", resurl);
    }

    @Test
    public void testLoadHandlersFromCamel() throws Exception {
        ModuleLoader moduleLoader = Module.getCallerModuleLoader();
        ModuleIdentifier modid = ModuleIdentifier.create("org.apache.camel");
        ModuleClassLoader classLoader = moduleLoader.loadModule(modid).getClassLoader();
        URL resurl = classLoader.getResource("META-INF/spring.handlers");
        Assert.assertNotNull("URL not null", resurl);
    }

    @Test
    public void testLoadHandlersFromDeployment() throws Exception {
        ClassLoader classLoader = getClass().getClassLoader();
        URL resurl = classLoader.getResource("META-INF/spring.handlers");
        Assert.assertNotNull("URL not null", resurl);
    }

    @Test
    public void testJNDILookup() throws Exception {
        CamelContext camelctx = contextRegistry.getCamelContext("spring-jee");
        Assert.assertEquals(ServiceStatus.Started, camelctx.getStatus());
        ProducerTemplate producer = camelctx.createProducerTemplate();
        String result = producer.requestBody("direct:start", "Kermit", String.class);
        Assert.assertTrue("Starts with: Hello Kermit using =>" + result, result.startsWith("Hello Kermit using"));
        Assert.assertTrue("Contains: ConnectionFactory =>" + result, result.contains("ConnectionFactory"));
    }
}
