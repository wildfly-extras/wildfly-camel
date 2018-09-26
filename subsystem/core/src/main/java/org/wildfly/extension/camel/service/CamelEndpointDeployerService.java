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
import java.lang.reflect.Field;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

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
import org.wildfly.extension.camel.service.CamelEndpointDeploymentSchedulerService.EndpointHttpHandler;
import org.wildfly.extension.undertow.Host;
import org.wildfly.extension.undertow.ServletContainerService;
import org.wildfly.extension.undertow.UndertowService;
import org.wildfly.extension.undertow.deployment.UndertowDeploymentInfoService;

import io.undertow.security.api.AuthenticationMechanismFactory;
import io.undertow.server.HandlerWrapper;
import io.undertow.server.HttpHandler;
import io.undertow.server.session.SessionListener;
import io.undertow.servlet.Servlets;
import io.undertow.servlet.api.Deployment;
import io.undertow.servlet.api.DeploymentInfo;
import io.undertow.servlet.api.DeploymentManager;
import io.undertow.servlet.api.FilterInfo;
import io.undertow.servlet.api.FilterMappingInfo;
import io.undertow.servlet.api.LifecycleInterceptor;
import io.undertow.servlet.api.ListenerInfo;
import io.undertow.servlet.api.LoginConfig;
import io.undertow.servlet.api.SecurityConstraint;
import io.undertow.servlet.api.SecurityInfo.EmptyRoleSemantic;
import io.undertow.servlet.api.ServletInfo;
import io.undertow.servlet.api.ThreadSetupHandler;
import io.undertow.servlet.api.TransportGuaranteeType;
import io.undertow.servlet.api.WebResourceCollection;
import io.undertow.servlet.core.ManagedServlet;
import io.undertow.websockets.jsr.WebSocketDeploymentInfo;
import io.undertow.websockets.jsr.WebSocketDeploymentInfo.ContainerReadyListener;

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

    /**
     * This method can simplified substantially, once https://github.com/undertow-io/undertow/pull/642 reaches us.
     * Currently, the method is just an adjusted copy of {@link DeploymentInfo#clone()}.
     *
     * @param src the {@link DeploymentInfo} to clone and adapt
     * @param uri the {@link URI} of the CXF endpoint
     * @param servletInfo of the servlet that will serve the endpoint
     * @return a new adapted {@link DeploymentInfo}
     */
    static DeploymentInfo adaptDeploymentInfo(DeploymentInfo src, URI uri, ServletInfo servletInfo) {
        final String contextPath = uri.getPath();
        final String deploymentName = src.getDeploymentName() + ":" + uri.getPath();

        final DeploymentInfo info = new DeploymentInfo()
                .setClassLoader(src.getClassLoader())
                .setContextPath(contextPath)
                .setResourceManager(src.getResourceManager())
                .setMajorVersion(src.getMajorVersion())
                .setMinorVersion(src.getMinorVersion())
                .setDeploymentName(deploymentName)
                .setClassIntrospecter(src.getClassIntrospecter());

        info.addServlet(servletInfo);

        for (Map.Entry<String, FilterInfo> e : src.getFilters().entrySet()) {
            info.addFilter(e.getValue().clone());
        }
        info.setDisplayName(src.getDisplayName());
        for (FilterMappingInfo fmi : src.getFilterMappings()) {
            switch (fmi.getMappingType()) {
            case URL:
                info.addFilterUrlMapping(fmi.getFilterName(), fmi.getMapping(), fmi.getDispatcher());
                break;
            case SERVLET:
                info.addFilterServletNameMapping(fmi.getFilterName(), fmi.getMapping(), fmi.getDispatcher());
                break;
            default:
                throw new IllegalStateException(
                        "Unexpected " + io.undertow.servlet.api.FilterMappingInfo.MappingType.class.getName() + " "
                                + fmi.getMappingType());
            }
        }
        final List<ListenerInfo> listeners = src.getListeners();
        final List<String> approvedListeners = Arrays.asList(
                "org.wildfly.extension.undertow.deployment.JspInitializationListener",
                "org.jboss.weld.module.web.servlet.WeldInitialListener",
                "org.jboss.weld.module.web.servlet.WeldTerminalListener",
                "org.wildfly.microprofile.opentracing.smallrye.TracerInitializer"
                );
        final String infoServiceListenerClassNamePrefix = UndertowDeploymentInfoService.class.getName() + "$";
        for (ListenerInfo listenerInfo : listeners) {
            CamelLogger.LOGGER.debug("Copying ListenerInfo {}", listenerInfo);
            if (listenerInfo.getListenerClass().getName().startsWith(infoServiceListenerClassNamePrefix)) {
                /* ignore */
            } else {
                assert approvedListeners.stream().anyMatch(cl -> cl.equals(listenerInfo.getListenerClass().getName())) : "Unexpected "+ ListenerInfo.class.getName() + ": "+ listenerInfo + "; expected any of ["+ approvedListeners.stream().collect(Collectors.joining(", ")) +"]";
                info.addListener(listenerInfo);
            }
        }

        info.addServletContainerInitalizers(src.getServletContainerInitializers());
        for (ThreadSetupHandler a : src.getThreadSetupActions()) {
            info.addThreadSetupAction(a);
        }
        for (Entry<String, String> en : src.getInitParameters().entrySet()) {
            info.addInitParameter(en.getKey(), en.getValue());
        }
        for (Entry<String, Object> en : src.getServletContextAttributes().entrySet()) {
            if (WebSocketDeploymentInfo.ATTRIBUTE_NAME.equals(en.getKey())) {
                final WebSocketDeploymentInfo srcWsdi = (WebSocketDeploymentInfo) en.getValue();

                /* Simplify start
                 * Once https://github.com/undertow-io/undertow/pull/672 reaches us
                 * the follwing code can be simplified using the newly added WebSocketDeploymentInfo methods clone()
                 * and [get|add]Listeners() */
                final WebSocketDeploymentInfo targetWsdi = new WebSocketDeploymentInfo()
                        .setWorker(srcWsdi.getWorker())
                        .setBuffers(srcWsdi.getBuffers())
                        .setDispatchToWorkerThread(srcWsdi.isDispatchToWorkerThread())
                        .setReconnectHandler(srcWsdi.getReconnectHandler())
                        ;
                targetWsdi.setClientBindAddress(srcWsdi.getClientBindAddress());
                srcWsdi.getAnnotatedEndpoints().stream().forEach(e -> targetWsdi.addEndpoint(e));
                srcWsdi.getProgramaticEndpoints().stream().forEach(e -> targetWsdi.addEndpoint(e));
                srcWsdi.getExtensions().stream().forEach(e -> targetWsdi.addExtension(e));
                try {
                    final Field containerReadyListenersField = srcWsdi.getClass().getDeclaredField("containerReadyListeners");
                    containerReadyListenersField.setAccessible(true);
                    @SuppressWarnings("unchecked")
                    final List<ContainerReadyListener> containerReadyListeners = (List<ContainerReadyListener>) containerReadyListenersField.get(srcWsdi);
                    assert containerReadyListeners.stream().anyMatch(l -> l.getClass().getName().startsWith(infoServiceListenerClassNamePrefix)) : infoServiceListenerClassNamePrefix + "* not found in "+ WebSocketDeploymentInfo.class.getSimpleName() + ".containerReadyListeners";
                    assert containerReadyListeners.size() == 1 : WebSocketDeploymentInfo.class.getSimpleName() + ".containerReadyListeners.size() expected 1, actual "+ listeners.size();
                } catch (NoSuchFieldException | SecurityException | IllegalAccessException e1) {
                    throw new RuntimeException(e1);
                }
                /* Simplify end */
            } else {
                info.addServletContextAttribute(en.getKey(), en.getValue());
            }
        }
        info.addWelcomePages(src.getWelcomePages());
        info.addErrorPages(src.getErrorPages());
        info.addMimeMappings(src.getMimeMappings());
        info.setExecutor(src.getExecutor());
        info.setAsyncExecutor(src.getAsyncExecutor());
        info.setTempDir(src.getTempDir());
        info.setJspConfigDescriptor(src.getJspConfigDescriptor());
        info.setDefaultServletConfig(src.getDefaultServletConfig());
        for (Entry<String, String> en : src.getLocaleCharsetMapping().entrySet()) {
            info.addLocaleCharsetMapping(en.getKey(), en.getValue());
        }
        info.setSessionManagerFactory(src.getSessionManagerFactory());
        final LoginConfig loginConfig = src.getLoginConfig();
        if (loginConfig != null) {
            info.setLoginConfig(loginConfig.clone());
        }
        info.setIdentityManager(src.getIdentityManager());
        info.setConfidentialPortManager(src.getConfidentialPortManager());
        info.setDefaultEncoding(src.getDefaultEncoding());
        info.setUrlEncoding(src.getUrlEncoding());
        info.addSecurityConstraints(filterConstraints(src, uri));
        for (HandlerWrapper w : src.getOuterHandlerChainWrappers()) {
            info.addOuterHandlerChainWrapper(w);
        }
        for (HandlerWrapper w : src.getInnerHandlerChainWrappers()) {
            info.addInnerHandlerChainWrapper(w);
        }
        info.setInitialSecurityWrapper(src.getInitialSecurityWrapper());
        for (HandlerWrapper w : src.getSecurityWrappers()) {
            info.addSecurityWrapper(w);
        }
        for (HandlerWrapper w : src.getInitialHandlerChainWrappers()) {
            info.addInitialHandlerChainWrapper(w);
        }
        info.addSecurityRoles(src.getSecurityRoles());
        info.addNotificationReceivers(src.getNotificationReceivers());
        info.setAllowNonStandardWrappers(src.isAllowNonStandardWrappers());
        info.setDefaultSessionTimeout(src.getDefaultSessionTimeout());
        info.setServletContextAttributeBackingMap(src.getServletContextAttributeBackingMap());
        info.setServletSessionConfig(src.getServletSessionConfig());
        info.setHostName(src.getHostName());
        info.setDenyUncoveredHttpMethods(src.isDenyUncoveredHttpMethods());
        info.setServletStackTraces(src.getServletStackTraces());
        info.setInvalidateSessionOnLogout(src.isInvalidateSessionOnLogout());
        info.setDefaultCookieVersion(src.getDefaultCookieVersion());
        info.setSessionPersistenceManager(src.getSessionPersistenceManager());
        for (Map.Entry<String, Set<String>> e : src.getPrincipalVersusRolesMap().entrySet()) {
            info.addPrincipalVsRoleMappings(e.getKey(), e.getValue());
        }
        info.setIgnoreFlush(src.isIgnoreFlush());
        info.setAuthorizationManager(src.getAuthorizationManager());
        for (Entry<String, AuthenticationMechanismFactory> e : src.getAuthenticationMechanisms().entrySet()) {
            info.addAuthenticationMechanism(e.getKey(), e.getValue());
        }
        info.setJaspiAuthenticationMechanism(src.getJaspiAuthenticationMechanism());
        info.setSecurityContextFactory(src.getSecurityContextFactory());
        info.setServerName(src.getServerName());
        info.setMetricsCollector(src.getMetricsCollector());
        info.setSessionConfigWrapper(src.getSessionConfigWrapper());
        info.setEagerFilterInit(src.isEagerFilterInit());
        info.setDisableCachingForSecuredPages(src.isDisableCachingForSecuredPages());
        info.setExceptionHandler(src.getExceptionHandler());
        info.setEscapeErrorMessage(src.isEscapeErrorMessage());
        for (SessionListener e : src.getSessionListeners()) {
            info.addSessionListener(e);
        }
        for (LifecycleInterceptor e : src.getLifecycleInterceptors()) {
            info.addLifecycleInterceptor(e);
        }
        info.setAuthenticationMode(src.getAuthenticationMode());
        info.setDefaultMultipartConfig(src.getDefaultMultipartConfig());
        info.setContentTypeCacheSize(src.getContentTypeCacheSize());
        info.setSessionIdGenerator(src.getSessionIdGenerator());
        info.setSendCustomReasonPhraseOnError(src.isSendCustomReasonPhraseOnError());
        info.setChangeSessionIdOnLogin(src.isChangeSessionIdOnLogin());
        info.setCrawlerSessionManagerConfig(src.getCrawlerSessionManagerConfig());
        info.setSecurityDisabled(src.isSecurityDisabled());
        info.setUseCachedAuthenticationMechanism(src.isUseCachedAuthenticationMechanism());
        info.setCheckOtherSessionManagers(src.isCheckOtherSessionManagers());

        return info;
    }

    public static ServiceController<CamelEndpointDeployerService> addService(DeploymentUnit deploymentUnit,
            ServiceTarget serviceTarget, ServiceName deploymentInfoServiceName, ServiceName hostServiceName) {

        CamelEndpointDeployerService service = new CamelEndpointDeployerService();
        ServiceBuilder<CamelEndpointDeployerService> sb = serviceTarget
                .addService(deployerServiceName(deploymentUnit.getServiceName()), service);
        sb.addDependency(hostServiceName, Host.class, service.hostSupplier);
        sb.addDependency(deploymentInfoServiceName, DeploymentInfo.class, service.deploymentInfoSupplier);
        sb.addDependency(
                CamelEndpointDeploymentSchedulerService.deploymentSchedulerServiceName(deploymentUnit.getServiceName()),
                CamelEndpointDeploymentSchedulerService.class, service.deploymentSchedulerServiceSupplier);
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

    public static ServiceName deployerServiceName(ServiceName deploymentUnitServiceName) {
        return deploymentUnitServiceName.append(SERVICE_NAME);
    }

    static List<SecurityConstraint> filterConstraints(DeploymentInfo mainDeploymentInfo, URI uri) {
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

    static io.undertow.servlet.api.TransportGuaranteeType transportGuaranteeType(URI uri,
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

    private final InjectedValue<CamelEndpointDeploymentSchedulerService> deploymentSchedulerServiceSupplier = new InjectedValue<>();

    private final InjectedValue<Host> hostSupplier = new InjectedValue<>();

    private final InjectedValue<ServletContainerService> servletContainerServiceSupplier = new InjectedValue<>();

    public CamelEndpointDeployerService() {
    }

    @Override
    public CamelEndpointDeployerService getValue() throws IllegalStateException, IllegalArgumentException {
        return this;
    }

    /**
     * Exposes a HTTP endpoint defined by the given {@link EndpointHttpHandler} under the given {@link URI}'s path.
     *
     * @param uri determines the path and protocol under which the HTTP endpoint should be exposed
     * @param endpointHttpHandler an {@link EndpointHttpHandler} to use for handling HTTP requests sent to the given
     *        {@link URI}'s path
     */
    public void deploy(URI uri, EndpointHttpHandler endpointHttpHandler) {

        final ServletInfo servletInfo = Servlets.servlet(EndpointServlet.NAME, EndpointServlet.class).addMapping("/*")
                .setAsyncSupported(true);

        final DeploymentInfo mainDeploymentInfo = deploymentInfoSupplier.getValue();

        DeploymentInfo endPointDeplyomentInfo = adaptDeploymentInfo(mainDeploymentInfo, uri, servletInfo);
        CamelLogger.LOGGER.debug("Deploying endpoint {}", endPointDeplyomentInfo.getDeploymentName());

        final ClassLoader old = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(endPointDeplyomentInfo.getClassLoader());
        try {
            final DeploymentManager manager = servletContainerServiceSupplier.getValue().getServletContainer()
                    .addDeployment(endPointDeplyomentInfo);
            manager.deploy();
            final Deployment deployment = manager.getDeployment();
            try {
                HttpHandler servletHandler = manager.start();
                hostSupplier.getValue().registerDeployment(deployment, servletHandler);

                ManagedServlet managedServlet = deployment.getServlets().getManagedServlet(EndpointServlet.NAME);

                EndpointServlet servletInstance = (EndpointServlet) managedServlet.getServlet().getInstance();
                servletInstance.setEndpointHttpHandler(endpointHttpHandler);
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

    @Override
    public void start(StartContext context) throws StartException {
        /*
         * Now that the injectedMainDeploymentInfo is ready, we can link this to CamelEndpointDeploymentSchedulerService
         */
        deploymentSchedulerServiceSupplier.getValue().registerDeployer(this);
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

    @SuppressWarnings("serial")
    static class EndpointServlet extends HttpServlet {

        public static final String NAME = "EndpointServlet";
        private EndpointHttpHandler endpointHttpHandler;

        @Override
        protected void service(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
            endpointHttpHandler.service(getServletContext(), req, res);
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
