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

import static org.wildfly.camel.CamelMessages.MESSAGES;

import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;

import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.gravia.provision.DefaultEnvironment;
import org.jboss.gravia.provision.Environment;
import org.jboss.gravia.repository.DefaultRepositoryXMLReader;
import org.jboss.gravia.repository.RepositoryReader;
import org.jboss.gravia.resource.Capability;
import org.jboss.gravia.resource.DefaultResourceBuilder;
import org.jboss.gravia.resource.Requirement;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleClassLoader;
import org.jboss.modules.Resource;
import org.jboss.msc.service.AbstractService;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.wildfly.camel.CamelConstants;

/**
 * Service responsible for preloading the {@link XRepository}.
 *
 * @author Thomas.Diesler@jboss.com
 * @since 27-Jun-2013
 */
public class EnvironmentService extends AbstractService<Environment> {

    private Environment environment;

    public static ServiceController<Environment> addService(ServiceTarget serviceTarget, ServiceVerificationHandler verificationHandler) {
        EnvironmentService service = new EnvironmentService();
        ServiceBuilder<Environment> builder = serviceTarget.addService(CamelConstants.ENVIRONMENT_SERVICE_NAME, service);
        builder.addListener(verificationHandler);
        return builder.install();
    }

    // Hide ctor
    private EnvironmentService() {
    }

    @Override
    public void start(StartContext startContext) throws StartException {

        environment = new DefaultEnvironment("WildFly Environment");

        // Install camel features to the repository
        ModuleClassLoader classLoader = Module.getCallerModule().getClassLoader();
        Iterator<Resource> itres = classLoader.iterateResources("META-INF/environment-content", false);
        while(itres.hasNext()) {
            Resource modres = itres.next();
            try {
                InputStream input = modres.openStream();
                RepositoryReader reader = new DefaultRepositoryXMLReader(input);
                org.jboss.gravia.resource.Resource xmlres = reader.nextResource();
                while (xmlres != null) {
                    if (environment.getResource(xmlres.getIdentity()) == null) {
                        DefaultResourceBuilder builder = new DefaultResourceBuilder();
                        for (Capability cap : xmlres.getCapabilities(null)) {
                            builder.addCapability(cap.getNamespace(), cap.getAttributes(), cap.getDirectives());
                        }
                        for (Requirement req : xmlres.getRequirements(null)) {
                            builder.addCapability(req.getNamespace(), req.getAttributes(), req.getDirectives());
                        }
                        environment.addResource(builder.getResource());
                    }
                    xmlres = reader.nextResource();
                }
            } catch (IOException e) {
                throw MESSAGES.cannotInstallResourceToEnvironment(modres.getName());
            }
        }
    }

    @Override
    public Environment getValue() throws IllegalStateException {
        return environment;
    }
}