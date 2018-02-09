/*
 * #%L
 * Wildfly Camel :: Testsuite
 * %%
 * Copyright (C) 2013 - 2018 RedHat
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
package org.wildfly.camel.test.spring.subE;

import org.jboss.as.naming.ImmediateManagedReferenceFactory;
import org.jboss.as.naming.ManagedReferenceFactory;
import org.jboss.as.naming.ServiceBasedNamingStore;
import org.jboss.as.naming.deployment.ContextNames;
import org.jboss.as.naming.deployment.ContextNames.BindInfo;
import org.jboss.msc.service.ServiceActivator;
import org.jboss.msc.service.ServiceActivatorContext;
import org.jboss.msc.service.ServiceRegistryException;
import org.jboss.msc.service.ServiceTarget;

public class DelayedBinderServiceActivator implements ServiceActivator {

    public DelayedBinderServiceActivator() {
    }

    @Override
    public void activate(ServiceActivatorContext context) throws ServiceRegistryException {
        BindInfo bindInfo = ContextNames.bindInfoFor("java:/spring/binding/test");
        DelayedBinderService service = new DelayedBinderService(bindInfo.getBindName());
        ManagedReferenceFactory managedReferenceFactory = new ImmediateManagedReferenceFactory(service);

        ServiceTarget serviceTarget = context.getServiceTarget();
        serviceTarget.addService(bindInfo.getBinderServiceName(), service)
            .addInjection(service.getManagedObjectInjector(), managedReferenceFactory)
            .addDependency(bindInfo.getParentContextServiceName(), ServiceBasedNamingStore.class, service.getNamingStoreInjector())
            .install();
    }
}
