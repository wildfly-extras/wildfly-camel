/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
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
