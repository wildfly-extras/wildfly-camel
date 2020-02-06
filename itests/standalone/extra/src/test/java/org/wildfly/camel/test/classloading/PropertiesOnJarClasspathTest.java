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

package org.wildfly.camel.test.classloading;

import java.net.URL;
import java.util.Properties;

import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.extension.camel.CamelContextRegistry;

@RunWith(Arquillian.class)
public class PropertiesOnJarClasspathTest {

    @ArquillianResource
    private CamelContextRegistry camelContextRegistry;

    @Deployment
    public static JavaArchive createDeployment() {
        JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "camel-global-modules.jar");
        archive.addAsResource("modules/jboss-camel-context.xml", "jboss-camel-context.xml");
        archive.addAsResource("modules/psetA.properties", "psetAA.properties");
        archive.addAsResource("modules/psetB.properties", "psetBA.properties");
        return archive;
    }

    @Test
    public void testClassLoaderAccess() throws Exception {

        Properties props = new Properties();
        ClassLoader loader = getClass().getClassLoader();
        URL propsA = loader.getResource("/psetAA.properties");
        URL propsB = loader.getResource("/psetBA.properties");
        Assert.assertNotNull("propsA not null", propsA);
        Assert.assertNotNull("propsB not null", propsB);

        System.out.println("Found: " + propsA);
        System.out.println("Found: " + propsB);
        props.load(propsA.openStream());
        props.load(propsB.openStream());

        Assert.assertEquals("valA", props.get("keyA"));
        Assert.assertEquals("valB", props.get("keyB"));
    }

    @Test
    public void testCamelRouteAccess() throws Exception {

        CamelContext camelctx = camelContextRegistry.getCamelContext("spring-context");
        Assert.assertNotNull("Expected spring-context to not be null", camelctx);

        ProducerTemplate template = camelctx.createProducerTemplate();
        String res = template.requestBody("direct:start", null, String.class);
        Assert.assertEquals("Hello valA valB", res);
    }
}
