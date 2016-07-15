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

import static org.wildfly.extension.camel.CamelLogger.LOGGER;

import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.camel.component.undertow.HttpHandlerRegistrationInfo;
import org.apache.camel.component.undertow.UndertowHost;
import org.jboss.as.network.NetworkUtils;
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
import org.wildfly.extension.undertow.ListenerService;
import org.wildfly.extension.undertow.UndertowEventListener;
import org.wildfly.extension.undertow.UndertowService;

import io.undertow.Handlers;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.PathTemplateHandler;
import io.undertow.servlet.api.Deployment;
import io.undertow.util.HttpString;

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

    @SuppressWarnings("deprecation")
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

        URL result;
        try {
            String hostAddress = NetworkUtils.formatPossibleIpv6Address(address.getHostAddress());
            result = new URL(socketBinding.getName() + "://" + hostAddress + ":" + socketBinding.getPort());
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
        private static final String REST_PATH_PLACEHOLDER = "{";
        private final Map<String, DelegatingHttpHandler> handlers = new HashMap<>();
        private final Host defaultHost;

        WildFlyUndertowHost(Host host) {
            this.defaultHost = host;
        }

        @Override
        public void validateEndpointURI(URI httpURI) {
            // Camel HTTP endpoint port defaults are 0 or -1
            boolean portMatched = httpURI.getPort() == 0 || httpURI.getPort() == -1;

            // If a port was specified, verify that undertow has a listener configured for it
            if (!portMatched) {
                for (ListenerService<?> service : defaultHost.getServer().getListeners()) {
                    InjectedValue<SocketBinding> binding = service.getBinding();
                    if (binding != null) {
                        if (binding.getValue().getPort() == httpURI.getPort()) {
                            portMatched = true;
                            break;
                        }
                    }
                }
            }
            
            if (!"localhost".equals(httpURI.getHost())) 
                LOGGER.warn("Cannot bind to host other than 'localhost': {}", httpURI);
            if (!portMatched) 
                LOGGER.warn("Cannot bind to specific port: {}", httpURI);
        }

        @Override
		public void registerHandler(HttpHandlerRegistrationInfo reginfo, HttpHandler handler) {
        	String path = reginfo.getUri().getPath();
            if (path.contains(REST_PATH_PLACEHOLDER)) {
                String pathPrefix = path.substring(0, path.indexOf(REST_PATH_PLACEHOLDER));
                String remaining = path.substring(path.indexOf(REST_PATH_PLACEHOLDER));

                PathTemplateHandler pathTemplateHandler = Handlers.pathTemplate();
                pathTemplateHandler.add(remaining, handler);

                handler = pathTemplateHandler;
                path = pathPrefix;
            } else if (!reginfo.isMatchOnUriPrefix()) {
                handler = Handlers.path(handler);
            }
            DelegatingHttpHandler delhandler = handlers.get(path);
            if (delhandler == null) {
                delhandler = new DelegatingHttpHandler();
                defaultHost.registerHandler(path, delhandler);
                handlers.put(path, delhandler);
            }
            delhandler.addDelegate(reginfo, handler);
        }

		@Override
		public void unregisterHandler(HttpHandlerRegistrationInfo reginfo) {
        	String path = reginfo.getUri().getPath();
            if (path.contains(REST_PATH_PLACEHOLDER)) {
                path = path.substring(0, path.indexOf(REST_PATH_PLACEHOLDER));
            }
            // This will remove the handlers for all methods
            defaultHost.unregisterHandler(path);
            handlers.remove(path);
        }
    }

    class DelegatingHttpHandler implements HttpHandler {
        
        private Map<String, HttpHandler> delegates = new HashMap<>();
        
        void addDelegate(HttpHandlerRegistrationInfo reginfo, HttpHandler handler) {
            String methodRestrict = reginfo.getMethodRestrict();
            if (methodRestrict != null) {
                for (String method: methodRestrict.split(",")) {
                    checkedPut(method.trim(), handler);
                }
            } else {
                checkedPut("ALL", handler);
            }
        }

        private void checkedPut(String method, HttpHandler handler) {
            HttpHandler prev = delegates.put(method, handler);
            IllegalStateAssertion.assertNull(prev, "Handler for " + method + " already registered");
        }
        
        @Override
        public void handleRequest(HttpServerExchange exchange) throws Exception {
            HttpString method = exchange.getRequestMethod();
            HttpHandler delegate = delegates.get(method.toString());
            if (delegate == null) {
                delegate = delegates.get("ALL");
            }
            IllegalStateAssertion.assertNotNull(delegate, "Cannot obtain handler for method: " + method);
            delegate.handleRequest(exchange);
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
