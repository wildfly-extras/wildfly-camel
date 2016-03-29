/*
 * #%L
 * Wildfly Camel :: Testsuite
 * %%
 * Copyright (C) 2013 - 2016 RedHat
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

import java.util.ArrayList;
import java.util.List;

import org.apache.camel.CamelContext;
import org.apache.camel.spi.Container;
import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.camel.test.cdi.subC.InjectedContextBean;
import org.wildfly.extension.camel.CamelAware;
import org.wildfly.extension.camel.CamelContextRegistry;

/**
 * Verifies that CDI proxied camel contexts do not become candidates for lifecycle management.
 */
@RunWith(Arquillian.class)
@CamelAware
public class CDIContextCreationIntegrationTest {

    private static final String CDI_CONTEXT_A = "cdi-context-a.jar";
    private static final String CDI_CONTEXT_B = "cdi-context-b.jar";

    @ArquillianResource
    private Deployer deployer;

    @ArquillianResource
    private CamelContextRegistry contextRegistry;

    @Deployment
    public static JavaArchive createDeployment() {
        return ShrinkWrap.create(JavaArchive.class);
    }

    @Deployment(name = CDI_CONTEXT_A, managed = false, testable = false)
    public static JavaArchive createCdiTestJar() {
        return createCamelCdiDeployment(CDI_CONTEXT_A);
    }

    @Deployment(name = CDI_CONTEXT_B, managed = false , testable = false)
    public static JavaArchive createOtherCdiTestJar() {
        return createCamelCdiDeployment(CDI_CONTEXT_B);
    }

    private static JavaArchive createCamelCdiDeployment(String name) {
        return ShrinkWrap.create(JavaArchive.class, name)
            .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml")
            .addPackage(InjectedContextBean.class.getPackage());
    }

    @After
    public void tearDown() {
        Container.Instance.set(null);
    }

    @Test
    public void testCDIContextCreation() throws InterruptedException {
        final FakeContainer container = new FakeContainer();
        Container.Instance.set(container);

        Assert.assertEquals(0, container.getContextCount());
        Assert.assertEquals(0, contextRegistry.getCamelContexts().size());

        deployer.deploy(CDI_CONTEXT_A);
        Assert.assertEquals(1, container.getContextCount());
        Assert.assertEquals(1, contextRegistry.getCamelContexts().size());

        deployer.deploy(CDI_CONTEXT_B);
        Assert.assertEquals(2, container.getContextCount());
        Assert.assertEquals(2, contextRegistry.getCamelContexts().size());

        deployer.undeploy(CDI_CONTEXT_A);
        deployer.undeploy(CDI_CONTEXT_B);

        Assert.assertEquals(0, contextRegistry.getCamelContexts().size());
    }

    private static final class FakeContainer implements Container {
        private List<CamelContext> camelContextList = new ArrayList<>();

        @Override
        public void manage(CamelContext camelContext) {
            synchronized (camelContextList) {
                camelContextList.add(camelContext);
            }
        }

        public int getContextCount() {
            synchronized (camelContextList) {
                return camelContextList.size();
            }
        }
    }
}
