/*
 * #%L
 * Wildfly Camel :: Subsystem
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
package org.wildfly.extension.camel.service;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.function.Consumer;
import java.util.regex.Pattern;

import javax.servlet.Servlet;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jboss.as.ee.component.ComponentConfiguration;
import org.jboss.as.ee.component.EEModuleConfiguration;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.wildfly.extension.camel.CamelLogger;
import org.wildfly.extension.undertow.Host;
import org.wildfly.extension.undertow.ServletContainerService;
import org.wildfly.extension.undertow.UndertowService;

import io.undertow.server.HandlerWrapper;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.servlet.Servlets;
import io.undertow.servlet.api.Deployment;
import io.undertow.servlet.api.DeploymentInfo;
import io.undertow.servlet.api.DeploymentManager;
import io.undertow.servlet.api.SecurityConstraint;
import io.undertow.servlet.api.SecurityInfo.EmptyRoleSemantic;
import io.undertow.servlet.api.ServletInfo;
import io.undertow.servlet.api.TransportGuaranteeType;
import io.undertow.servlet.api.WebResourceCollection;
import io.undertow.servlet.core.DeploymentImpl;
import io.undertow.servlet.core.ManagedServlet;

/**
 * A service responsible for exposing and unexposing CXF endpoints via {@link #deploy(URI, EndpointHttpHandler)} and
 * {@link #undeploy(URI)}.
 *
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 */
public class CamelEndpointDeployerService implements Service<CamelEndpointDeployerService> {

    private static final String MATCH_ALL_ENDPOINT_URI_PREFIX = "///*";

    private static final String MATCH_ALL_PREFIX = "/*";

    /** The name for the {@link CamelEndpointDeployerService} */
    private static final String SERVICE_NAME = "EndpointDeployer";

    /** A {@link ThreadLocal} to pass the {@link HttpServerExchange} from the {@link HttpHandler} chain to {@link DelegatingEndpointHttpHandler} */
    private static final ThreadLocal<HttpServerExchange> exchangeThreadLocal = new ThreadLocal<>();

    /** Stores the {@link HttpServerExchange} to {@link #exchangeThreadLocal} */
    private static final HandlerWrapper exchangeStoringHandlerWrapper = new HandlerWrapper() {
        @Override
        public HttpHandler wrap(final HttpHandler handler) {
            return exchange -> {
                exchangeThreadLocal.set(exchange);
                try {
                    handler.handleRequest(exchange);
                } finally {
                    exchangeThreadLocal.remove();
                }
            };
        }

    };

    public static ServiceController<CamelEndpointDeployerService> addService(DeploymentUnit deploymentUnit,
            ServiceTarget serviceTarget, ServiceName deploymentInfoServiceName, ServiceName hostServiceName) {

        CamelEndpointDeployerService service = new CamelEndpointDeployerService();
        ServiceBuilder<CamelEndpointDeployerService> sb = serviceTarget.addService(deployerServiceName(deploymentUnit.getServiceName()), service);
        sb.addDependency(hostServiceName, Host.class, service.hostSupplier);
        sb.addDependency(deploymentInfoServiceName, DeploymentInfo.class, service.deploymentInfoSupplier);
        sb.addDependency(UndertowService.SERVLET_CONTAINER.append("default"), ServletContainerService.class, service.servletContainerServiceSupplier);

        final EEModuleConfiguration moduleConfiguration = deploymentUnit
                .getAttachment(org.jboss.as.ee.component.Attachments.EE_MODULE_CONFIGURATION);
        if (moduleConfiguration != null) {
            for (final ComponentConfiguration c : moduleConfiguration.getComponentConfigurations()) {
                sb.addDependency(c.getComponentDescription().getStartServiceName());
            }
        }
        return sb.install();
    }

    public static ServiceName deployerServiceName(ServiceName depUnitServiceName) {
        return depUnitServiceName.append(SERVICE_NAME);
    }

