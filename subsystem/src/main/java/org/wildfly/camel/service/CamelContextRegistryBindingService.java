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

import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.naming.ManagedReferenceFactory;
import org.jboss.as.naming.ManagedReferenceInjector;
import org.jboss.as.naming.ServiceBasedNamingStore;
import org.jboss.as.naming.deployment.ContextNames;
import org.jboss.as.naming.service.BinderService;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.wildfly.camel.CamelConstants;
import org.wildfly.camel.CamelContextRegistry;

/**
 * The {@link CamelContextRegistry} JNDI binding service
 *
 * @author Thomas.Diesler@jboss.com
 * @since 05-Jun-2013
 */
public final class CamelContextRegistryBindingService {

    public static ServiceController<?> addService(ServiceTarget serviceTarget, ServiceVerificationHandler verificationHandler) {
        final ContextNames.BindInfo bindInfo = getBindInfo();
        BinderService binderService = new BinderService(bindInfo.getBindName()) {
            @Override
            public synchronized void start(StartContext context) throws StartException {
                super.start(context);
                LOGGER.infoBoundCamelNamingObject(bindInfo.getAbsoluteJndiName());
            }

            @Override
            public synchronized void stop(StopContext context) {
                LOGGER.debugf("Unbind camel jndi name: %s", bindInfo.getAbsoluteJndiName());
                super.stop(context);
            }
        };
        Injector<ManagedReferenceFactory> injector = binderService.getManagedObjectInjector();
        ServiceBuilder<?> builder = serviceTarget.addService(bindInfo.getBinderServiceName(), binderService);
        builder.addDependency(bindInfo.getParentContextServiceName(), ServiceBasedNamingStore.class, binderService.getNamingStoreInjector());
        builder.addDependency(CamelConstants.CAMEL_CONTEXT_REGISTRY_SERVICE_NAME, CamelContextRegistry.class, new ManagedReferenceInjector<CamelContextRegistry>(injector));
        builder.addListener(verificationHandler);
        return builder.install();
    }

    static ServiceName getBinderServiceName() {
        return getBindInfo().getBinderServiceName();
    }

    static ContextNames.BindInfo getBindInfo() {
        return ContextNames.bindInfoFor(CamelConstants.CAMEL_CONTEXT_REGISTRY_BINDING_NAME);
    }
}