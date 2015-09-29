/*
 * #%L
 * Wildfly Camel :: Testsuite
 * %%
 * Copyright (C) 2013 - 2014 RedHat
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


import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleClassLoader;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.modules.ModuleLoader;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.extension.camel.CamelAware;

/**
 * [#540] jboss-modules does not respect path excludes
 *
 * https://github.com/wildfly-extras/wildfly-camel/issues/540
 */
@CamelAware
@RunWith(Arquillian.class)
public class SpringExcludedPathsTest {

    @Deployment
    public static JavaArchive createdeployment() {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "cxf-paths-tests");
        return archive;
    }

    @Test
    public void testAccessFromCamelSpringModule() throws Exception {
        ModuleLoader moduleLoader = Module.getCallerModuleLoader();
        ModuleIdentifier modid = ModuleIdentifier.create("org.apache.camel.spring");
        ModuleClassLoader classLoader = moduleLoader.loadModule(modid).getClassLoader();
        Class<?> loadedClass = classLoader.loadClass("org.apache.camel.spring.SpringCamelContext");
        Assert.assertNotNull("Class not null", loadedClass);
        loadedClass = classLoader.loadClass("org.apache.camel.osgi.OsgiSpringCamelContext");
        Assert.assertNotNull("Class not null", loadedClass);
    }

    @Test
    public void testAccessFromDeployment() throws Exception {
        ClassLoader classLoader = getClass().getClassLoader();
        Class<?> loadedClass = classLoader.loadClass("org.apache.camel.spring.SpringCamelContext");
        Assert.assertNotNull("Class not null", loadedClass);
        try {
            classLoader.loadClass("org.apache.camel.osgi.OsgiSpringCamelContext");
            Assert.fail("ClassNotFoundException expected");
        } catch (ClassNotFoundException ex) {
            // expected
        }
    }
}
