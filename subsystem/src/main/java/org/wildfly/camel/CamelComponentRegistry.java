/*
 * #%L
 * Wildfly Camel Subsystem
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
