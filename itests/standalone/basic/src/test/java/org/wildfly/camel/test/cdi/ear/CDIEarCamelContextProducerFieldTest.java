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
import org.wildfly.camel.test.cdi.ear.config.producer.field.CamelContextProducerFieldA;
import org.wildfly.camel.test.cdi.ear.config.producer.field.CamelContextProducerFieldB;
import org.wildfly.camel.test.common.utils.TestUtils;
import org.wildfly.extension.camel.CamelAware;
import org.wildfly.extension.camel.CamelContextRegistry;

@CamelAware
@RunWith(Arquillian.class)
public class CDIEarCamelContextProducerFieldTest {

    @ArquillianResource
    private CamelContextRegistry contextRegistry;

    @Deployment
    public static JavaArchive createDeployment() {
        return ShrinkWrap.create(JavaArchive.class, "camel-cdi-ear-producer-field-tests.jar")
            .addClass(TestUtils.class);
    }

    @Deployment(testable = false, name = "context-producer-field.ear")
    public static EnterpriseArchive createEarDeployment() {
        return ShrinkWrap.create(EnterpriseArchive.class, "context-producer-field.ear")
            .addAsModule(ShrinkWrap.create(WebArchive.class, "context-producer-field-a.war")
                .addClass(CamelContextProducerFieldA.class)
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml")
            )
            .addAsModule(ShrinkWrap.create(WebArchive.class, "context-producer-field-b.war")
                .addClass(CamelContextProducerFieldB.class)
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml")
            );
    }

    @Test
    public void testCamelCdiEarContextProducerField() throws Exception {
        CamelContext camelctxA = contextRegistry.getCamelContext("context-producer-field-a");
        Assert.assertNotNull("Expected context-producer-field-a to not be null", camelctxA);

        CamelContext camelctxB = contextRegistry.getCamelContext("context-producer-field-b");
        Assert.assertNotNull("Expected context-producer-field-b to not be null", camelctxB);

        String moduleNameA = TestUtils.getClassLoaderModuleName(camelctxA.getApplicationContextClassLoader());
        Assert.assertEquals("deployment.context-producer-field.ear.context-producer-field-a.war", moduleNameA);

        String moduleNameB = TestUtils.getClassLoaderModuleName(camelctxB.getApplicationContextClassLoader());
        Assert.assertEquals("deployment.context-producer-field.ear.context-producer-field-b.war", moduleNameB);
    }
}
