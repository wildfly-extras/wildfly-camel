/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.cxf.transport.undertow;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.GeneralSecurityException;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.common.classloader.ClassLoaderUtils;
import org.apache.cxf.common.classloader.ClassLoaderUtils.ClassLoaderHolder;
import org.apache.cxf.configuration.jsse.TLSServerParameters;
import org.apache.cxf.configuration.security.CertificateConstraintsType;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.transport.http.DestinationRegistry;
import org.apache.cxf.transport.https.CertConstraintsJaxBUtils;
import org.apache.cxf.transport.servlet.ServletDestination;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wildfly.extension.camel.service.CamelEndpointDeploymentSchedulerService.EndpointHttpHandler;

public class UndertowHTTPDestination extends ServletDestination implements EndpointHttpHandler {

    private static final Logger LOG = LoggerFactory.getLogger(UndertowHTTPDestination.class);

    protected HttpServerEngine engine;
    protected HttpServerEngineFactory serverEngineFactory;
    protected URL nurl;
    protected ClassLoader loader;

    /**
     * This variable signifies that finalizeConfig() has been called.
     * It gets called after this object has been spring configured.
     * It is used to automatically reinitialize things when resources
     * are reset, such as setTlsServerParameters().
     */
    private boolean configFinalized;

    /**
     * Constructor
     *
     * @param bus the associated Bus
     * @param registry the associated destinationRegistry
     * @param ei the endpoint info of the destination
     * @throws java.io.IOException
     */
    public UndertowHTTPDestination(Bus bus, DestinationRegistry registry, EndpointInfo ei) throws IOException {
        //Add the default port if the address is missing it
        super(bus, registry, ei, getAddressValue(ei, true).getAddress(), true);
        this.serverEngineFactory = bus.getExtension(HttpServerEngineFactory.class);
        if (serverEngineFactory != null) {
            nurl = new URL(getAddress(endpointInfo));
        }
        loader = bus.getExtension(ClassLoader.class);
    }

    /**
     * Post-configure retreival of server engine.
     */
    public void retrieveEngine() throws GeneralSecurityException, IOException {
        if (serverEngineFactory == null) {
            return;
        }
        engine = serverEngineFactory.retrieveHTTPServerEngine(nurl.getPort());
        if (engine == null) {
            engine = serverEngineFactory.getHTTPServerEngine(nurl.getHost(), nurl.getPort(), nurl.getProtocol());
        }

        assert engine != null;
        TLSServerParameters serverParameters = engine.getTlsServerParameters();
        if (serverParameters != null && serverParameters.getCertConstraints() != null) {
            CertificateConstraintsType constraints = serverParameters.getCertConstraints();
            if (constraints != null) {
                certConstraints = CertConstraintsJaxBUtils.createCertConstraints(constraints);
            }
        }

        // When configuring for "http", however, it is still possible that
        // Spring configuration has configured the port for https.
        if (!nurl.getProtocol().equals(engine.getProtocol())) {
            throw new IllegalStateException("Port " + engine.getPort() + " is configured with wrong protocol \"" + engine.getProtocol() + "\" for \"" + nurl + "\"");
        }
    }

    /**
     * This method is used to finalize the configuration
     * after the configuration items have been set.
     *
     */
    public void finalizeConfig() {
        assert !configFinalized;

        try {
            retrieveEngine();
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
        configFinalized = true;
    }

    public String getAddress(EndpointInfo endpointInfo) {
        return endpointInfo.getAddress();
    }

    /**
     * Activate receipt of incoming messages.
     */
    public void activate() {
        super.activate();
        LOG.debug("Activating receipt of incoming messages");

        Class<?> serviceClass = endpointInfo.getProperty("serviceClass", Class.class);
        if (serviceClass != null && this.loader == null) {
            final ClassLoader cl = serviceClass.getClassLoader();
            LOG.debug("Using classloader {} obtained via endpointInfo serviceClass", cl);
            this.loader = cl;
        }

        if (engine != null) {
            UndertowHTTPHandler jhd = createJettyHTTPHandler(this, contextMatchOnExact());
            engine.addServant(nurl, jhd);
        }
    }

    public UndertowHTTPHandler createJettyHTTPHandler(UndertowHTTPDestination jhd, boolean cmExact) {
        return new UndertowHTTPHandler(jhd, cmExact);
    }

    /**
     * Deactivate receipt of incoming messages.
     */
    public void deactivate() {
        super.deactivate();
        LOG.debug("Deactivating receipt of incoming messages");
        if (engine != null) {
            engine.removeServant(nurl);
        }
    }

    public String getBasePathForFullAddress(String addr) {
        try {
            return new URL(addr).getPath();
        } catch (MalformedURLException e) {
            return null;
        }
    }

    @Override
    public void service(ServletContext context, HttpServletRequest req, HttpServletResponse resp) throws IOException {

        ClassLoaderHolder origLoader = null;
        Bus origBus = BusFactory.getAndSetThreadDefaultBus(bus);
        try {
            if (loader != null) {
                origLoader = ClassLoaderUtils.setThreadContextClassloader(loader);
            }
            invoke(null, context, req, resp);
        } finally {
            if (origBus != bus) {
                BusFactory.setThreadDefaultBus(origBus);
            }
            if (origLoader != null) {
                origLoader.reset();
            }
        }
    }

    public ClassLoader getClassLoader() {
        return loader;
    }
}
