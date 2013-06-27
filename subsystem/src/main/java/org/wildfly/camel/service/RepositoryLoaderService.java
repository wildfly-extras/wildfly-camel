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

import static org.wildfly.camel.CamelMessages.MESSAGES;

import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;

import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.osgi.OSGiConstants;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleClassLoader;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.modules.Resource;
import org.jboss.msc.service.AbstractService;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.value.InjectedValue;
import org.jboss.osgi.repository.RepositoryReader;
import org.jboss.osgi.repository.RepositoryStorage;
import org.jboss.osgi.repository.RepositoryXMLReader;
import org.jboss.osgi.repository.XRepository;
import org.jboss.osgi.resolver.XIdentityCapability;
import org.jboss.osgi.resolver.XRequirement;
import org.jboss.osgi.resolver.XRequirementBuilder;
import org.jboss.osgi.resolver.XResource;
import org.wildfly.camel.CamelConstants;

/**
 * Service responsible for preloading the {@link XRepository}.
 *
 * @author Thomas.Diesler@jboss.com
 * @since 27-Jun-2013
 */
public class RepositoryLoaderService extends AbstractService<Void> {

    private final InjectedValue<XRepository> injectedRepository = new InjectedValue<XRepository>();

    public static ServiceController<Void> addService(ServiceTarget serviceTarget, ServiceVerificationHandler verificationHandler) {
        RepositoryLoaderService service = new RepositoryLoaderService();
        ServiceBuilder<Void> builder = serviceTarget.addService(CamelConstants.REPOSITORY_LOADER_NAME, service);
        builder.addDependency(OSGiConstants.REPOSITORY_SERVICE_NAME, XRepository.class, service.injectedRepository);
        builder.addListener(verificationHandler);
        return builder.install();
    }

    // Hide ctor
    private RepositoryLoaderService() {
    }

    @Override
    public void start(StartContext startContext) throws StartException {

        XRepository repository = injectedRepository.getValue();
        RepositoryStorage storage = repository.adapt(RepositoryStorage.class);

        // Install camel features to the repository
        ModuleClassLoader classLoader = Module.getCallerModule().getClassLoader();
        Iterator<Resource> itres = classLoader.iterateResources("META-INF/repository-content", false);
        while(itres.hasNext()) {
            Resource res = itres.next();
            try {
                InputStream input = res.openStream();
                RepositoryReader reader = RepositoryXMLReader.create(input);
                XResource auxres = reader.nextResource();
                while (auxres != null) {
                    XIdentityCapability icap = auxres.getIdentityCapability();
                    if (XResource.MODULE_IDENTITY_NAMESPACE.equals(icap.getNamespace())) {
                        ModuleIdentifier moduleId = ModuleIdentifier.fromString(icap.getName());
                        XRequirement req = XRequirementBuilder.create(moduleId).getRequirement();
                        repository.findProviders(req);
                    } else if (storage.getResource(icap) == null) {
                        storage.addResource(auxres);
                    }
                    auxres = reader.nextResource();
                }
            } catch (IOException e) {
                throw MESSAGES.cannotInstallCamelFeature(res.getName());
            }
        }
    }
}
