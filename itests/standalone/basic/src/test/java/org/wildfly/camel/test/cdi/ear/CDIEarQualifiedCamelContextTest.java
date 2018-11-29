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

import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.camel.test.cdi.ear.config.qualifier.WildFlyCamelContextQualifier;
import org.wildfly.camel.test.common.utils.TestUtils;
import org.wildfly.extension.camel.CamelAware;
import org.wildfly.extension.camel.CamelContextRegistry;

@CamelAware
@RunWith(Arquillian.class)
public class CDIEarQualifiedCamelContextTest {

    @ArquillianResource
    private CamelContextRegistry contextRegistry;

    @Deployment
    public static JavaArchive createDeployment() {
        return ShrinkWrap.create(JavaArchive.class, "camel-cdi-ear-qualified-context-tests.jar")
            .addClass(TestUtils.class);
    }

    @Deployment(testable = false, name = "qualified-context.ear")
    public static EnterpriseArchive createEarDeployment() {
        return ShrinkWrap.create(EnterpriseArchive.class, "qualified-context.ear")
            .addAsModule(ShrinkWrap.create(WebArchive.class, "qualified-context.war")
                .addPackage(WildFlyCamelContextQualifier.class.getPackage())
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml")
            );
    }

    @Test
    public void testCamelCdiEarQualifiedContextCreation() throws Exception {
        CamelContext camelctx = contextRegistry.getCamelContext("qualified-context");
        Assert.assertNotNull("Expected qualified-context to not be null", camelctx);

        String moduleName = TestUtils.getClassLoaderModuleName(camelctx.getApplicationContextClassLoader());
        Assert.assertEquals("deployment.qualified-context.ear.qualified-context.war", moduleName);

        ProducerTemplate template = camelctx.createProducerTemplate();
        String result = template.requestBody("direct:start", null, String.class);
        Assert.assertEquals("Hello Kermit", result);
    }
}