    private static List<SecurityConstraint> filterConstraints(DeploymentInfo mainDeploymentInfo, URI uri) {
        final List<SecurityConstraint> result = new ArrayList<>();
        final String uriPath = uri.getPath();
        final String endpointUriPrefix = "//" + uriPath;
        final String mainContextPath = mainDeploymentInfo.getContextPath();
        final boolean isRelativeUri = uriPath.startsWith(mainContextPath);
        final String relativeUriPath = isRelativeUri ? uriPath.substring(mainContextPath.length()) : null;
        for (SecurityConstraint mainSecurityConstraint : mainDeploymentInfo.getSecurityConstraints()) {
            final SecurityConstraint endpointSecurityConstraint = new SecurityConstraint();
            for (WebResourceCollection mainResourceCollection : mainSecurityConstraint.getWebResourceCollections()) {
                final WebResourceCollection endpointResourceCollection = new WebResourceCollection();
                for (String mainUrlPattern : mainResourceCollection.getUrlPatterns()) {
                    if (MATCH_ALL_ENDPOINT_URI_PREFIX.equals(mainUrlPattern)) {
                        endpointResourceCollection.addUrlPattern(MATCH_ALL_PREFIX);
                    } else {
                        final UrlPattern pattern = new UrlPattern(mainUrlPattern);
                        String relativePattern = null;
                        if (isRelativeUri) {
                            if ((relativePattern = pattern.relativize(relativeUriPath)) != null) {
                                endpointResourceCollection.addUrlPattern(relativePattern);
                            }
                        } else if ((relativePattern = pattern.relativize(endpointUriPrefix)) != null) {
                            endpointResourceCollection.addUrlPattern(relativePattern);
                        }
                    }
                }
                if (!endpointResourceCollection.getUrlPatterns().isEmpty()) {
                    endpointResourceCollection.addHttpMethods(mainResourceCollection.getHttpMethods());
                    endpointResourceCollection.addHttpMethodOmissions(mainResourceCollection.getHttpMethodOmissions());
                    endpointSecurityConstraint.addWebResourceCollection(endpointResourceCollection);
                }
            }

            if (!endpointSecurityConstraint.getWebResourceCollections().isEmpty()) {
                endpointSecurityConstraint.addRolesAllowed(mainSecurityConstraint.getRolesAllowed());
                endpointSecurityConstraint.setEmptyRoleSemantic(mainSecurityConstraint.getEmptyRoleSemantic());
                endpointSecurityConstraint.setTransportGuaranteeType(
                        transportGuaranteeType(uri, mainSecurityConstraint.getTransportGuaranteeType()));
                result.add(endpointSecurityConstraint);
            }

        }
        if (result.isEmpty() && uri.getScheme().equals("https")) {
            final WebResourceCollection webResourceCollection = new WebResourceCollection();
            webResourceCollection.addUrlPattern("/*");

            final SecurityConstraint endpointSecurityConstraint = new SecurityConstraint();
            endpointSecurityConstraint.addWebResourceCollection(webResourceCollection);
            endpointSecurityConstraint.setTransportGuaranteeType(TransportGuaranteeType.CONFIDENTIAL);
            endpointSecurityConstraint.setEmptyRoleSemantic(EmptyRoleSemantic.PERMIT);
            result.add(endpointSecurityConstraint);
        }

        return result;
    }

    private static TransportGuaranteeType transportGuaranteeType(URI uri,
            final TransportGuaranteeType transportGuaranteeType) {
        if (uri.getScheme().equals("https")) {
            return io.undertow.servlet.api.TransportGuaranteeType.CONFIDENTIAL;
        } else if (transportGuaranteeType != null) {
            return transportGuaranteeType;
        } else {
            return io.undertow.servlet.api.TransportGuaranteeType.NONE;
        }
    }

    private final InjectedValue<DeploymentInfo> deploymentInfoSupplier = new InjectedValue<>();

    private final Map<URI, DeploymentManager> deployments = new HashMap<>();

    private final InjectedValue<Host> hostSupplier = new InjectedValue<>();

    private final InjectedValue<ServletContainerService> servletContainerServiceSupplier = new InjectedValue<>();

    public CamelEndpointDeployerService() {
    }

    @Override
    public CamelEndpointDeployerService getValue() throws IllegalStateException, IllegalArgumentException {
        return this;
    }

    /**
     * Exposes an HTTP endpoint defined by the given {@link EndpointHttpHandler} under the given {@link URI}'s path.
     *
     * @param uri determines the path and protocol under which the HTTP endpoint should be exposed
     * @param endpointHttpHandler an {@link EndpointHttpHandler} to use for handling HTTP requests sent to the given
     *        {@link URI}'s path
     */
    public void deploy(URI uri, EndpointHttpHandler endpointHttpHandler) {
        doDeploy(
                uri,
                servletInstance -> servletInstance.setEndpointHttpHandler(endpointHttpHandler), // plug the endpointHttpHandler into the servlet
                deploymentInfo -> {}, // no need to customize the deploymentInfo
                deployment -> {} // no need to customize the deployment
        );
    }

