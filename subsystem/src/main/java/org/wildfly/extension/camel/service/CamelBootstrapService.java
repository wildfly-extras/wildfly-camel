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

package org.wildfly.extension.camel.service;

import static org.wildfly.extension.camel.CamelLogger.LOGGER;

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
import org.wildfly.extension.camel.CamelConstants;
import org.wildfly.extension.gravia.GraviaConstants;

/**
 * Service responsible for creating and managing the life-cycle of the Camel
 * subsystem.
 *
 * @author Thomas.Diesler@jboss.com
 * @since 19-Apr-2013
 */
public final class CamelBootstrapService extends AbstractService<Void> {

    private final InjectedValue<Environment> injectedEnvironment = new InjectedValue<Environment>();
    private final InjectedValue<Repository> injectedRepository = new InjectedValue<Repository>();

    public static ServiceController<Void> addService(ServiceTarget serviceTarget, ServiceVerificationHandler verificationHandler) {
        CamelBootstrapService service = new CamelBootstrapService();
        ServiceBuilder<Void> builder = serviceTarget.addService(CamelConstants.CAMEL_SUBSYSTEM_SERVICE_NAME, service);
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
        LOGGER.info("Activating Camel Subsystem");

        // Install camel features to the repository
        installRepositoryContent(startContext);
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
            } catch (IOException ex) {
                throw new IllegalStateException("Cannot install feature to repository: " + res.getName(), ex);
            }
        }
    }
}
