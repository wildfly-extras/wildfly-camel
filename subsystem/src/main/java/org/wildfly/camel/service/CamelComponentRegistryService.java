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
import org.jboss.modules.ModuleIdentifier;
import org.jboss.modules.ModuleLoadException;
import org.jboss.modules.ModuleLoader;
import org.jboss.modules.Resource;
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

    static final String[] componentNames = new String[] { "cxf", "jms", "jmx" };
    
    private CamelComponentRegistry componentRegistry;

    public static ServiceController<CamelComponentRegistry> addService(ServiceTarget serviceTarget, ServiceVerificationHandler verificationHandler) {
        CamelComponentRegistryService service = new CamelComponentRegistryService();
        ServiceBuilder<CamelComponentRegistry> builder = serviceTarget.addService(CamelConstants.CAMEL_COMPONENT_REGISTRY_SERVICE_NAME, service);
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

        // Register system components 
        ModuleLoader moduleLoader = Module.getBootModuleLoader();
        for (String compName : componentNames) {
            Module module;
            try {
                ModuleIdentifier cmpid = ModuleIdentifier.create("org.apache.camel.component." + compName);
                module = moduleLoader.loadModule(cmpid);
            } catch (ModuleLoadException ex) {
                throw new StartException(ex);
            }
            componentRegistry.registerComponents(module);
        }
    }

    @Override
    public CamelComponentRegistry getValue() {
        return componentRegistry;
    }

    static class DefaultCamelComponentRegistry implements CamelComponentRegistry {

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

            Iterator<Resource> itres;
            try {
                itres = module.iterateResources(PathFilters.getMetaInfServicesFilter());
            } catch (ModuleLoadException ex) {
                throw MESSAGES.cannotLoadComponentFromModule(ex, module.getIdentifier());
            }

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
                    ModuleClassLoader classLoader = module.getClassLoader();
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
