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

package org.wildfly.camel.test.handler;

import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.modules.ModuleLoader;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.camel.test.handler.subA.CamelActivationBean;
import org.wildfly.camel.utils.ServiceLocator;
import org.wildfly.extension.camel.CamelAware;
import org.wildfly.extension.camel.CamelConstants;
import org.wildfly.extension.camel.ContextCreateHandlerRegistry;

@CamelAware
@RunWith(Arquillian.class)
public class ContextCreateHandlerRegistryIntegrationTest {

    private static final String CAMEL_TEST_JAR = "camel-test.jar";

    @ArquillianResource
    private Deployer deployer;

    @Deployment
    public static JavaArchive createDeployment() {
        return ShrinkWrap.create(JavaArchive.class, "context-create-handler-tests.jar");
    }

    @Deployment(testable = false, managed = false, name = CAMEL_TEST_JAR)
    public static JavaArchive createCamelDeployment() {
        return ShrinkWrap.create(JavaArchive.class, "camel-test.jar")
                .addClass(CamelActivationBean.class);
    }

    @Test
    public void testHandlerRegistry() throws Exception {
        ContextCreateHandlerRegistry handlerRegistry = ServiceLocator.getRequiredService(CamelConstants.CONTEXT_CREATE_HANDLER_REGISTRY_SERVICE_NAME, ContextCreateHandlerRegistry.class);
        ModuleLoader moduleLoader = Module.getCallerModuleLoader();

        deployer.deploy(CAMEL_TEST_JAR);

        Module module = moduleLoader.loadModule(ModuleIdentifier.create("deployment.camel-test.jar"));
        ClassLoader classLoader = module.getClassLoader();

        // Registry should have classloader key after deploy
        Assert.assertTrue(handlerRegistry.containsKey(classLoader));

        deployer.undeploy(CAMEL_TEST_JAR);

        // Registry should have removed classloader key after undeploy
        Assert.assertFalse("Expected registry to not contain key: " + classLoader, handlerRegistry.containsKey(classLoader));
    }
}
