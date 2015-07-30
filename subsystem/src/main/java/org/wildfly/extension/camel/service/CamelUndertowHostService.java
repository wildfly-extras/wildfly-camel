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

import io.undertow.server.HttpHandler;

import java.net.URI;

import org.apache.camel.component.undertow.UndertowHost;
import org.jboss.gravia.runtime.ModuleContext;
import org.jboss.gravia.runtime.Runtime;
import org.jboss.gravia.runtime.ServiceRegistration;
import org.jboss.gravia.utils.IllegalStateAssertion;
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
import org.wildfly.extension.camel.CamelContextRegistry;
import org.wildfly.extension.gravia.GraviaConstants;
import org.wildfly.extension.undertow.Host;
import org.wildfly.extension.undertow.UndertowService;

/**
 * The {@link CamelContextRegistry} service
 *
 * @author Thomas.Diesler@jboss.com
 * @since 19-Apr-2013
 */
public class CamelUndertowHostService extends AbstractService<UndertowHost> {

    private static final ServiceName SERVICE_NAME = CamelConstants.CAMEL_BASE_NAME.append("Undertow");

    private final InjectedValue<Host> injectedDefaultHost = new InjectedValue<>();
    private final InjectedValue<Runtime> injectedRuntime = new InjectedValue<Runtime>();

    private ServiceRegistration<UndertowHost> registration;
    private UndertowHost undertowHost;

    public static ServiceController<UndertowHost> addService(ServiceTarget serviceTarget) {
        CamelUndertowHostService service = new CamelUndertowHostService();
        ServiceBuilder<UndertowHost> builder = serviceTarget.addService(SERVICE_NAME, service);
        builder.addDependency(GraviaConstants.RUNTIME_SERVICE_NAME, Runtime.class, service.injectedRuntime);
        builder.addDependency(UndertowService.virtualHostName("default-server", "default-host"), Host.class, service.injectedDefaultHost);
        return builder.install();
    }

    // Hide ctor
    private CamelUndertowHostService() {
    }

    @Override
    public void start(StartContext startContext) throws StartException {
        undertowHost = new WildflyUndertowHost(injectedDefaultHost.getValue());
        ModuleContext syscontext = injectedRuntime.getValue().getModuleContext();
        registration = syscontext.registerService(UndertowHost.class, undertowHost, null);
    }

    @Override
    public void stop(StopContext context) {
        if (registration != null) {
            registration.unregister();
        }
    }

    @Override
    public UndertowHost getValue() throws IllegalStateException {
        return undertowHost;
    }

    class WildflyUndertowHost implements UndertowHost {

        private final Host defaultHost;

        WildflyUndertowHost(Host host) {
            this.defaultHost = host;
        }

        @Override
        public void validateEndpointURI(URI httpURI) {
            IllegalStateAssertion.assertEquals("localhost", httpURI.getHost(), "Cannot bind to host other than 'localhost': " + httpURI);
            IllegalStateAssertion.assertEquals(-1, httpURI.getPort(), "Cannot bind to specific port: " + httpURI);
        }

        @Override
        public void registerHandler(String path, HttpHandler handler) {
            defaultHost.registerHandler(path, handler);
        }

        @Override
        public void unregisterHandler(String path) {
            defaultHost.unregisterHandler(path);
        }
    }
}
