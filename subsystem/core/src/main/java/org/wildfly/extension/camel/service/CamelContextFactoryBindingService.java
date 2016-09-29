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

import org.jboss.as.naming.ManagedReferenceFactory;
import org.jboss.as.naming.ManagedReferenceInjector;
import org.jboss.as.naming.ServiceBasedNamingStore;
import org.jboss.as.naming.deployment.ContextNames;
import org.jboss.as.naming.service.BinderService;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.wildfly.extension.camel.CamelConstants;
import org.wildfly.extension.camel.CamelContextFactory;

/**
 * The {@link CamelContextFactory} JNDI binding service
 *
 * @author Thomas.Diesler@jboss.com
 * @since 05-Jun-2013
 */
public final class CamelContextFactoryBindingService {

    public static ServiceController<?> addService(final ServiceTarget serviceTarget) {
        final ContextNames.BindInfo bindInfo = ContextNames.bindInfoFor(CamelConstants.CAMEL_CONTEXT_FACTORY_BINDING_NAME);
        BinderService binderService = new BinderService(bindInfo.getBindName()) {
            @Override
            public synchronized void start(StartContext context) throws StartException {
                super.start(context);
                LOGGER.info("Bound camel naming object: {}", bindInfo.getAbsoluteJndiName());
            }

            @Override
            public synchronized void stop(StopContext context) {
                LOGGER.debug("Unbind camel naming object: {}", bindInfo.getAbsoluteJndiName());
                super.stop(context);
            }
        };
        Injector<ManagedReferenceFactory> injector = binderService.getManagedObjectInjector();
        ServiceBuilder<?> builder = serviceTarget.addService(bindInfo.getBinderServiceName(), binderService);
        builder.addDependency(bindInfo.getParentContextServiceName(), ServiceBasedNamingStore.class, binderService.getNamingStoreInjector());
        builder.addDependency(CamelConstants.CAMEL_CONTEXT_FACTORY_SERVICE_NAME, CamelContextFactory.class, new ManagedReferenceInjector<CamelContextFactory>(injector));
        return builder.install();
    }
}
