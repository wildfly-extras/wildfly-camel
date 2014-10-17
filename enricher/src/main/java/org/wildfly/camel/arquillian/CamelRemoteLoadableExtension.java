/*
 * #%L
 * Wildfly Camel Enricher
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
package org.wildfly.camel.arquillian;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import org.jboss.arquillian.container.test.spi.RemoteLoadableExtension;
import org.jboss.arquillian.test.spi.enricher.resource.ResourceProvider;
import org.jboss.as.arquillian.service.DependenciesProvider;
import org.jboss.modules.ModuleIdentifier;

/**
 * CamelRemoteLoadableExtension
 *
 * @author Thomas.Diesler@jboss.com
 * @since 07-Jun-2011
 */
public class CamelRemoteLoadableExtension implements RemoteLoadableExtension, DependenciesProvider {

    private static Set<ModuleIdentifier> dependencies = new LinkedHashSet<ModuleIdentifier>();
    static {
        dependencies.add(ModuleIdentifier.create("org.jboss.gravia"));
        dependencies.add(ModuleIdentifier.create("org.wildfly.camel"));
    }

    @Override
    public void register(ExtensionBuilder builder) {
        builder.service(ResourceProvider.class, CamelContextFactoryProvider.class);
        builder.service(ResourceProvider.class, CamelContextRegistryProvider.class);
        builder.service(ResourceProvider.class, ProvisionerProvider.class);
    }

    @Override
    public Set<ModuleIdentifier> getDependencies() {
        return Collections.unmodifiableSet(dependencies);
    }

}
