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
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

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
import org.wildfly.extension.camel.parser.SubsystemState.RuntimeState;
import org.wildfly.extension.gravia.GraviaConstants;
import org.wildfly.extension.undertow.Host;
import org.wildfly.extension.undertow.ListenerService;
import org.wildfly.extension.undertow.UndertowEventListener;
import org.wildfly.extension.undertow.UndertowService;

import io.undertow.Handlers;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.RoutingHandler;
import io.undertow.servlet.api.Deployment;
import io.undertow.util.PathTemplate;
import io.undertow.util.URLUtils;

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

    private final RuntimeState runtimeState;
    private ServiceRegistration<UndertowHost> registration;
    private UndertowEventListener eventListener;
    private UndertowHost undertowHost;

    @SuppressWarnings("deprecation")
    public static ServiceController<UndertowHost> addService(ServiceTarget serviceTarget, RuntimeState runtimeState) {
        CamelUndertowHostService service = new CamelUndertowHostService(runtimeState);
        ServiceBuilder<UndertowHost> builder = serviceTarget.addService(SERVICE_NAME, service);
        builder.addDependency(GraviaConstants.RUNTIME_SERVICE_NAME, Runtime.class, service.injectedRuntime);
        builder.addDependency(UndertowService.UNDERTOW, UndertowService.class, service.injectedUndertowService);
        builder.addDependency(SocketBinding.JBOSS_BINDING_NAME.append("http"), SocketBinding.class, service.injectedHttpSocketBinding);
        builder.addDependency(UndertowService.virtualHostName("default-server", "default-host"), Host.class, service.injectedDefaultHost);
        return builder.install();
    }

    // Hide ctor
    private CamelUndertowHostService(RuntimeState runtimeState) {
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
        private static final String DEFAULT_METHODS = "GET,HEAD,POST,PUT,DELETE,TRACE,OPTIONS,CONNECT,PATCH";
        private final Map<String, DelegatingRoutingHandler> handlers = new HashMap<>();
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

            if (!"localhost".equals(httpURI.getHost())) {
                LOGGER.debug("Cannot bind to host other than 'localhost': {}", httpURI);
            }
            if (!portMatched) {
                LOGGER.debug("Cannot bind to specific port: {}", httpURI);
            }
        }

        @Override
		public void registerHandler(HttpHandlerRegistrationInfo reginfo, HttpHandler handler) {

            String contextPath = getContextPath(reginfo);
            IllegalStateAssertion.assertFalse(contextPath.equals("/"), "Cannot register a HTTP handler to a path of /");
            LOGGER.debug("Using context path {}" , contextPath);

            String relativePath = getRelativePath(reginfo);
            LOGGER.debug("Using relative path {}" , relativePath);

            DelegatingRoutingHandler routingHandler = handlers.get(contextPath);
            if (routingHandler == null) {
                routingHandler = new DelegatingRoutingHandler();
                handlers.put(contextPath, routingHandler);
                LOGGER.debug("Created new DelegatingRoutingHandler {}" ,routingHandler);
            }

            String methods = reginfo.getMethodRestrict() == null ? DEFAULT_METHODS : reginfo.getMethodRestrict();
            LOGGER.debug("Using methods {}" , methods);

            for (String method : methods.split(",")) {
                LOGGER.debug("Adding {}: {} for handler {}", method, relativePath, handler);
                routingHandler.add(method, relativePath, handler);
            }

            LOGGER.debug("Registering DelegatingRoutingHandler on path {}", contextPath);
            defaultHost.registerHandler(contextPath, routingHandler);
        }

		@Override
		public void unregisterHandler(HttpHandlerRegistrationInfo reginfo) {
            String contextPath = getContextPath(reginfo);
            LOGGER.debug("unregisterHandler {}", contextPath);

            DelegatingRoutingHandler routingHandler = handlers.get(contextPath);
            if (routingHandler != null) {
                String methods = reginfo.getMethodRestrict() == null ? DEFAULT_METHODS : reginfo.getMethodRestrict();
                for (String method : methods.split(",")) {
                    String relativePath = getRelativePath(reginfo);
                    routingHandler.remove(method, relativePath);
                    LOGGER.debug("Unregistered {}: {}", method, relativePath);
                }

                // No paths remain registered so remove the base handler
                if (!routingHandler.hasRegisteredPaths()) {
                    defaultHost.unregisterHandler(contextPath);
                    handlers.remove(contextPath);
                    LOGGER.debug("Unregistered root handler from {}", contextPath);
                }
            }
        }

        private String getBasePath(HttpHandlerRegistrationInfo reginfo) {
            String path = reginfo.getUri().getPath();
            if (path.contains(REST_PATH_PLACEHOLDER)) {
                path = PathTemplate.create(path).getBase();
            }
            return URLUtils.normalizeSlashes(path);
        }

        private String getContextPath(HttpHandlerRegistrationInfo reginfo) {
            String path = getBasePath(reginfo);
            String[] pathElements = path.replaceFirst("^/", "").split("/");
            return "/" + pathElements[0];
        }

        private String getRelativePath(HttpHandlerRegistrationInfo reginfo) {
            String path = reginfo.getUri().getPath();
            String contextPath = getContextPath(reginfo);
            return URLUtils.normalizeSlashes(path.substring(contextPath.length()));
        }
    }

    class DelegatingRoutingHandler implements HttpHandler {

        private final List<MethodPathMapping> paths = new CopyOnWriteArrayList<>();
        private final RoutingHandler delegate = Handlers.routing();

        DelegatingRoutingHandler add(String method, String path, HttpHandler handler) {
            MethodPathMapping mapping = new MethodPathMapping(method, path);
            IllegalStateAssertion.assertFalse(paths.contains(mapping), "Cannot register duplicate handler for " + mapping);

            LOGGER.debug("Registered paths {}", this.toString());
            delegate.add(method, path, handler);
            paths.add(mapping);
            return this;
        }

        void remove(String method, String path) {
            // There is currently no way to remove paths from a RoutingHandler so set them to null
            // https://issues.jboss.org/browse/UNDERTOW-1073
            delegate.add(method, path, null);
            paths.remove(new MethodPathMapping(method, path));
        }

        boolean hasRegisteredPaths() {
            return !paths.isEmpty();
        }

        @Override
        public void handleRequest(HttpServerExchange exchange) throws Exception {
            if (exchange.getRelativePath().isEmpty()) {
                exchange.setRelativePath("/");
            }
            delegate.handleRequest(exchange);
        }

        @Override
        public String toString() {
            String formattedPaths = paths.stream()
                .map(methodPathMapping -> methodPathMapping.toString())
                .collect(Collectors.joining(", "));

            return String.format("DelegatingRoutingHandler [%s]", formattedPaths);
        }
    }

    class MethodPathMapping {
        private String method;
        private String path;

        MethodPathMapping(String method, String path) {
            this.method = method;
            this.path = path;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            MethodPathMapping that = (MethodPathMapping) o;

            if (method != null ? !method.equals(that.method) : that.method != null) {
                return false;
            }
            return path != null ? path.equals(that.path) : that.path == null;
        }

        @Override
        public int hashCode() {
            int result = method != null ? method.hashCode() : 0;
            result = 31 * result + (path != null ? path.hashCode() : 0);
            return result;
        }

        @Override
        public String toString() {
            return String.format("%s: %s", method, path);
        }
    }

    class CamelUndertowEventListener implements UndertowEventListener {

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