    /**
     * Exposes an HTTP endpoint that will be served by the given {@link HttpHandler} under the given {@link URI}'s path.
     *
     * @param uri determines the path and protocol under which the HTTP endpoint should be exposed
     * @param routingHandler an {@link HttpHandler} to use for handling HTTP requests sent to the given
     *        {@link URI}'s path
     */
    public void deploy(URI uri, final HttpHandler routingHandler) {
        final Set<Deployment> availableDeployments = hostSupplier.getValue().getDeployments();
        if (!availableDeployments.stream().anyMatch(
                        deployment -> deployment.getHandler() instanceof CamelEndpointDeployerHandler
                    && ((CamelEndpointDeployerHandler) deployment.getHandler()).getRoutingHandler() == routingHandler)) {
            /* deploy only if the routing handler is not there already */
            doDeploy(
                    uri,
                    servletInstance -> servletInstance.setEndpointHttpHandler(new DelegatingEndpointHttpHandler(routingHandler)), // plug the endpointHttpHandler into the servlet
                    deploymentInfo -> deploymentInfo.addInnerHandlerChainWrapper(exchangeStoringHandlerWrapper), // add the handler to the chain
                    deployment -> { // wrap the initial handler with our custom class so that we can recognize it at other places
                        final HttpHandler servletHandler = new CamelEndpointDeployerHandler(deployment.getHandler(), routingHandler);
                        deployment.setInitialHandler(servletHandler);
                    });
        }
    }

    private void doDeploy(URI uri, Consumer<EndpointServlet> endpointServletConsumer, Consumer<DeploymentInfo> deploymentInfoConsumer, Consumer<DeploymentImpl> deploymentConsumer) {

        final ServletInfo servletInfo = Servlets.servlet(EndpointServlet.NAME, EndpointServlet.class).addMapping("/*")
                .setAsyncSupported(true);

        final DeploymentInfo mainDeploymentInfo = deploymentInfoSupplier.getValue();

        DeploymentInfo endPointDeplyomentInfo = adaptDeploymentInfo(mainDeploymentInfo, uri, servletInfo);
        deploymentInfoConsumer.accept(endPointDeplyomentInfo);
        CamelLogger.LOGGER.debug("Deploying endpoint {}", endPointDeplyomentInfo.getDeploymentName());

        final ClassLoader old = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(endPointDeplyomentInfo.getClassLoader());
        try {
            final DeploymentManager manager = servletContainerServiceSupplier.getValue().getServletContainer()
                    .addDeployment(endPointDeplyomentInfo);
            manager.deploy();
            final Deployment deployment = manager.getDeployment();
            try {
                deploymentConsumer.accept((DeploymentImpl) deployment);
                manager.start();
                hostSupplier.getValue().registerDeployment(deployment, deployment.getHandler());

                ManagedServlet managedServlet = deployment.getServlets().getManagedServlet(EndpointServlet.NAME);

                EndpointServlet servletInstance = (EndpointServlet) managedServlet.getServlet().getInstance();
                endpointServletConsumer.accept(servletInstance);
            } catch (ServletException ex) {
                throw new IllegalStateException(ex);
            }
            synchronized (deployments) {
                deployments.put(uri, manager);
            }
        } finally {
            Thread.currentThread().setContextClassLoader(old);
        }
    }

    private DeploymentInfo adaptDeploymentInfo(DeploymentInfo src, URI uri, ServletInfo servletInfo) {

    	String deploymentName = src.getDeploymentName() + ":" + uri.getPath();

        DeploymentInfo info = src.clone()
                .setContextPath(uri.getPath())
                .setDeploymentName(deploymentName)
                .addServlet(servletInfo);

        info.addSecurityConstraints(filterConstraints(src, uri));

        return info;
    }

    @Override
    public void start(StartContext context) throws StartException {
    }

    @Override
    public void stop(StopContext context) {
        synchronized (deployments) {
            for (DeploymentManager deploymentManager : deployments.values()) {
                undeploy(deploymentManager);
            }
            deployments.clear();
        }
    }

    /**
     * Unexpose the endpoint available under the given {@link URI}'s path.
     *
     * @param uri the URI to unexpose
     */
    public void undeploy(URI uri) {
        synchronized (deployments) {
            DeploymentManager deploymentManager = deployments.remove(uri);
            if (deploymentManager != null) {
                try {
                    undeploy(deploymentManager);
                } catch (IllegalStateException e) {
                    CamelLogger.LOGGER.warn("Could not undeploy endpoint "
                            + deploymentManager.getDeployment().getDeploymentInfo().getDeploymentName(), e);
                }
            }
        }
    }

    private void undeploy(DeploymentManager deploymentManager) {
        final Deployment deployment = deploymentManager.getDeployment();
        CamelLogger.LOGGER.debug("Undeploying endpoint {}", deployment.getDeploymentInfo().getDeploymentName());

        final ClassLoader old = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(deployment.getDeploymentInfo().getClassLoader());
        try {
            try {
                hostSupplier.getValue().unregisterDeployment(deployment);
                deploymentManager.stop();
            } catch (ServletException e) {
                throw new RuntimeException(e);
            }
            deploymentManager.undeploy();
            servletContainerServiceSupplier.getValue().getServletContainer().removeDeployment(deployment.getDeploymentInfo());
        } finally {
            Thread.currentThread().setContextClassLoader(old);
        }

    }

