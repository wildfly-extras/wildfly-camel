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
import org.apache.camel.Component;
import org.apache.camel.impl.DefaultComponentResolver;
import org.apache.camel.spi.ComponentResolver;

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
        if (componentRegistry == null)
            throw CamelMessages.MESSAGES.illegalArgumentNull("componentRegistry");
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
