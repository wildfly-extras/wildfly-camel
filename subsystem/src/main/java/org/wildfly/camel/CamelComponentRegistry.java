/*
 * #%L
 * Wildfly Camel Subsystem
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


package org.wildfly.camel;

import java.util.Set;

import org.apache.camel.Component;
import org.apache.camel.spi.ComponentResolver;
import org.jboss.modules.Module;

/**
 * An abstraction of {@link Component} registration.
 *
 * The {@link CamelComponentRegistry} is the entry point for {@link Component} registration and lookup.
 *
 * @see {@link org.wildfly.camel.service.CamelComponentRegistryService}
 *
 * @author Thomas.Diesler@jboss.com
 * @since 05-Jul-2013
 */
public interface CamelComponentRegistry {

    /** Get the camel component resolver for the given name */
    ComponentResolver getComponent(String name);

    /** Register the camel component in this registry */
    CamelComponentRegistration registerComponent(String name, ComponentResolver resolver);

    /** Register the camel components that are found in the given module */
    Set<CamelComponentRegistration> registerComponents(Module module);

    /** The return handle for camel context registrations */
    interface CamelComponentRegistration {

        ComponentResolver getComponentResolver();

        void unregister();
    }
}
