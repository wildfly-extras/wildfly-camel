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

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;

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
import org.wildfly.camel.test.cdi.ear.config.inject.CamelContextInjectionClassLoaderRecorderA;
import org.wildfly.camel.test.cdi.ear.config.inject.CamelContextInjectionClassLoaderRecorderB;
import org.wildfly.camel.test.common.utils.ManifestBuilder;
import org.wildfly.camel.test.common.utils.TestUtils;
import org.wildfly.extension.camel.CamelAware;
import org.wildfly.extension.camel.CamelContextRegistry;

/**
 Verifies that the 'auto-configured' CDI CamelContext is associated with the EAR deployment ClassLoader.

 This is the only viable option given that the EAR BeanArchive and Camel CDI extension is shared
 across each WAR sub-deployment.

 https://github.com/apache/camel/blob/master/components/camel-cdi/src/main/docs/cdi.adoc#auto-configured-camel-context
*/

@CamelAware
@RunWith(Arquillian.class)
public class CDIEarCamelContextInjectTest {

    @ArquillianResource
    private CamelContextRegistry contextRegistry;

    @Deployment
    public static JavaArchive createDeployment() {
        return ShrinkWrap.create(JavaArchive.class, "camel-cdi-ear-context-inject-tests.jar")
            .addClass(TestUtils.class);
    }

    @Deployment(testable = false, name = "context-inject.ear")
    public static EnterpriseArchive createEarDeployment() {
        return ShrinkWrap.create(EnterpriseArchive.class, "context-inject.ear")
            .addAsModule(ShrinkWrap.create(WebArchive.class, "context-inject-a.war")
                .addClasses(CamelContextInjectionClassLoaderRecorderA.class, TestUtils.class)
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml")
                .setManifest(() -> {
                    ManifestBuilder builder = new ManifestBuilder();
                    builder.addManifestHeader("Dependencies", "org.jboss.modules");
                    return builder.openStream();
                })
            )
            .addAsModule(ShrinkWrap.create(WebArchive.class, "context-inject-b.war")
                .addClasses(CamelContextInjectionClassLoaderRecorderB.class, TestUtils.class)
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml")
                .setManifest(() -> {
                    ManifestBuilder builder = new ManifestBuilder();
                    builder.addManifestHeader("Dependencies", "org.jboss.modules");
                    return builder.openStream();
                })
            );
    }

    @Test
    public void testCamelCdiEarContextInjection() throws Exception {
        Set<CamelContext> camelContexts = contextRegistry.getCamelContexts();
        Assert.assertEquals("Expected 1 CamelContext to be present in CamelContextRegistry", camelContexts.size(), 1);

        // Verify the CamelContext ApplicationContextClassLoader is as expected
        CamelContext camelctx = camelContexts.iterator().next();
        String moduleName = TestUtils.getClassLoaderModuleName(camelctx.getApplicationContextClassLoader());
        Assert.assertEquals("deployment.context-inject.ear", moduleName);

        // Verify that each WAR sub deployment each had the same CamelContext injected
        Path dataDir = Paths.get(System.getProperty("jboss.server.data.dir"));

        String moduleNameA = new String(Files.readAllBytes(dataDir.resolve("injected-context-a.txt")), StandardCharsets.UTF_8);
        Assert.assertEquals("deployment.context-inject.ear", moduleNameA);

        String moduleNameB = new String(Files.readAllBytes(dataDir.resolve("injected-context-b.txt")), StandardCharsets.UTF_8);
        Assert.assertEquals("deployment.context-inject.ear", moduleNameB);
    }
}
