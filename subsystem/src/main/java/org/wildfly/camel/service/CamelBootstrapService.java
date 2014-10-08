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

package org.wildfly.camel.service;

import static org.wildfly.camel.CamelLogger.LOGGER;
import static org.wildfly.camel.CamelMessages.MESSAGES;

import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;

import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.gravia.repository.DefaultRepositoryXMLReader;
import org.jboss.gravia.repository.Repository;
import org.jboss.gravia.repository.RepositoryReader;
import org.jboss.gravia.resolver.Environment;
import org.jboss.gravia.resource.Resource;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleClassLoader;
import org.jboss.msc.service.AbstractService;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.value.InjectedValue;
import org.wildfly.camel.CamelComponentRegistry;
import org.wildfly.camel.CamelConstants;
import org.wildfly.extension.gravia.GraviaConstants;

/**
 * Service responsible for creating and managing the life-cycle of the Camel
 * subsystem.
 *
 * @author Thomas.Diesler@jboss.com
 * @since 19-Apr-2013
 */
public final class CamelBootstrapService extends AbstractService<Void> {

    private final InjectedValue<CamelComponentRegistry> injectedComponentRegistry = new InjectedValue<CamelComponentRegistry>();
    private final InjectedValue<Environment> injectedEnvironment = new InjectedValue<Environment>();
    private final InjectedValue<Repository> injectedRepository = new InjectedValue<Repository>();

    public static ServiceController<Void> addService(ServiceTarget serviceTarget, ServiceVerificationHandler verificationHandler) {
        CamelBootstrapService service = new CamelBootstrapService();
        ServiceBuilder<Void> builder = serviceTarget.addService(CamelConstants.CAMEL_SUBSYSTEM_SERVICE_NAME, service);
        builder.addDependency(CamelConstants.CAMEL_COMPONENT_REGISTRY_SERVICE_NAME, CamelComponentRegistry.class, service.injectedComponentRegistry);
        builder.addDependency(GraviaConstants.ENVIRONMENT_SERVICE_NAME, Environment.class, service.injectedEnvironment);
        builder.addDependency(GraviaConstants.REPOSITORY_SERVICE_NAME, Repository.class, service.injectedRepository);
        builder.addListener(verificationHandler);
        return builder.install();
    }

    // Hide ctor
    private CamelBootstrapService() {
    }

    @Override
    public void start(StartContext startContext) throws StartException {
        LOGGER.infoActivatingSubsystem();

        // Install camel features to the repository
        installRepositoryContent(startContext);

        // Register the statically configured components
        CamelComponentRegistry registry = injectedComponentRegistry.getValue();
        registry.registerComponents(Module.getCallerModule());
    }

    private void installRepositoryContent(StartContext startContext) throws StartException {

        Repository repository = injectedRepository.getValue();
        ModuleClassLoader classLoader = Module.getCallerModule().getClassLoader();
        Iterator<org.jboss.modules.Resource> itres = classLoader.iterateResources("META-INF/repository-content", false);
        while (itres.hasNext()) {
            org.jboss.modules.Resource res = itres.next();
            try {
                InputStream input = res.openStream();
                RepositoryReader reader = new DefaultRepositoryXMLReader(input);
                Resource auxres = reader.nextResource();
                while (auxres != null) {
                    if (repository.getResource(auxres.getIdentity()) == null) {
                        repository.addResource(auxres);
                    }
                    auxres = reader.nextResource();
                }
            } catch (IOException e) {
                throw MESSAGES.cannotInstallCamelFeature(res.getName());
            }
        }
    }
}
