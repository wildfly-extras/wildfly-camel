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

import org.jboss.modules.ModuleClassLoader;
import org.jboss.msc.service.AbstractService;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.wildfly.camel.utils.IllegalArgumentAssertion;
import org.wildfly.extension.camel.CamelConstants;
import org.wildfly.extension.camel.CamelContextFactory;
import org.wildfly.extension.camel.WildFlyCamelContext;
import org.wildfly.extension.camel.handler.ModuleClassLoaderAssociationHandler;

/**
 * The {@link CamelContextFactory} service
 *
 * @author Thomas.Diesler@jboss.com
 * @since 05-Jun-2013
 */
public class CamelContextFactoryService extends AbstractService<CamelContextFactory> {

    private CamelContextFactory contextFactory;

    public static ServiceController<CamelContextFactory> addService(ServiceTarget serviceTarget) {
        CamelContextFactoryService service = new CamelContextFactoryService();
        ServiceBuilder<CamelContextFactory> builder = serviceTarget.addService(CamelConstants.CAMEL_CONTEXT_FACTORY_SERVICE_NAME, service);
        return builder.install();
    }

    // Hide ctor
    private CamelContextFactoryService() {
    }

    @Override
    public void start(StartContext startContext) throws StartException {
        contextFactory = new CamelContextFactoryImpl();
	}

	@Override
    public CamelContextFactory getValue() {
        return contextFactory;
    }

    static final class CamelContextFactoryImpl implements CamelContextFactory {

        @Override
        public WildFlyCamelContext createCamelContext() throws Exception {
            return new WildFlyCamelContext();
        }

        @Override
        public WildFlyCamelContext createCamelContext(ClassLoader classLoader) throws Exception {
            IllegalArgumentAssertion.assertTrue(classLoader instanceof ModuleClassLoader, "ModuleClassLoader required: " + classLoader);
            ModuleClassLoaderAssociationHandler.associate((ModuleClassLoader) classLoader);
            try {
                return new WildFlyCamelContext();
            } finally {
                ModuleClassLoaderAssociationHandler.disassociate();
            }
        }
    }
}
