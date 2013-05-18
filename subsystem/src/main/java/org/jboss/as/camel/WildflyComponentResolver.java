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
package org.jboss.as.camel;

import static org.jboss.as.camel.CamelMessages.MESSAGES;

import java.io.IOException;
import java.net.URL;
import java.util.Properties;

import org.apache.camel.CamelContext;
import org.apache.camel.Component;
import org.apache.camel.impl.DefaultComponentResolver;
import org.apache.camel.spi.ComponentResolver;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleClassLoader;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.modules.ModuleLoadException;
import org.jboss.modules.ModuleLoader;

/**
 * The default Wildfly {@link ComponentResolver}.
 *
 * @author Thomas.Diesler@jboss.com
 * @since 18-Myy-2013
 */
public class WildflyComponentResolver implements ComponentResolver {

    // [TODO] make this configurable
    private static String[] modulePrefixes = new String[] { "org.apache.camel.component", "org.jboss.as.camel.component" };

    private static ComponentResolver defaultResolver = new DefaultComponentResolver();
    private static ModuleLoader moduleLoader;
    static {
        ModuleClassLoader classLoader = (ModuleClassLoader) WildflyComponentResolver.class.getClassLoader();
        moduleLoader = classLoader.getModule().getModuleLoader();
    }

    @Override
    public Component resolveComponent(String name, CamelContext context) throws Exception {

        // Try the default resolver
        Component component = defaultResolver.resolveComponent(name, context);
        if (component != null)
            return component;

        Module module = null;
        for (String prefix : modulePrefixes) {
            ModuleIdentifier moduleid = ModuleIdentifier.create(prefix + "." + name);
            try {
                module = moduleLoader.loadModule(moduleid);
                break;
            } catch (ModuleLoadException ex) {
                // ignore
            }
        }
        if (module != null) {
            ModuleClassLoader classLoader = module.getClassLoader();
            String uri = DefaultComponentResolver.RESOURCE_PATH + name;
            URL resource = module.getExportedResource(uri);
            if (resource == null)
                throw MESSAGES.cannotFindComponentProperties(module.getIdentifier());

            // Load the component properties
            Properties props = new Properties();
            try {
                props.load(resource.openStream());
            } catch (IOException ex) {
                throw MESSAGES.cannotLoadComponentProperties(ex, module.getIdentifier());
            }

            Class<?> type;
            String className = props.getProperty("class");
            try {
                type = classLoader.loadClass(className);
            } catch (Exception ex) {
                throw MESSAGES.cannotLoadComponentType(ex, name);
            }

            // create the component
            if (Component.class.isAssignableFrom(type)) {
                component = (Component) context.getInjector().newInstance(type);
            } else {
                throw MESSAGES.componentTypeException(type);
            }
        }
        return component;
    }

}
