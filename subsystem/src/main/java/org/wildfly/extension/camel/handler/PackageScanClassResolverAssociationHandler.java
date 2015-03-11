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

import static org.wildfly.extension.camel.CamelLogger.LOGGER;

import java.util.Iterator;
import java.util.Set;

import org.apache.camel.CamelContext;
import org.apache.camel.impl.DefaultPackageScanClassResolver;
import org.apache.camel.spi.PackageScanClassResolver;
import org.apache.camel.spi.PackageScanFilter;
import org.jboss.modules.ModuleClassLoader;
import org.jboss.modules.Resource;
import org.wildfly.extension.camel.ContextCreateHandler;

/**
 * A {@link ContextCreateHandler} for PackageScanClassResolver association
 *
 * @author Thomas.Diesler@jboss.com
 * @since 13-Mar-2015
 */
public final class PackageScanClassResolverAssociationHandler implements ContextCreateHandler {

    private final ModuleClassLoader moduleClassLoader;

    public PackageScanClassResolverAssociationHandler(ModuleClassLoader moduleClassLoader) {
        this.moduleClassLoader = moduleClassLoader;
    }

    @Override
    public void setup(CamelContext camelctx) {
        PackageScanClassResolver resolver = new PackageScanClassResolverImpl(moduleClassLoader);
        camelctx.setPackageScanClassResolver(resolver);
    }

    static final class PackageScanClassResolverImpl extends DefaultPackageScanClassResolver {

        PackageScanClassResolverImpl(ModuleClassLoader classLoader) {
            addClassLoader(classLoader);
        }

        @Override
        protected void find(PackageScanFilter filter, String packageName, ClassLoader classLoader, Set<Class<?>> classes) {
            LOGGER.info("Searching for: {} in package: {} using classloader: {}", new Object[] { filter, packageName, classLoader });
            if (classLoader instanceof ModuleClassLoader) {
                ModuleClassLoader moduleClassLoader = (ModuleClassLoader) classLoader;
                Iterator<Resource> itres = moduleClassLoader.iterateResources("/", true);
                while (itres.hasNext()) {
                    Resource resource = itres.next();
                    String resname = resource.getName();
                    if (resname.startsWith(packageName) && resname.endsWith(".class")) {
                        String className = resname.substring(0, resname.length() - 6).replace('/', '.');
                        try {
                            Class<?> loadedClass = moduleClassLoader.loadClass(className);
                            if (filter.matches(loadedClass)) {
                                LOGGER.info("Found type in package scan: {}", loadedClass.getName());
                                classes.add(loadedClass);
                            }
                        } catch (ClassNotFoundException ex) {
                            //ignore
                        }
                    }
                }
            } else {
                super.find(filter, packageName, classLoader, classes);
            }
        }
    }
}