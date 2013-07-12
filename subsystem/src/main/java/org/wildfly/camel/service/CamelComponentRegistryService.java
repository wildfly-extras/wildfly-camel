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

package org.wildfly.camel.service;

import static org.wildfly.camel.CamelLogger.LOGGER;
import static org.wildfly.camel.CamelMessages.MESSAGES;

import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Properties;
import java.util.Set;

import org.apache.camel.CamelContext;
import org.apache.camel.Component;
import org.apache.camel.impl.DefaultComponentResolver;
import org.apache.camel.spi.ComponentResolver;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleClassLoader;
import org.jboss.modules.ModuleLoadException;
import org.jboss.modules.Resource;
import org.jboss.modules.filter.PathFilter;
import org.jboss.modules.filter.PathFilters;
import org.jboss.msc.service.AbstractService;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceController.Mode;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.ValueService;
import org.jboss.msc.value.ImmediateValue;
import org.wildfly.camel.CamelComponentRegistry;
import org.wildfly.camel.CamelConstants;

/**
 * The {@link CamelComponentRegistry} service
 *
 * Ths implementation creates a jboss-msc {@link org.jboss.msc.service.Service}.
 *
 * JBoss services can create a dependency on the {@link ComponentResolver} service like this
 *
 * <code>
        ServiceName serviceName = CamelConstants.CAMEL_COMPONENT_BASE_NAME.append(componentName);
        builder.addDependency(serviceName, ComponentResolver.class, service.injectedComponentResolver);
 * </code>
 *
 * @author Thomas.Diesler@jboss.com
 * @since 05-Jul-2013
 */
public class CamelComponentRegistryService extends AbstractService<CamelComponentRegistry> {

    private CamelComponentRegistry componentRegistry;

    public static ServiceController<CamelComponentRegistry> addService(ServiceTarget serviceTarget, ServiceVerificationHandler verificationHandler) {
        CamelComponentRegistryService service = new CamelComponentRegistryService();
        ServiceBuilder<CamelComponentRegistry> builder = serviceTarget.addService(CamelConstants.CAMEL_COMPONENT_REGISTRY_NAME, service);
        builder.addListener(verificationHandler);
        return builder.install();
    }

    // Hide ctor
    private CamelComponentRegistryService() {
    }

    @Override
    public void start(StartContext startContext) throws StartException {
        final ServiceContainer serviceContainer = startContext.getController().getServiceContainer();
        final ServiceTarget serviceTarget = startContext.getChildTarget();
        componentRegistry = new DefaultCamelComponentRegistry(serviceContainer, serviceTarget);
    }

    @Override
    public CamelComponentRegistry getValue() {
        return componentRegistry;
    }

    class DefaultCamelComponentRegistry implements CamelComponentRegistry {

        private final ServiceContainer serviceContainer;
        private final ServiceTarget serviceTarget;

        DefaultCamelComponentRegistry(ServiceContainer serviceContainer, ServiceTarget serviceTarget) {
            this.serviceContainer = serviceContainer;
            this.serviceTarget = serviceTarget;
        }

        @Override
        public ComponentResolver getComponent(String name) {
            ServiceController<?> controller = serviceContainer.getService(getInternalServiceName(name));
            return controller != null ? (ComponentResolver) controller.getValue() : null;
        }

        @Override
        public Set<CamelComponentRegistration> registerComponents(Module module) {
            Set<CamelComponentRegistration> registrations = new HashSet<CamelComponentRegistration>();
            ModuleClassLoader classLoader = module.getClassLoader();

            // All of these approaches don't work
            // [MODULES-171] Cannot iterate over META-INF contents from imported modules
            //itres = module.iterateResources(PathFilters.getMetaInfFilter());
            //itres = module.iterateResources(PathFilters.isChildOf("META-INF/services/org/apache/camel/component"));
            //itres = module.iterateResources(PathFilters.isChildOf(DefaultComponentResolver.RESOURCE_PATH));
            //itres = classLoader.iterateResources(DefaultComponentResolver.RESOURCE_PATH, true);

            Iterator<Resource> itres;
            try {
                PathFilter filter = PathFilters.any(PathFilters.is("META-INF/services/org/apache/camel/component"), PathFilters.isChildOf("META-INF/services/org/apache/camel/component"));
                itres = module.iterateResources(filter);
            } catch (ModuleLoadException ex) {
                throw MESSAGES.cannotLoadComponentFromModule(ex, module.getIdentifier());
            }

            // Remove the trailing slash from DefaultComponentResolver.RESOURCE_PATH
            //Iterator<Resource> itres = classLoader.iterateResources("META-INF/services/org/apache/camel/component", true);
            while (itres.hasNext()) {
                Resource res = itres.next();
                String fullname = res.getName();
                if (!fullname.startsWith(DefaultComponentResolver.RESOURCE_PATH))
                    continue;

                // Load the component properties
                String cname = fullname.substring(fullname.lastIndexOf("/") + 1);
                Properties props = new Properties();
                try {
                    props.load(res.openStream());
                } catch (IOException ex) {
                    throw MESSAGES.cannotLoadComponentFromModule(ex, module.getIdentifier());
                }

                final Class<?> type;
                String className = props.getProperty("class");
                try {
                    type = classLoader.loadClass(className);
                } catch (Exception ex) {
                    throw MESSAGES.cannotLoadComponentType(ex, cname);
                }

                // Check component type
                if (Component.class.isAssignableFrom(type) == false)
                    throw MESSAGES.componentTypeException(type);

                // Register the ComponentResolver service
                ComponentResolver resolver = new ComponentResolver() {
                    @Override
                    public Component resolveComponent(String name, CamelContext context) throws Exception {
                        return (Component) context.getInjector().newInstance(type);
                    }
                };
                registrations.add(registerComponent(cname, resolver));
            }
            return registrations;
        }

        @Override
        public CamelComponentRegistration registerComponent(final String name, final ComponentResolver resolver) {
            if (getComponent(name) != null)
                throw MESSAGES.camelComponentAlreadyRegistered(name);

            LOGGER.infoRegisterCamelComponent(name);

            // Install the {@link ComponentResolver} as {@link Service}
            ValueService<ComponentResolver> service = new ValueService<ComponentResolver>(new ImmediateValue<ComponentResolver>(resolver));
            ServiceBuilder<ComponentResolver> builder = serviceTarget.addService(getInternalServiceName(name), service);
            final ServiceController<ComponentResolver> controller = builder.install();

            return new CamelComponentRegistration() {

                @Override
                public ComponentResolver getComponentResolver() {
                    return resolver;
                }

                @Override
                public void unregister() {
                    controller.setMode(Mode.REMOVE);
                }
            };
        }

        private ServiceName getInternalServiceName(String name) {
            return CamelConstants.CAMEL_COMPONENT_BASE_NAME.append(name);
        }
    }
}
