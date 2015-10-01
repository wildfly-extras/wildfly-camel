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

package org.wildfly.extension.camel.undertow;

import org.jboss.gravia.runtime.ModuleContext;
import org.jboss.gravia.runtime.Runtime;
import org.jboss.gravia.runtime.ServiceRegistration;
import org.jboss.msc.service.AbstractService;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.wildfly.extension.camel.CamelConstants;
import org.wildfly.extension.gravia.GraviaConstants;
import org.wildfly.extension.undertow.Host;
import org.wildfly.extension.undertow.UndertowService;

/**
 * A service that registers the default undertow {@link Host}
 *
 * @author Thomas.Diesler@jboss.com
 * @since 19-Apr-2013
 */
public class UndertowHostService extends AbstractService<Host> {

    private static final ServiceName SERVICE_NAME = CamelConstants.CAMEL_BASE_NAME.append("undertow", "host");

    private final InjectedValue<Host> injectedDefaultHost = new InjectedValue<>();
    private final InjectedValue<Runtime> injectedRuntime = new InjectedValue<Runtime>();

    private ServiceRegistration<Host> registration;

    public static ServiceController<Host> addService(ServiceTarget serviceTarget) {
        UndertowHostService service = new UndertowHostService();
        ServiceBuilder<Host> builder = serviceTarget.addService(SERVICE_NAME, service);
        builder.addDependency(GraviaConstants.RUNTIME_SERVICE_NAME, Runtime.class, service.injectedRuntime);
        builder.addDependency(UndertowService.virtualHostName("default-server", "default-host"), Host.class, service.injectedDefaultHost);
        return builder.install();
    }

    // Hide ctor
    private UndertowHostService() {
    }

    @Override
    public void start(StartContext startContext) throws StartException {
        ModuleContext syscontext = injectedRuntime.getValue().getModuleContext();
        registration = syscontext.registerService(Host.class, injectedDefaultHost.getValue(), null);
    }

    @Override
    public void stop(StopContext context) {
        if (registration != null) {
            registration.unregister();
        }
    }

    @Override
    public Host getValue() throws IllegalStateException {
        return injectedDefaultHost.getValue();
    }
}
