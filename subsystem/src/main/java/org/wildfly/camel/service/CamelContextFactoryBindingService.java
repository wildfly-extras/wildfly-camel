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
import org.wildfly.camel.CamelContextFactory;

/**
 * The {@link CamelContextFactory} JNDI binding service
 *
 * @author Thomas.Diesler@jboss.com
 * @since 05-Jun-2013
 */
public final class CamelContextFactoryBindingService {

    public static ServiceController<?> addService(final ServiceTarget serviceTarget, ServiceVerificationHandler verificationHandler) {
        ContextNames.BindInfo bindInfo = ContextNames.bindInfoFor(CamelConstants.CAMEL_CONTEXT_FACTORY_BINDING_NAME);
        BinderService binderService = new BinderService(bindInfo.getBindName()) {
            @Override
            public synchronized void start(StartContext context) throws StartException {
                super.start(context);
                LOGGER.infoBoundNamingService(CamelConstants.CAMEL_CONTEXT_FACTORY_BINDING_NAME);
            }

            @Override
            public synchronized void stop(StopContext context) {
                super.stop(context);
                LOGGER.debug(CamelConstants.CAMEL_CONTEXT_FACTORY_BINDING_NAME);
            }
        };
        Injector<ManagedReferenceFactory> injector = binderService.getManagedObjectInjector();
        ServiceBuilder<?> builder = serviceTarget.addService(getBinderServiceName(), binderService);
        builder.addDependency(bindInfo.getParentContextServiceName(), ServiceBasedNamingStore.class, binderService.getNamingStoreInjector());
        builder.addDependency(CamelConstants.CAMEL_CONTEXT_FACTORY_NAME, CamelContextFactory.class, new ManagedReferenceInjector<CamelContextFactory>(injector));
        builder.addListener(verificationHandler);
        return builder.install();
    }

    private static ServiceName getBinderServiceName() {
        ContextNames.BindInfo bindInfo = ContextNames.bindInfoFor(CamelConstants.CAMEL_CONTEXT_FACTORY_BINDING_NAME);
        return bindInfo.getBinderServiceName();
    }
}