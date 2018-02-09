/*
 * #%L
 * Wildfly Camel :: Testsuite
 * %%
 * Copyright (C) 2013 - 2017 RedHat
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
package org.wildfly.camel.test.spring;

import static org.jboss.as.naming.deployment.ContextNames.BindInfo;
import static org.jboss.as.naming.deployment.ContextNames.bindInfoFor;

import org.apache.camel.CamelContext;
import org.apache.camel.ServiceStatus;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.naming.ManagedReferenceFactory;
import org.jboss.gravia.resource.ManifestBuilder;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.camel.test.spring.subE.CamelContextStartupEventNotifier;
import org.wildfly.camel.test.spring.subE.DelayedBinderService;
import org.wildfly.camel.test.spring.subE.DelayedBinderServiceActivator;
import org.wildfly.extension.camel.CamelAware;
import org.wildfly.extension.camel.CamelContextRegistry;

@RunWith(Arquillian.class)
@CamelAware
public class SpringContextBindingDependenciesEarTest {

    @ArquillianResource
    private ServiceContainer serviceContainer;

    @ArquillianResource
    private CamelContextRegistry contextRegistry;

    @Deployment
    public static EnterpriseArchive createDeployment() {
        return ShrinkWrap.create(EnterpriseArchive.class, "spring-jndi-binding-tests.ear")
            .addAsModule(
                ShrinkWrap.create(JavaArchive.class, "spring-jndi-binding-tests.jar")
                    .addClasses(SpringContextBindingDependenciesEarTest.class, DelayedBinderServiceActivator.class,
                        DelayedBinderService.class, CamelContextStartupEventNotifier.class)
                    .addAsResource("spring/jndi-bindings-camel-context.xml", "jndi-bindings-camel-context.xml")
                    .addAsManifestResource(new StringAsset(DelayedBinderServiceActivator.class.getName()), "services/org.jboss.msc.service.ServiceActivator")
                    .setManifest(() -> {
                        ManifestBuilder builder = new ManifestBuilder();
                        builder.addManifestHeader("Dependencies", "org.jboss.as.server");
                        return builder.openStream();
                    })
            );
    }

    @Test
    public void testCamelSpringDeploymentWaitsForJndiBindings() {
        CamelContext camelctx = contextRegistry.getCamelContext("jndi-binding-spring-context");
        Assert.assertNotNull("Expected jndi-binding-spring-context to not be null", camelctx);
        Assert.assertEquals(ServiceStatus.Started, camelctx.getStatus());

        BindInfo bindInfo = bindInfoFor("java:/spring/binding/test");
        ServiceName serviceName = bindInfo.getBinderServiceName();
        ServiceController<?> controller = serviceContainer.getService(serviceName);

        Assert.assertNotNull("Expected controller to not be null", controller);
        ManagedReferenceFactory referenceFactory = (ManagedReferenceFactory) controller.getValue();
        DelayedBinderService binderService = (DelayedBinderService) referenceFactory.getReference().getInstance();

        // Make sure the DelayedBinderService did sleep
        Assert.assertTrue("Expected DelayedBinderService.getSleepStart() to be > 0", binderService.getSleepStart() > 0);

        // Verify that the camel context waited for the binding service to finish starting
        CamelContextStartupEventNotifier notifier = (CamelContextStartupEventNotifier) camelctx.getRegistry().lookupByName("contextStartupEventNotifier");
        long startupDelay = notifier.getStartupTime() - binderService.getSleepStart();
        Assert.assertTrue(startupDelay >= binderService.getSleepDelay());
    }
}
