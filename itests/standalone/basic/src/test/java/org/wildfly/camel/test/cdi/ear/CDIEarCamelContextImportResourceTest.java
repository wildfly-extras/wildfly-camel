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
package org.wildfly.camel.test.cdi.ear;

import java.io.IOException;

import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.camel.test.cdi.ear.config.imported.XmlRouteBuilder;
import org.wildfly.camel.test.common.utils.TestUtils;
import org.wildfly.extension.camel.CamelAware;
import org.wildfly.extension.camel.CamelContextRegistry;

@CamelAware
@RunWith(Arquillian.class)
public class CDIEarCamelContextImportResourceTest {

    @ArquillianResource
    private CamelContextRegistry contextRegistry;

    @Deployment
    public static JavaArchive createDeployment() {
        return ShrinkWrap.create(JavaArchive.class, "camel-cdi-ear-import-resource-tests.jar")
            .addClass(TestUtils.class);
    }

    @Deployment(testable = false, name = "import-resource.ear")
    public static EnterpriseArchive createEarDeployment() {
        try {
            String contextA = TestUtils.getResourceValue(CDIEarCamelContextImportResourceTest.class, "/cdi/imported-camel-context.xml");
            contextA = contextA.replaceAll("@.*?@", "import-resource-context-a");

            String contextB = TestUtils.getResourceValue(CDIEarCamelContextImportResourceTest.class, "/cdi/imported-camel-context.xml");
            contextB = contextB.replaceAll("@.*?@", "import-resource-context-b");

            return ShrinkWrap.create(EnterpriseArchive.class, "import-resource.ear")
                .addAsModule(ShrinkWrap.create(WebArchive.class, "import-resource-a.war")
                    .addClass(XmlRouteBuilder.class)
                    .addAsResource(new StringAsset(contextA), "classloading/camel-context.xml")
                    .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml")
                )
                .addAsModule(ShrinkWrap.create(WebArchive.class, "import-resource-b.war")
                    .addClass(XmlRouteBuilder.class)
                    .addAsResource(new StringAsset(contextB), "classloading/camel-context.xml")
                    .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml")
                );
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @Test
    public void testCamelCdiEarImportResource() throws Exception {
        CamelContext camelctxA = contextRegistry.getCamelContext("import-resource-context-a");
        Assert.assertNotNull("Expected import-resource-context-a to not be null", camelctxA);

        CamelContext camelctxB = contextRegistry.getCamelContext("import-resource-context-b");
        Assert.assertNotNull("Expected import-resource-context-b to not be null", camelctxB);

        String moduleNameA = TestUtils.getClassLoaderModuleName(camelctxA.getApplicationContextClassLoader());
        Assert.assertEquals("deployment.import-resource.ear.import-resource-a.war", moduleNameA);

        String moduleNameB = TestUtils.getClassLoaderModuleName(camelctxB.getApplicationContextClassLoader());
        Assert.assertEquals("deployment.import-resource.ear.import-resource-b.war", moduleNameB);

        ProducerTemplate template = camelctxA.createProducerTemplate();
        String result = template.requestBody("direct:start", null, String.class);
        Assert.assertEquals("Hello from import-resource-context-a", result);

        template = camelctxB.createProducerTemplate();
        result = template.requestBody("direct:start", null, String.class);
        Assert.assertEquals("Hello from import-resource-context-b", result);
    }
}
