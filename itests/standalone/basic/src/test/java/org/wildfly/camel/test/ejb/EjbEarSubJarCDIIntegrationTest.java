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
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.camel.test.ejb.subA.HelloBean;
import org.wildfly.camel.test.ejb.subB.CDICamelContext;
import org.wildfly.extension.camel.CamelAware;
import org.wildfly.extension.camel.CamelContextRegistry;

@CamelAware
@RunWith(Arquillian.class)
public class EjbEarSubJarCDIIntegrationTest {

    static final String SIMPLE_JAR = "camel-ejb-sub-deployment.jar";
    static final String SIMPLE_EAR = "camel-ejb-ear.ear";

    @ArquillianResource
    CamelContextRegistry contextRegistry;

    @Deployment
    public static JavaArchive createDeployment() {
        return ShrinkWrap.create(JavaArchive.class, "camel-ejb-ear-tests");
    }

    @Test
    public void testEjbEarDeployment() throws Exception {
        CamelContext camelctx = contextRegistry.getCamelContext("cdi-ear-context");
        Assert.assertEquals(ServiceStatus.Started, camelctx.getStatus());
        assertContextAccess(camelctx);
    }

    @Test
    public void testEjbEarContextLookup() throws Exception {
        InitialContext inicxt = new InitialContext();
        CamelContext camelctx = (CamelContext) inicxt.lookup("java:jboss/camel/context/cdi-ear-context");
        Assert.assertEquals(ServiceStatus.Started, camelctx.getStatus());
        assertContextAccess(camelctx);
    }

    private void assertContextAccess(CamelContext camelctx) {
        ProducerTemplate producer = camelctx.createProducerTemplate();
        String result = producer.requestBody("direct:start", "Kermit", String.class);
        Assert.assertEquals("Hello Kermit", result);
    }

    @Deployment(name = SIMPLE_EAR, managed = true, testable = false)
    public static EnterpriseArchive createEarDeployment() {
        return ShrinkWrap.create(EnterpriseArchive.class, SIMPLE_EAR)
            .addAsModule(getEjbModule());
    }

    private static JavaArchive getEjbModule() {
        return ShrinkWrap.create(JavaArchive.class, SIMPLE_JAR)
            .addClasses(CDICamelContext.class, HelloBean.class)
            .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml");
    }
}
