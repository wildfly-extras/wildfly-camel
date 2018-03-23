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

import java.net.URL;

import javax.xml.namespace.QName;
import javax.xml.ws.Service;

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
import org.wildfly.camel.test.spring.subE.service.DelayedBinderService;
import org.wildfly.camel.test.spring.subE.service.DelayedBinderServiceActivator;
import org.wildfly.camel.test.spring.subE.servlet.MultipleResourceInjectionServlet;
import org.wildfly.camel.test.spring.subE.ws.WebServiceEndpoint;
import org.wildfly.camel.test.spring.subE.ws.WebServiceEndpointImpl;
import org.wildfly.extension.camel.CamelAware;
import org.wildfly.extension.camel.CamelContextRegistry;

@RunWith(Arquillian.class)
@CamelAware
public class SpringContextBindingJaxWsTest {

    private static final String SIMPLE_WAR = "simple.war";

    @ArquillianResource
    private Deployer deployer;

    @Deployment
    public static JavaArchive createDeployment() {
        return ShrinkWrap.create(JavaArchive.class, "camel-spring-binding-ws-tests.jar")
            .addClasses(WebServiceEndpoint.class);
    }

    @Deployment(managed = false, testable = false, name = SIMPLE_WAR)
    public static WebArchive createWsDeployment() {
        return ShrinkWrap.create(WebArchive.class, SIMPLE_WAR)
            .addClasses(DelayedBinderServiceActivator.class, DelayedBinderService.class,
                MultipleResourceInjectionServlet.class, WebServiceEndpoint.class, WebServiceEndpointImpl.class)
            .addAsResource("spring/jndi-delayed-bindings-camel-context.xml", "camel-context.xml")
            .addAsManifestResource(new StringAsset(DelayedBinderServiceActivator.class.getName()), "services/org.jboss.msc.service.ServiceActivator");
    }

    @ArquillianResource
    private CamelContextRegistry contextRegistry;

    @Test
    public void testJaxWsServiceCamelJndiBindingsInjectable() throws Exception {
        try {
            deployer.deploy(SIMPLE_WAR);

            CamelContext camelctx = contextRegistry.getCamelContext("jndi-delayed-binding-spring-context");
            Assert.assertNotNull("Expected jndi-delayed-binding-spring-context to not be null", camelctx);
            Assert.assertEquals(ServiceStatus.Started, camelctx.getStatus());

            QName qname = new QName("http://wildfly.camel.test.ws", "WebServiceEndpointImplService");
            Service service = Service.create(new URL("http://localhost:8080/simple/WebServiceEndpointImpl?wsdl"), qname);
            WebServiceEndpoint endpoint = service.getPort(WebServiceEndpoint.class);
            Assert.assertNotNull("Endpoint not null", endpoint);
            Assert.assertEquals("camelctxA,camelctxB,camelctxC", endpoint.getContextInjectionStatus());
        } finally {
            deployer.undeploy(SIMPLE_WAR);
        }
    }
}
