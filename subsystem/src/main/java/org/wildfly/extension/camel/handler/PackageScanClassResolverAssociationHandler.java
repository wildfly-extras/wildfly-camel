/*
 * #%L
 * Wildfly Camel :: Subsystem
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

import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.apache.camel.CamelContext;
import org.apache.camel.impl.DefaultPackageScanClassResolver;
import org.apache.camel.spi.PackageScanClassResolver;
import org.apache.camel.spi.PackageScanFilter;
import org.jboss.gravia.utils.IllegalStateAssertion;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.modules.ModuleClassLoader;
import org.wildfly.extension.camel.ContextCreateHandler;

public final class PackageScanClassResolverAssociationHandler implements ContextCreateHandler {

    private final Map<DotName, Set<ClassInfo>> annotatedClasses;

    public PackageScanClassResolverAssociationHandler(Map<DotName, Set<ClassInfo>> annotatedClasses) {
        this.annotatedClasses = annotatedClasses;
    }

    @Override
    public void setup(CamelContext camelctx) {

        // Verify that the application context class loader is a ModuleClassLoader
        final ClassLoader classLoader = camelctx.getApplicationContextClassLoader();
        IllegalStateAssertion.assertTrue(classLoader instanceof ModuleClassLoader, "Invalid class loader association: " + classLoader);

        PackageScanClassResolver resolver = new DefaultPackageScanClassResolver() {
            protected void find(PackageScanFilter filter, String packageName, ClassLoader loader, Set<Class<?>> classes) {
                if (loader == classLoader) {
                    for (Entry<DotName, Set<ClassInfo>> entry : annotatedClasses.entrySet()) {
                        for (ClassInfo classInfo : entry.getValue()) {
                            Class<?> targetClass = loadClass(classLoader, classInfo.name());
                            if (filter.matches(targetClass)) {
                                classes.add(targetClass);
                            }
                        }
                    }
                }
            }
        };
        resolver.addClassLoader(classLoader);
        camelctx.setPackageScanClassResolver(resolver);
    }

    private Class<?> loadClass(ClassLoader classLoader, DotName dotname) {
        try {
            return classLoader.loadClass(dotname.toString());
        } catch (ClassNotFoundException ex) {
            throw new IllegalStateException(ex);
        }
    }
}