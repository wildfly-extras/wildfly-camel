/*
 * #%L
 * Wildfly Camel Enricher
 * %%
 * Copyright (C) 2013 JBoss by Red Hat
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as 
 * published by the Free Software Foundation, either version 2.1 of the 
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public 
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
