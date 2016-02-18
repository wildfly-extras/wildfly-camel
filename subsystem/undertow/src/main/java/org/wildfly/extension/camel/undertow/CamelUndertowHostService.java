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

import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;

import org.apache.camel.component.undertow.UndertowHost;
import org.jboss.as.network.SocketBinding;
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
import org.wildfly.extension.camel.parser.SubsystemRuntimeState;
import org.wildfly.extension.gravia.GraviaConstants;
import org.wildfly.extension.undertow.AbstractUndertowEventListener;
import org.wildfly.extension.undertow.Host;
import org.wildfly.extension.undertow.UndertowEventListener;
import org.wildfly.extension.undertow.UndertowService;

import io.undertow.server.HttpHandler;
import io.undertow.servlet.api.Deployment;

/**
 * The {@link UndertowHost} service
 *
 * @author Thomas.Diesler@jboss.com
 * @since 19-Apr-2013
 */
public class CamelUndertowHostService extends AbstractService<UndertowHost> {

    private static final ServiceName SERVICE_NAME = CamelConstants.CAMEL_BASE_NAME.append("Undertow");

    private final InjectedValue<SocketBinding> injectedHttpSocketBinding = new InjectedValue<>();
    private final InjectedValue<UndertowService> injectedUndertowService = new InjectedValue<>();
    private final InjectedValue<Host> injectedDefaultHost = new InjectedValue<>();
    private final InjectedValue<Runtime> injectedRuntime = new InjectedValue<Runtime>();

    private final SubsystemRuntimeState runtimeState;
    private ServiceRegistration<UndertowHost> registration;
    private UndertowEventListener eventListener;
    private UndertowHost undertowHost;

    public static ServiceController<UndertowHost> addService(ServiceTarget serviceTarget, SubsystemRuntimeState runtimeState) {
        CamelUndertowHostService service = new CamelUndertowHostService(runtimeState);
        ServiceBuilder<UndertowHost> builder = serviceTarget.addService(SERVICE_NAME, service);
        builder.addDependency(GraviaConstants.RUNTIME_SERVICE_NAME, Runtime.class, service.injectedRuntime);
        builder.addDependency(UndertowService.UNDERTOW, UndertowService.class, service.injectedUndertowService);
        builder.addDependency(SocketBinding.JBOSS_BINDING_NAME.append("http"), SocketBinding.class, service.injectedHttpSocketBinding);
        builder.addDependency(UndertowService.virtualHostName("default-server", "default-host"), Host.class, service.injectedDefaultHost);
        return builder.install();
    }

    // Hide ctor
    private CamelUndertowHostService(SubsystemRuntimeState runtimeState) {
        this.runtimeState = runtimeState;
    }

    @Override
    public void start(StartContext startContext) throws StartException {
        runtimeState.setHttpHost(getConnectionURL());
        eventListener = new CamelUndertowEventListener();
        injectedUndertowService.getValue().registerListener(eventListener);
        undertowHost = new WildFlyUndertowHost(injectedDefaultHost.getValue());
        ModuleContext syscontext = injectedRuntime.getValue().getModuleContext();
        registration = syscontext.registerService(UndertowHost.class, undertowHost, null);
    }

    private URL getConnectionURL() throws StartException {
        SocketBinding socketBinding = injectedHttpSocketBinding.getValue();
        InetAddress address = socketBinding.getNetworkInterfaceBinding().getAddress();

        /* Derive the address from network interfaces
        if (address.getHostAddress().equals("127.0.0.1")) {
            InetAddress derived = null;
            try {
                List<NetworkInterface> nets = Collections.list(NetworkInterface.getNetworkInterfaces());
                for (int i = 0; derived == null && i < nets.size(); i++) {
                    for (InetAddress aux : Collections.list(nets.get(i).getInetAddresses())) {
                        if (!aux.getHostAddress().equals("127.0.0.1")) {
                            derived = aux;
                            break;
                        }
                    }
                }
            } catch (SocketException ex) {
                throw new StartException(ex);
            }
            IllegalStateAssertion.assertNotNull(derived, "Cannot derive internet address from interfaces");
            address = derived;
        }
        */

        URL result;
        try {
            result = new URL(socketBinding.getName() + "://" + address.getHostAddress() + ":" + socketBinding.getPort());
        } catch (MalformedURLException ex) {
            throw new StartException(ex);
        }
        return result;
    }

    @Override
    public void stop(StopContext context) {
        injectedUndertowService.getValue().unregisterListener(eventListener);
        registration.unregister();
    }

    @Override
    public UndertowHost getValue() throws IllegalStateException {
        return undertowHost;
    }

    class WildFlyUndertowHost implements UndertowHost {

        private final Host defaultHost;

        WildFlyUndertowHost(Host host) {
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

    class CamelUndertowEventListener extends AbstractUndertowEventListener {

        @Override
        public void onDeploymentStart(Deployment dep, Host host) {
            runtimeState.addHttpContext(dep.getServletContext().getContextPath());
        }

        @Override
        public void onDeploymentStop(Deployment dep, Host host) {
            runtimeState.removeHttpContext(dep.getServletContext().getContextPath());
        }
    }
}
