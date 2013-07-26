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

import org.apache.camel.CamelContext;

/**
 * An abstraction of {@link CamelContext} registration.
 *
 * The {@link CamelContextRegistry} is the entry point for {@link CamelContext} registration and lookup.
 *
 * @see {@link CamelContextRegistryService}
 *
 * @author Thomas.Diesler@jboss.com
 * @since 19-Apr-2013
 */
public interface CamelContextRegistry {

    /** Get the camel context for the given name */
    CamelContext getCamelContext(String name);

    /** Register the camel context in this registry */
    CamelContextRegistration registerCamelContext(CamelContext camelctx);

    /** The return handle for camel context registrations */
    interface CamelContextRegistration {

        CamelContext getCamelContext();

        void unregister();
    }
}
