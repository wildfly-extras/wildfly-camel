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

import org.apache.camel.CamelContext;
import org.apache.camel.Component;
import org.apache.camel.impl.DefaultComponentResolver;
import org.apache.camel.spi.ComponentResolver;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

/**
 * The default Wildfly {@link ComponentResolver}.
 *
 * @author Thomas.Diesler@jboss.com
 * @since 18-Myy-2013
 */
public class WildflyComponentResolver implements ComponentResolver {

    private static ComponentResolver defaultResolver = new DefaultComponentResolver();
    private final BundleContext syscontext;

    public WildflyComponentResolver(BundleContext syscontext) {
        if (syscontext == null)
            throw CamelMessages.MESSAGES.illegalArgumentNull("syscontext");
        this.syscontext = syscontext;
    }

    @Override
    public Component resolveComponent(String name, CamelContext context) throws Exception {

        // Try the default resolver
        Component component = defaultResolver.resolveComponent(name, context);
        if (component != null)
            return component;

        // Try registered {@link ComponentResolver} services
        for (ServiceReference<ComponentResolver> sref : syscontext.getServiceReferences(ComponentResolver.class, "(component=" + name + ")")) {
            ComponentResolver resolver = syscontext.getService(sref);
            component = resolver.resolveComponent(name, context);
            break;
        }

        return component;
    }

}
