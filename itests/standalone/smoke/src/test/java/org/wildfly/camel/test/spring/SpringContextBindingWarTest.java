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

import org.apache.camel.CamelContext;
import org.apache.camel.ServiceStatus;
import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.camel.test.common.http.HttpRequest;
import org.wildfly.camel.test.common.http.HttpRequest.HttpResponse;
import org.wildfly.camel.test.spring.subE.service.DelayedBinderService;
import org.wildfly.camel.test.spring.subE.service.DelayedBinderServiceActivator;
import org.wildfly.camel.test.spring.subE.servlet.MultipleResourceInjectionServlet;
import org.wildfly.camel.test.spring.subE.servlet.SingleResourceInjectionServlet;
import org.wildfly.extension.camel.CamelAware;
import org.wildfly.extension.camel.CamelContextRegistry;

@RunWith(Arquillian.class)
@CamelAware
public class SpringContextBindingWarTest {

    private static final String SIMPLE_WAR_A = "simple-a.war";
    private static final String SIMPLE_WAR_B = "simple-b.war";

    @ArquillianResource
    private Deployer deployer;

    @Deployment
    public static JavaArchive createDeployment() {
        return ShrinkWrap.create(JavaArchive.class, "camel-spring-binding-tests.jar")
            .addClasses(HttpRequest.class);
    }

    @Deployment(managed = false, testable = false, name = SIMPLE_WAR_A)
    public static WebArchive createMultipleResourceInjectionPointDeployment() {
        return ShrinkWrap.create(WebArchive.class, SIMPLE_WAR_A)
            .addClasses(DelayedBinderServiceActivator.class, DelayedBinderService.class, MultipleResourceInjectionServlet.class)
            .addAsResource("spring/jndi-delayed-bindings-camel-context.xml", "camel-context.xml")
            .addAsManifestResource(new StringAsset(DelayedBinderServiceActivator.class.getName()), "services/org.jboss.msc.service.ServiceActivator");
    }

    @Deployment(managed = false, testable = false, name = SIMPLE_WAR_B)
    public static WebArchive createSingleResourceInjectionPointDeployment() {
        return ShrinkWrap.create(WebArchive.class, SIMPLE_WAR_B)
            .addClasses(DelayedBinderServiceActivator.class, DelayedBinderService.class, SingleResourceInjectionServlet.class)
            .addAsResource("spring/jndi-delayed-bindings-camel-context.xml", "camel-context.xml")
            .addAsManifestResource(new StringAsset(DelayedBinderServiceActivator.class.getName()), "services/org.jboss.msc.service.ServiceActivator");
    }

    @ArquillianResource
    private CamelContextRegistry contextRegistry;

    @Test
    public void testMultipleCamelJndiBindingsInjectable() throws Exception {
        deployResourceInjectionTest(SIMPLE_WAR_A, "camelctxA,camelctxB,camelctxC");
    }

    @Test
    public void testSingleCamelJndiBindingsInjectable() throws Exception {
        deployResourceInjectionTest(SIMPLE_WAR_B, "camelctx");
    }

    private void deployResourceInjectionTest(String depName, String expectedResult) throws Exception {
        try {
            deployer.deploy(depName);

            CamelContext camelctx = contextRegistry.getCamelContext("jndi-delayed-binding-spring-context");
            Assert.assertNotNull("Expected jndi-delayed-binding-spring-context to not be null", camelctx);
            Assert.assertEquals(ServiceStatus.Started, camelctx.getStatus());

            String contextPath = depName.replace(".war", "");

            HttpResponse response = HttpRequest.get("http://localhost:8080/" + contextPath).getResponse();
            Assert.assertEquals(expectedResult, response.getBody());
        } finally {
            deployer.undeploy(depName);
        }
    }
}
