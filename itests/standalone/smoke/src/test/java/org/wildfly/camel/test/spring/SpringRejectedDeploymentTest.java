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
package org.wildfly.camel.test.spring;

import java.lang.management.ManagementFactory;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.extension.camel.CamelAware;
import org.wildfly.extension.camel.CamelContextRegistry;

@CamelAware
@RunWith(Arquillian.class)
public class SpringRejectedDeploymentTest {

    private static final String SIMPLE_JAR = "simple.jar";

    @ArquillianResource
    Deployer deployer;

    @ArquillianResource
    CamelContextRegistry contextRegistry;

    @ArquillianResource
    ServiceContainer serviceContainer;

    @Deployment
    public static JavaArchive createDeployment() {
        return ShrinkWrap.create(JavaArchive.class, "camel-spring-rejected-tests.jar");
    }

    @Deployment(testable = false, managed = false, name = SIMPLE_JAR)
    public static JavaArchive createCamelSpringDeployment() {
        return ShrinkWrap.create(JavaArchive.class, SIMPLE_JAR)
            .addAsResource("spring/failed-start-camel-context.xml", "camel-context.xml");
    }

    @Test
    public void testDeploymentRejectedForContextStartupFailure() throws Exception {

        List<String> names = contextRegistry.getCamelContextNames();
        Assert.assertEquals(Collections.emptyList(), names);

        MBeanServer server = ManagementFactory.getPlatformMBeanServer();
        Set<ObjectName> onames = server.queryNames(new ObjectName("org.apache.camel:*"), null);
        Assert.assertEquals(Collections.emptySet(), onames);

        try {
            deployer.deploy(SIMPLE_JAR);
            Assert.fail("Expected deployment exception to be thrown but it was not");
        } catch (Exception e) {
            // Make sure the deployment was rolled back
            ServiceController<?> service = serviceContainer.getService(ServiceName.of("jboss.deployment.unit.\"simple.jar\""));
            Assert.assertNull("Expected simple.jar service to be null", service);
        } finally {
            deployer.undeploy(SIMPLE_JAR);
        }

        onames = server.queryNames(new ObjectName("org.apache.camel:*"), null);
        Assert.assertEquals(Collections.emptySet(), onames);
    }
}