    /**
     * An {@link HttpHandler} that delegates to {@link #initialServletHandler} and stores the {@link #routingHandler}
     * so that the given routing handler can be recognized at other places and based on that it can be deployed only once.
     */
    public static class CamelEndpointDeployerHandler implements HttpHandler {
        private final HttpHandler initialServletHandler;
        private final HttpHandler routingHandler;

        public CamelEndpointDeployerHandler(HttpHandler initialServletHandler, HttpHandler routingHandler) {
            super();
            this.initialServletHandler = initialServletHandler;
            this.routingHandler = routingHandler;
        }

        public HttpHandler getRoutingHandler() {
            return routingHandler;
        }

        @Override
        public void handleRequest(HttpServerExchange exchange) throws Exception {
            initialServletHandler.handleRequest(exchange);
        }

    }

    /**
     * A funny class: although called from within a {@link Servlet} it actually delegates to an {@link HttpHandler}
     * using the {@link HttpServerExchange} stored in {@link CamelEndpointDeployerService#exchangeThreadLocal}. Found no
     * better way to deploy an {@link HttpHandler} that we get through
     * {@link CamelEndpointDeployerService#deploy(URI, HttpHandler)} so that the role permissions are enforced.
     * The role permissions are enforced by an {@link HttpHandler} quite late in the chain, so our {@link HttpHandler}
     * would have to be placed behind that one. But Undertow API do not allow that. Therefore this weird servlet
     * wrapping an {@link HttpHandler}.
     */
    static class DelegatingEndpointHttpHandler implements EndpointHttpHandler {

        private final HttpHandler handler;

        public DelegatingEndpointHttpHandler(HttpHandler handler) {
            super();
            this.handler = handler;
        }

        @Override
        public ClassLoader getClassLoader() {
            return null;
        }

        @Override
        public void service(ServletContext context, HttpServletRequest req, HttpServletResponse resp)
                throws IOException {
            HttpServerExchange exchange = exchangeThreadLocal.get();
            try {
                handler.handleRequest(exchange);
            } catch (IOException e) {
                throw e;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

    }

	public interface EndpointHttpHandler {
	    ClassLoader getClassLoader();
	    void service(ServletContext context, HttpServletRequest req, HttpServletResponse resp) throws IOException;
	}
	
    @SuppressWarnings("serial")
    static class EndpointServlet extends HttpServlet {

        public static final String NAME = "EndpointServlet";
        private EndpointHttpHandler endpointHttpHandler;

        @Override
        protected void service(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
            if (endpointHttpHandler != null) {
                endpointHttpHandler.service(getServletContext(), req, res);
            }
        }

        public void setEndpointHttpHandler(EndpointHttpHandler endpointHttpHandler) {
            this.endpointHttpHandler = endpointHttpHandler;
        }

    }

    static class UrlPattern {

        private final String source;

        public UrlPattern(String source) {
            super();
            this.source = source;
        }

        public String relativize(String path) {
            final StringTokenizer patternTokenizer = new StringTokenizer(source, "/", true);
            final StringTokenizer pathTokenizer = new StringTokenizer(path, "/", true);
            /* skip the common segments */
            while (pathTokenizer.hasMoreTokens()) {
                if (!patternTokenizer.hasMoreTokens()) {
                    return null;
                }
                final String patternToken = patternTokenizer.nextToken();
                final String pathToken = pathTokenizer.nextToken();
                final boolean isSlash = patternToken.equals("/");
                if (isSlash != pathToken.equals("/")) {
                    return null;
                } else if (!isSlash) {
                    if ("*".equals(pathToken)) {
                        continue;
                    } else {
                        final StringTokenizer segmentTokenizer = new StringTokenizer(patternToken, "*", true);
                        final StringBuilder segmentPatternBuilder = new StringBuilder(patternToken.length());
                        while (segmentTokenizer.hasMoreTokens()) {
                            final String segmentToken = segmentTokenizer.nextToken();
                            if ("*".equals(segmentToken)) {
                                segmentPatternBuilder.append(".*");
                            } else {
                                segmentPatternBuilder.append(Pattern.quote(segmentToken));
                            }
                        }
                        final Pattern pat = Pattern.compile(segmentPatternBuilder.toString());
                        if (!pat.matcher(pathToken).matches()) {
                            return null;
                        }
                    }
                }
            }
            final StringBuilder result = new StringBuilder();
            while (patternTokenizer.hasMoreTokens()) {
                result.append(patternTokenizer.nextToken());
            }
            if (result.length() == 0) {
                return "";
            } else if (result.charAt(0) != '/') {
                return "/" + result.toString();
            } else {
                return result.toString();
            }
        }
    }
}
