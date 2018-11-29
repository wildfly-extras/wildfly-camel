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
import org.wildfly.camel.test.cdi.ear.config.producer.method.CamelContextProducerA;
import org.wildfly.camel.test.cdi.ear.config.producer.method.CamelContextProducerB;
import org.wildfly.camel.test.common.utils.TestUtils;
import org.wildfly.extension.camel.CamelAware;
import org.wildfly.extension.camel.CamelContextRegistry;

@CamelAware
@RunWith(Arquillian.class)
public class CDIEarCamelContextProducerMethodTest {

    @ArquillianResource
    private CamelContextRegistry contextRegistry;

    @Deployment
    public static JavaArchive createDeployment() {
        return ShrinkWrap.create(JavaArchive.class, "camel-cdi-ear-producer-method-tests.jar")
            .addClass(TestUtils.class);
    }

    @Deployment(testable = false, name = "context-producer-method.ear")
    public static EnterpriseArchive createEarDeployment() {
        return ShrinkWrap.create(EnterpriseArchive.class, "context-producer-method.ear")
            .addAsModule(ShrinkWrap.create(WebArchive.class, "context-producer-method-a.war")
                .addClass(CamelContextProducerA.class)
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml")
            )
            .addAsModule(ShrinkWrap.create(WebArchive.class, "context-producer-method-b.war")
                .addClass(CamelContextProducerB.class)
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml")
            );
    }

    @Test
    public void testCamelCdiEarContextProducerMethod() throws Exception {
        CamelContext camelctxA = contextRegistry.getCamelContext("context-producer-a");
        Assert.assertNotNull("Expected context-producer-a to not be null", camelctxA);

        CamelContext camelctxB = contextRegistry.getCamelContext("context-producer-b");
        Assert.assertNotNull("Expected context-producer-b to not be null", camelctxB);

        String moduleNameA = TestUtils.getClassLoaderModuleName(camelctxA.getApplicationContextClassLoader());
        Assert.assertEquals("deployment.context-producer-method.ear.context-producer-method-a.war", moduleNameA);

        String moduleNameB = TestUtils.getClassLoaderModuleName(camelctxB.getApplicationContextClassLoader());
        Assert.assertEquals("deployment.context-producer-method.ear.context-producer-method-b.war", moduleNameB);
    }
}
