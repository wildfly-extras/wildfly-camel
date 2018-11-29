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
import org.wildfly.camel.test.cdi.ear.config.classloading.ClassLoadingRouteBuilderA;
import org.wildfly.camel.test.cdi.ear.config.classloading.ClassLoadingRouteBuilderB;
import org.wildfly.camel.test.common.types.HelloBean;
import org.wildfly.camel.test.common.utils.TestUtils;
import org.wildfly.extension.camel.CamelAware;
import org.wildfly.extension.camel.CamelContextRegistry;

@CamelAware
@RunWith(Arquillian.class)
public class CDIEarClassLoadingTest {

    @ArquillianResource
    private CamelContextRegistry contextRegistry;

    @Deployment
    public static JavaArchive createDeployment() {
        return ShrinkWrap.create(JavaArchive.class, "camel-cdi-ear-class-loading-tests.jar")
            .addClass(TestUtils.class);
    }

    @Deployment(testable = false, name = "class-loading.ear")
    public static EnterpriseArchive createEarDeployment() {
        return ShrinkWrap.create(EnterpriseArchive.class, "class-loading.ear")
            .addAsModule(ShrinkWrap.create(WebArchive.class, "class-loading-a.war")
                .addClasses(ClassLoadingRouteBuilderA.class, HelloBean.class)
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml")
            )
            .addAsModule(ShrinkWrap.create(WebArchive.class, "class-loading-b.war")
                .addClasses(ClassLoadingRouteBuilderB.class, HelloBean.class)
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml")
            );
    }

    @Test
    public void testCamelCdiEarClassLoading() throws Exception {
        CamelContext camelctxA = contextRegistry.getCamelContext("sub-deployment-a");
        Assert.assertNotNull("Expected sub-deployment-a to not be null", camelctxA);

        CamelContext camelctxB = contextRegistry.getCamelContext("sub-deployment-b");
        Assert.assertNotNull("Expected sub-deployment-b to not be null", camelctxB);

        String moduleNameA = TestUtils.getClassLoaderModuleName(camelctxA.getApplicationContextClassLoader());
        Assert.assertEquals("deployment.class-loading.ear.class-loading-a.war", moduleNameA);

        String moduleNameB = TestUtils.getClassLoaderModuleName(camelctxB.getApplicationContextClassLoader());
        Assert.assertEquals("deployment.class-loading.ear.class-loading-b.war", moduleNameB);

        ProducerTemplate template = camelctxA.createProducerTemplate();
        String result = template.requestBody("direct:start", null, String.class);
        Assert.assertEquals("Hello RouteA", result);

        template = camelctxB.createProducerTemplate();
        result = template.requestBody("direct:start", null, String.class);
        Assert.assertEquals("Hello RouteB", result);
    }
}
