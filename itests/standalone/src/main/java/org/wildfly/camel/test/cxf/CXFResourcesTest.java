package org.wildfly.camel.test.cxf;
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


import java.net.URL;

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

/**
 * Test resource access in META-INF/cxf
 *
 * @author thomas.diesler@jboss.com
 * @since 19-Mar-2015
 */
@RunWith(Arquillian.class)
public class CXFResourcesTest {

    @Deployment
    public static JavaArchive createdeployment() {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "cxf-resaccess-tests");
        return archive;
    }

    @Test
    public void testAccessFromCXFModule() throws Exception {
        ModuleLoader moduleLoader = Module.getCallerModuleLoader();
        ModuleIdentifier modid = ModuleIdentifier.create("org.apache.cxf");
        ModuleClassLoader classLoader = moduleLoader.loadModule(modid).getClassLoader();
        URL resurl = classLoader.getResource("META-INF/cxf/cxf.xml");
        Assert.assertNotNull("URL not null", resurl);
    }

    @Test
    public void testAccessFromCXFComponentModule() throws Exception {
        ModuleLoader moduleLoader = Module.getCallerModuleLoader();
        ModuleIdentifier modid = ModuleIdentifier.create("org.apache.camel.component.cxf");
        ModuleClassLoader classLoader = moduleLoader.loadModule(modid).getClassLoader();
        URL resurl = classLoader.getResource("META-INF/cxf/cxf.xml");
        Assert.assertNotNull("URL not null", resurl);
    }

    @Test
    public void testAccessFromCamelComponentModule() throws Exception {
        ModuleLoader moduleLoader = Module.getCallerModuleLoader();
        ModuleIdentifier modid = ModuleIdentifier.create("org.apache.camel.component");
        ModuleClassLoader classLoader = moduleLoader.loadModule(modid).getClassLoader();
        URL resurl = classLoader.getResource("META-INF/cxf/cxf.xml");
        Assert.assertNotNull("URL not null", resurl);
    }

    @Test
    public void testAccessFromDeployment() throws Exception {
        ClassLoader classLoader = getClass().getClassLoader();
        URL resurl = classLoader.getResource("META-INF/cxf/cxf.xml");
        Assert.assertNotNull("URL not null", resurl);
    }
}
