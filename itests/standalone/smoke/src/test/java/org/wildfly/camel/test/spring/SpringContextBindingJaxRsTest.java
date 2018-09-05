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
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.camel.test.common.http.HttpRequest;
import org.wildfly.camel.test.common.http.HttpRequest.HttpResponse;
import org.wildfly.camel.test.spring.subE.rs.RestApplication;
import org.wildfly.camel.test.spring.subE.rs.RestService;
import org.wildfly.camel.test.spring.subE.service.DelayedBinderService;
import org.wildfly.camel.test.spring.subE.service.DelayedBinderServiceActivator;
import org.wildfly.camel.test.spring.subE.servlet.MultipleResourceInjectionServlet;
import org.wildfly.extension.camel.CamelAware;
import org.wildfly.extension.camel.CamelContextRegistry;

@org.junit.Ignore("https://github.com/wildfly-extras/wildfly-camel/issues/2601")
@RunWith(Arquillian.class)
@CamelAware
public class SpringContextBindingJaxRsTest {

    private static final String SIMPLE_WAR = "simple.war";

    @ArquillianResource
    private Deployer deployer;

    @Deployment
    public static JavaArchive createDeployment() {
        return ShrinkWrap.create(JavaArchive.class, "camel-spring-binding-rs-tests.jar")
            .addClasses(HttpRequest.class);
    }

    @Deployment(managed = false, testable = false, name = SIMPLE_WAR)
    public static WebArchive createRsDeployment() {
        return ShrinkWrap.create(WebArchive.class, SIMPLE_WAR)
            .addClasses(DelayedBinderServiceActivator.class, DelayedBinderService.class,
                MultipleResourceInjectionServlet.class, RestApplication.class, RestService.class)
            .addAsResource("spring/jndi-delayed-bindings-camel-context.xml", "camel-context.xml")
            .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml")
            .addAsManifestResource(new StringAsset(DelayedBinderServiceActivator.class.getName()), "services/org.jboss.msc.service.ServiceActivator");
    }

    @ArquillianResource
    private CamelContextRegistry contextRegistry;

    @Test
    public void testJaxRsServiceCamelJndiBindingsInjectable() throws Exception {
        try {
            deployer.deploy(SIMPLE_WAR);

            CamelContext camelctx = contextRegistry.getCamelContext("jndi-delayed-binding-spring-context");
            Assert.assertNotNull("Expected jndi-delayed-binding-spring-context to not be null", camelctx);
            Assert.assertEquals(ServiceStatus.Started, camelctx.getStatus());

            HttpResponse response = HttpRequest.get("http://localhost:8080/simple/rest/context").getResponse();
            Assert.assertEquals("camelctxA,camelctxB,camelctxC", response.getBody());
        } finally {
            deployer.undeploy(SIMPLE_WAR);
        }
    }
}
