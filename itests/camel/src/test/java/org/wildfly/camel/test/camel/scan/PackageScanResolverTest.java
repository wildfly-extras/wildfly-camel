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

package org.wildfly.camel.test.camel.scan;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Paths;
import java.util.Set;

import org.apache.camel.CamelContext;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.spi.PackageScanClassResolver;
import org.apache.camel.spi.PackageScanFilter;
import org.jboss.modules.DependencySpec;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleClassLoader;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.modules.ModuleSpec;
import org.jboss.modules.ResourceLoader;
import org.jboss.modules.ResourceLoaderSpec;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.wildfly.camel.test.camel.JavaArchiveResourceLoader;
import org.wildfly.camel.test.camel.TestModuleLoader;
import org.wildfly.extension.camel.handler.PackageScanClassResolverAssociationHandler;

public class PackageScanResolverTest {

    @Test
    public void testDirectModule() throws Exception {

        TestModuleLoader moduleLoader = new TestModuleLoader();

        ModuleIdentifier modidA = ModuleIdentifier.create("archiveA");
        ModuleSpec.Builder specBuilderA = ModuleSpec.build(modidA);
        ResourceLoader resourceLoaderA = new JavaArchiveResourceLoader(getModuleA());
        specBuilderA.addResourceRoot(ResourceLoaderSpec.createResourceLoaderSpec(resourceLoaderA));
        specBuilderA.addDependency(DependencySpec.createLocalDependencySpec());
        moduleLoader.addModuleSpec(specBuilderA.create());
        Module moduleA = moduleLoader.loadModule(modidA);
        ModuleClassLoader classloaderA = moduleA.getClassLoader();

        CamelContext camelctx = new DefaultCamelContext();
        PackageScanClassResolver resolver = setupPackageScanClassResolver(classloaderA, camelctx);

        Set<Class<?>> result = resolver.findImplementations(RuntimeException.class, "org/wildfly/extension/camel/config");
        assertResolverResult(modidA, result);
    }

    @Test
    public void testDependentModule() throws Exception {

        TestModuleLoader moduleLoader = new TestModuleLoader();

        ModuleIdentifier modidA = ModuleIdentifier.create("archiveA");
        ModuleSpec.Builder specBuilder = ModuleSpec.build(modidA);
        ResourceLoader resourceLoaderA = new JavaArchiveResourceLoader(getModuleA());
        specBuilder.addResourceRoot(ResourceLoaderSpec.createResourceLoaderSpec(resourceLoaderA));
        specBuilder.addDependency(DependencySpec.createLocalDependencySpec());
        moduleLoader.addModuleSpec(specBuilder.create());

        ModuleIdentifier modidB = ModuleIdentifier.create("archiveB");
        specBuilder = ModuleSpec.build(modidB);
        specBuilder.addDependency(DependencySpec.createModuleDependencySpec(modidA, true));
        moduleLoader.addModuleSpec(specBuilder.create());
        Module moduleB = moduleLoader.loadModule(modidB);
        ModuleClassLoader classloaderB = moduleB.getClassLoader();

        CamelContext camelctx = new DefaultCamelContext();
        PackageScanClassResolver resolver = setupPackageScanClassResolver(classloaderB, camelctx);

        Set<Class<?>> result = resolver.findImplementations(RuntimeException.class, "org/wildfly/extension/camel/config");
        assertResolverResult(modidA, result);
    }

    private PackageScanClassResolver setupPackageScanClassResolver(ModuleClassLoader classLoader, CamelContext camelctx) {
        new PackageScanClassResolverAssociationHandler(classLoader).setup(camelctx);
        PackageScanClassResolver resolver = camelctx.getPackageScanClassResolver();
        resolver.addFilter(new PackageScanFilter() {
            @Override
            public boolean matches(Class<?> type) {
                return type.getClassLoader() instanceof ModuleClassLoader;
            }
        });
        return resolver;
    }

    private void assertResolverResult(ModuleIdentifier modid, Set<Class<?>> result) {
        Assert.assertEquals(1, result.size());
        Class<?> foundType = result.iterator().next();
        Assert.assertEquals("org.wildfly.extension.camel.config.ConfigException", foundType.getName());
        ModuleClassLoader modcl = (ModuleClassLoader) foundType.getClassLoader();
        Assert.assertEquals(modid, modcl.getModule().getIdentifier());
    }

    // A module that contains a class that is not on the surefire classpath
    private JavaArchive getModuleA() throws IOException {
        JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "moduleA.jar");
        URLClassLoader classloader = new URLClassLoader(new URL[] {Paths.get("../../config/target/classes").toUri().toURL()});
        archive.addClass("org.wildfly.extension.camel.config.ConfigException", classloader);
        return archive;
    }
}
