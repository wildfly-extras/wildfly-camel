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

package org.wildfly.extension.camel.handler;

import static org.wildfly.extension.camel.CamelLogger.LOGGER;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;

import org.apache.camel.impl.DefaultClassResolver;
import org.apache.camel.util.FileUtil;
import org.jboss.gravia.utils.IllegalArgumentAssertion;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleClassLoader;
import org.jboss.modules.ModuleIdentifier;


/**
 * A class resolver that delegates to a module class loader
 *
 * @author Thomas.Diesler@jboss.com
 * @since 11-Feb-2015
 */
final class WildFlyClassResolver extends DefaultClassResolver {

    private final ModuleClassLoader classLoader;
    private final ModuleIdentifier moduleId;

    WildFlyClassResolver(Module module) {
        IllegalArgumentAssertion.assertNotNull(module, "module");
        this.classLoader = module.getClassLoader();
        this.moduleId = module.getIdentifier();
    }

    WildFlyClassResolver(ClassLoader classLoader) {
        IllegalArgumentAssertion.assertNotNull(classLoader, "classLoader");
        IllegalArgumentAssertion.assertTrue(classLoader instanceof ModuleClassLoader, "ModuleClassLoader required: " + classLoader);
        Module module = ((ModuleClassLoader) classLoader).getModule();
        this.classLoader = module.getClassLoader();
        this.moduleId = module.getIdentifier();
    }

    @Override
    protected Class<?> loadClass(String className, ClassLoader defaultClassLoader) {
        IllegalArgumentAssertion.assertNotNull(className, "className");
        Class<?> loadedClass = null;
        try {
            loadedClass = classLoader.loadClass(className);
        } catch (ClassNotFoundException e) {
            LOGGER.warn("Cannot load '{}' from module: {}", className, moduleId);
        }
        return loadedClass;
    }

    public InputStream loadResourceAsStream(String uri) {
        IllegalArgumentAssertion.assertNotNull(uri, "uri");
        String resolvedName = FileUtil.compactPath(uri, '/');
        return classLoader.getResourceAsStream(resolvedName);
    }

    public URL loadResourceAsURL(String uri) {
        IllegalArgumentAssertion.assertNotNull(uri, "uri");
        String resolvedName = FileUtil.compactPath(uri, '/');
        return classLoader.getResource(resolvedName);
    }

    @Override
    public Enumeration<URL> loadAllResourcesAsURL(String packageName) {
        IllegalArgumentAssertion.assertNotNull(packageName, "packageName");
        try {
            return classLoader.getResources(packageName);
        } catch (IOException e) {
            LOGGER.warn("Cannot load resources for: {}", packageName);
            return null;
        }
    }
}
