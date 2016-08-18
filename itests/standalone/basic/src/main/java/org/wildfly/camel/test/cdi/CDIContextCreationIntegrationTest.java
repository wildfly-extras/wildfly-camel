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
import java.util.concurrent.TimeUnit;

import org.apache.camel.CamelContext;
import org.apache.camel.ServiceStatus;
import org.apache.camel.component.mock.MockEndpoint;
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
import org.wildfly.camel.test.cdi.subA.RouteBuilderE;
import org.wildfly.camel.test.cdi.subA.RouteBuilderF;
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
    private static final String CDI_CONTEXT_C = "cdi-context-c.jar";

    @ArquillianResource
    private Deployer deployer;

    @ArquillianResource
    private CamelContextRegistry contextRegistry;

    @Deployment
    public static JavaArchive createDeployment() {
        JavaArchive archive = ShrinkWrap.create(JavaArchive.class);
        archive.addClasses(RouteBuilderF.class);
        return archive;
    }

    @Test
    public void testCDIContextCreation() throws InterruptedException {
        try (FakeContextTracker tracker = new FakeContextTracker()) {
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
        }
    }

    @Test
    public void testManualComponentConfig() throws InterruptedException {
        deployer.deploy(CDI_CONTEXT_C);
        try {
            Assert.assertEquals(1, contextRegistry.getCamelContexts().size());
            CamelContext camelctx = contextRegistry.getCamelContext("contextF");
            Assert.assertNotNull("Context not null", camelctx);

            Assert.assertEquals(ServiceStatus.Started, camelctx.getStatus());
            
            MockEndpoint mock = camelctx.getEndpoint(RouteBuilderF.MOCK_RESULT_URI, MockEndpoint.class);
            Assert.assertTrue("All messages received", mock.await(500, TimeUnit.MILLISECONDS));
            Assert.assertEquals(3, mock.getExpectedCount());
        } finally {
            deployer.undeploy(CDI_CONTEXT_C);
        }
    }

    @Deployment(name = CDI_CONTEXT_A, managed = false, testable = false)
    public static JavaArchive createTestJarA() {
        JavaArchive archive = ShrinkWrap.create(JavaArchive.class, CDI_CONTEXT_A);
        archive.addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml");
        archive.addClasses(RouteBuilderE.class);
        return archive;
    }

    @Deployment(name = CDI_CONTEXT_B, managed = false , testable = false)
    public static JavaArchive createTestJarB() {
        JavaArchive archive = ShrinkWrap.create(JavaArchive.class, CDI_CONTEXT_B);
        archive.addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml");
        archive.addClasses(RouteBuilderE.class);
        return archive;
    }

    @Deployment(name = CDI_CONTEXT_C, managed = false , testable = false)
    public static JavaArchive createTestJarC() {
        JavaArchive archive = ShrinkWrap.create(JavaArchive.class, CDI_CONTEXT_C);
        archive.addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml");
        archive.addClasses(RouteBuilderF.class);
        return archive;
    }

    private class FakeContextTracker extends CamelContextTracker {
        private List<CamelContext> camelContextList = new ArrayList<>();

        @Override
        public void contextCreated(CamelContext camelctx) {
            synchronized (camelContextList) {
                camelContextList.add(camelctx);
            }
        }

        int getContextCount() {
            synchronized (camelContextList) {
                return camelContextList.size();
            }
        }
    }
}
