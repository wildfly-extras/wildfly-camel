/*
 * #%L
 * Wildfly Camel :: Testsuite
 * %%
 * Copyright (C) 2013 - 2015 RedHat
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
package org.wildfly.camel.test.cdi;


import java.lang.management.ManagementFactory;
import java.util.Set;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.apache.camel.CamelContext;
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
import org.wildfly.camel.test.cdi.subB.Bootstrap;
import org.wildfly.camel.test.cdi.subB.HelloBean;
import org.wildfly.extension.camel.CamelAware;
import org.wildfly.extension.camel.CamelContextRegistry;

@CamelAware
@RunWith(Arquillian.class)
public class CDIEarIntegrationTest {

    private static final String SIMPLE_JAR = "camel-ejb-jar.jar";
    private static final String SIMPLE_EAR = "camel-ejb-ear.ear";

    @ArquillianResource
    CamelContextRegistry contextRegistry;

    @Deployment
    public static JavaArchive createdeployment() {
        return ShrinkWrap.create(JavaArchive.class, "camel-ejb-ear-tests");
    }

    @Test
    public void testEjbJarDeployment() throws Exception {
        // We don't actually know what the camel context name will be, so defer to JMX lookup
        MBeanServer server = ManagementFactory.getPlatformMBeanServer();
        Set<ObjectName> camelContextNames = server.queryNames(new ObjectName("org.apache.camel:context=*,type=context,*"), null);
        Assert.assertEquals(1, camelContextNames.size());

        ObjectName next = camelContextNames.iterator().next();
        String camelContextName = next.getKeyProperty("name").replace("\"", "");

        CamelContext camelctx = contextRegistry.getCamelContext(camelContextName);
        Assert.assertNotNull(camelctx);

        String result = camelctx.createProducerTemplate().requestBody("direct:start", "Kermit", String.class);
        Assert.assertEquals("Hello Kermit", result);
    }

    @Deployment(name = SIMPLE_EAR, managed = true, testable = false)
    public static EnterpriseArchive createEarDeployment() {
        return ShrinkWrap.create(EnterpriseArchive.class, SIMPLE_EAR)
            .addAsModule(
                ShrinkWrap.create(JavaArchive.class, SIMPLE_JAR)
                    .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml")
                    .addClasses(Bootstrap.class, HelloBean.class)
            );
    }
}
