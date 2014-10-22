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

package org.wildfly.camel;

import org.apache.camel.CamelContext;
import org.apache.camel.Component;
import org.apache.camel.impl.DefaultComponentResolver;
import org.apache.camel.spi.ComponentResolver;
import org.jboss.gravia.utils.IllegalArgumentAssertion;

/**
 * The default Wildfly {@link ComponentResolver}.
 *
 * @author Thomas.Diesler@jboss.com
 * @since 18-Myy-2013
 */
public class WildflyComponentResolver implements ComponentResolver {

    private static ComponentResolver defaultResolver = new DefaultComponentResolver();
    private final CamelComponentRegistry componentRegistry;

    public WildflyComponentResolver(CamelComponentRegistry componentRegistry) {
    	IllegalArgumentAssertion.assertNotNull(componentRegistry, "componentRegistry");
        this.componentRegistry = componentRegistry;
    }

    @Override
    public Component resolveComponent(String name, CamelContext context) throws Exception {

        // Try the default resolver
        Component component = defaultResolver.resolveComponent(name, context);
        if (component != null)
            return component;

        // Try registered {@link ComponentResolver} services
        ComponentResolver resolver = componentRegistry.getComponent(name);
        component = resolver != null ? resolver.resolveComponent(name, context) : null;

        return component;
    }

}
