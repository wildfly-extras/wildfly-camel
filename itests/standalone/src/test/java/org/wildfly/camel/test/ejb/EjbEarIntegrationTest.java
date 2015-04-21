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

package org.wildfly.camel.test.ejb;

import javax.naming.InitialContext;

import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.ServiceStatus;
import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.camel.test.ejb.subA.HelloBean;
import org.wildfly.extension.camel.CamelContextRegistry;

@RunWith(Arquillian.class)
public class EjbEarIntegrationTest {

    static final String SIMPLE_JAR = "camel-ejb-jar.jar";
    static final String SIMPLE_EAR = "camel-ejb-ear.ear";

    @ArquillianResource
    Deployer deployer;

    @ArquillianResource
    CamelContextRegistry contextRegistry;

    @Deployment
    public static JavaArchive createdeployment() {
        return ShrinkWrap.create(JavaArchive.class, "camel-ejb-ear-tests");
    }

    @Test
    public void testEjbJarDeployment() throws Exception {
        CamelContext camelctx = contextRegistry.getContext("ejb-jar-context");
        Assert.assertEquals(ServiceStatus.Started, camelctx.getStatus());
        assertContextAccess(camelctx);
    }

    @Test
    public void testEjbJarContextLookup() throws Exception {
        InitialContext inicxt = new InitialContext();
        CamelContext camelctx = (CamelContext) inicxt.lookup("java:jboss/camel/context/ejb-jar-context");
        Assert.assertEquals(ServiceStatus.Started, camelctx.getStatus());
        assertContextAccess(camelctx);
    }

    @Test
    public void testEjbEarDeployment() throws Exception {
        CamelContext camelctx = contextRegistry.getContext("ejb-ear-context");
        Assert.assertEquals(ServiceStatus.Started, camelctx.getStatus());
        assertContextAccess(camelctx);
    }

    @Test
    public void testEjbEarContextLookup() throws Exception {
        InitialContext inicxt = new InitialContext();
        CamelContext camelctx = (CamelContext) inicxt.lookup("java:jboss/camel/context/ejb-ear-context");
        Assert.assertEquals(ServiceStatus.Started, camelctx.getStatus());
        assertContextAccess(camelctx);
    }

    private void assertContextAccess(CamelContext camelctx) {
        ProducerTemplate producer = camelctx.createProducerTemplate();
        String result = producer.requestBody("direct:start", "Kermit", String.class);
        Assert.assertEquals("Hello Kermit", result);
    }

    @Deployment(name = SIMPLE_JAR, managed = true, testable = false)
    public static JavaArchive createJarDeployment() {
        return getEjbModule("ejb/ejb-jar-camel-context.xml");
    }

    @Deployment(name = SIMPLE_EAR, managed = true, testable = false)
    public static EnterpriseArchive createEarDeployment() {
        EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, SIMPLE_EAR);
        ear.addAsModule(getEjbModule("ejb/ejb-ear-camel-context.xml"));
        return ear;
    }

    private static JavaArchive getEjbModule(String descriptorName) {
        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, SIMPLE_JAR);
        jar.addAsManifestResource(descriptorName, "ejb-camel-context.xml");
        jar.addClasses(HelloBean.class);
        return jar;
    }
}
