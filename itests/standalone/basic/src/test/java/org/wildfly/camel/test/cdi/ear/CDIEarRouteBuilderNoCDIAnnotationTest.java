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

import java.util.Set;

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
import org.wildfly.camel.test.cdi.ear.config.auto.DefaultRouteBuilderA;
import org.wildfly.camel.test.cdi.ear.config.auto.DefaultRouteBuilderB;
import org.wildfly.camel.test.common.utils.TestUtils;
import org.wildfly.extension.camel.CamelAware;
import org.wildfly.extension.camel.CamelContextRegistry;

@CamelAware
@RunWith(Arquillian.class)
public class CDIEarRouteBuilderNoCDIAnnotationTest {

    @ArquillianResource
    private CamelContextRegistry contextRegistry;

    @Deployment
    public static JavaArchive createDeployment() {
        return ShrinkWrap.create(JavaArchive.class, "camel-cdi-ear-rb-notannotated-tests.jar")
            .addClass(TestUtils.class);
    }

    @Deployment(testable = false, name = "route-builder-not-annotated.ear")
    public static EnterpriseArchive createEarDeployment() {
        return ShrinkWrap.create(EnterpriseArchive.class, "route-builder-not-annotated.ear")
            .addAsModule(ShrinkWrap.create(WebArchive.class, "route-builder-not-annotated-a.war")
                .addClass(DefaultRouteBuilderA.class)
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml")
            )
            .addAsModule(ShrinkWrap.create(WebArchive.class, "route-builder-not-annotated-b.war")
                .addClass(DefaultRouteBuilderB.class)
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml")
            );
    }

    @Test
    public void testCamelCdiEarRouteBuilderWithoutAnnotations() throws Exception {
        Set<CamelContext> camelContexts = contextRegistry.getCamelContexts();
        Assert.assertEquals("Expected 1 CamelContext to be present in CamelContextRegistry", camelContexts.size(), 1);

        CamelContext camelctx = camelContexts.iterator().next();

        String moduleName = TestUtils.getClassLoaderModuleName(camelctx.getApplicationContextClassLoader());

        Assert.assertTrue("Expected ApplicationContextClassLoader to be EAR deployment ClassLoader",
            moduleName.contains("deployment.route-builder-not-annotated.ear"));

        ProducerTemplate template = camelctx.createProducerTemplate();
        String result = template.requestBody("direct:routeA", null, String.class);
        Assert.assertEquals("Hello from routeA", result);

        result = template.requestBody("direct:routeB", null, String.class);
        Assert.assertEquals("Hello from routeB", result);
    }
}
