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
import org.wildfly.camel.test.cdi.ear.config.named.NamedCamelContextProducer;
import org.wildfly.camel.test.common.utils.TestUtils;
import org.wildfly.extension.camel.CamelAware;
import org.wildfly.extension.camel.CamelContextRegistry;

@CamelAware
@RunWith(Arquillian.class)
public class CDIEarNamedCamelContextTest {

    @ArquillianResource
    private CamelContextRegistry contextRegistry;

    @Deployment
    public static JavaArchive createDeployment() {
        return ShrinkWrap.create(JavaArchive.class, "camel-cdi-ear-named-context-tests.jar")
            .addClass(TestUtils.class);
    }

    @Deployment(testable = false, name = "named-context.ear")
    public static EnterpriseArchive createEarDeployment() {
        return ShrinkWrap.create(EnterpriseArchive.class, "named-context.ear")
            .addAsModule(ShrinkWrap.create(WebArchive.class, "named-context.war")
                .addClass(NamedCamelContextProducer.class)
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml")
            );
    }

    @Test
    public void testCamelCdiEarNamedAnnotatedContext() throws Exception {
        assertContext("emptyNamedFieldContext");
        assertContext("emptyNamedGetterContext");
        assertContext("emptyNamedMethodContext");
        assertContext("named-field-context");
        assertContext("named-getter-context");
        assertContext("emptyNamedBeanContext");
        assertContext("named-bean-context");
    }

    private void assertContext(String name) {
        CamelContext camelctx = contextRegistry.getCamelContext(name);
        Assert.assertNotNull("Expected " + name + " to not be null", camelctx);

        String moduleName = TestUtils.getClassLoaderModuleName(camelctx.getApplicationContextClassLoader());
        Assert.assertEquals("deployment.named-context.ear.named-context.war", moduleName);
    }
}
