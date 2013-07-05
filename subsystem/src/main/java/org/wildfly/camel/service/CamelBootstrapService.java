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

import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.modules.ModuleLoadException;
import org.jboss.modules.ModuleLoader;
import org.jboss.msc.service.AbstractService;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.value.InjectedValue;
import org.wildfly.camel.CamelComponentRegistry;
import org.wildfly.camel.CamelConstants;

/**
 * Service responsible for creating and managing the life-cycle of the Camel subsystem.
 *
 * @author Thomas.Diesler@jboss.com
 * @since 19-Apr-2013
 */
public class CamelBootstrapService extends AbstractService<Void> {

    private final InjectedValue<CamelComponentRegistry> injectedComponentRegistry = new InjectedValue<CamelComponentRegistry>();

    public static ServiceController<Void> addService(ServiceTarget serviceTarget, ServiceVerificationHandler verificationHandler) {
        CamelBootstrapService service = new CamelBootstrapService();
        ServiceBuilder<Void> builder = serviceTarget.addService(CamelConstants.CAMEL_SUBSYSTEM_NAME, service);
        builder.addDependency(CamelConstants.CAMEL_COMPONENT_REGISTRY_NAME, CamelComponentRegistry.class, service.injectedComponentRegistry);
        builder.addListener(verificationHandler);
        return builder.install();
    }

    // Hide ctor
    private CamelBootstrapService() {
    }

    @Override
    public void start(StartContext startContext) throws StartException {
        LOGGER.infoActivatingSubsystem();

        // Register the statically configured components
        for (String comp : new String[] { "cxf", "jms", "jmx" }) {
            Module module;
            try {
                ModuleLoader moduleLoader = Module.getCallerModuleLoader();
                module = moduleLoader.loadModule(ModuleIdentifier.fromString("org.apache.camel.component." + comp));
            } catch (ModuleLoadException ex) {
                throw new IllegalStateException(ex);
            }
            CamelComponentRegistry registry = injectedComponentRegistry.getValue();
            registry.registerComponents(module);
        }
    }
}
