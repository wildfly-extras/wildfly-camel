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
import org.apache.camel.spi.CamelContextTracker;
import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.camel.test.cdi.subC.InjectedContextBean;
import org.wildfly.extension.camel.CamelAware;
import org.wildfly.extension.camel.CamelContextRegistry;

/**
 * Verifies that CDI proxied camel contexts do not become candidates for lifecycle management.
 */
@CamelAware
@RunWith(Arquillian.class)
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

    @Test
    public void testCDIContextCreation() throws InterruptedException {
        FakeContextTracker tracker = new FakeContextTracker();
        try {
            tracker.open();

            Assert.assertEquals(0, tracker.getContextCount());
            Assert.assertEquals(0, contextRegistry.getCamelContexts().size());

            deployer.deploy(CDI_CONTEXT_A);
            Assert.assertEquals(1, tracker.getContextCount());
            Assert.assertEquals(1, contextRegistry.getCamelContexts().size());

            deployer.deploy(CDI_CONTEXT_B);
            Assert.assertEquals(2, tracker.getContextCount());
            Assert.assertEquals(2, contextRegistry.getCamelContexts().size());

            deployer.undeploy(CDI_CONTEXT_A);
            deployer.undeploy(CDI_CONTEXT_B);

            Assert.assertEquals(0, contextRegistry.getCamelContexts().size());
        } finally {
            tracker.close();
        }
    }

    @Deployment(name = CDI_CONTEXT_A, managed = false, testable = false)
    public static JavaArchive createCdiTestJar() {
        JavaArchive archive = ShrinkWrap.create(JavaArchive.class, CDI_CONTEXT_A);
        archive.addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml");
        archive.addPackage(InjectedContextBean.class.getPackage());
        return archive;
    }

    @Deployment(name = CDI_CONTEXT_B, managed = false , testable = false)
    public static JavaArchive createOtherCdiTestJar() {
        JavaArchive archive = ShrinkWrap.create(JavaArchive.class, CDI_CONTEXT_B);
        archive.addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml");
        archive.addPackage(InjectedContextBean.class.getPackage());
        return archive;
    }

    private class FakeContextTracker extends CamelContextTracker {
        private List<CamelContext> camelContextList = new ArrayList<>();

        @Override
        public void contextCreated(CamelContext camelctx) {
            synchronized (camelContextList) {
                if (!camelctx.getClass().getName().contains("Proxy")) {
                    camelContextList.add(camelctx);
                }
            }
        }

        public int getContextCount() {
            synchronized (camelContextList) {
                return camelContextList.size();
            }
        }
    }
}
