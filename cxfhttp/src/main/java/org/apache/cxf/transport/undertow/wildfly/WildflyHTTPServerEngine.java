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

package org.apache.cxf.transport.undertow.wildfly;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.apache.cxf.transport.undertow.AbstractHTTPServerEngine;
import org.apache.cxf.transport.undertow.UndertowHTTPHandler;
import org.jboss.as.server.CurrentServiceContainer;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wildfly.extension.camel.service.CamelEndpointDeploymentSchedulerService;

class WildflyHTTPServerEngine extends AbstractHTTPServerEngine {
    private static final Logger LOG = LoggerFactory.getLogger(WildflyHTTPServerEngine.class);

    private final Map<URI, ServiceName> uriServiceNameMap = new HashMap<>();

    WildflyHTTPServerEngine(String protocol, String host, int port) {
        super(protocol, host, port);
    }

    public void addServant(URL nurl, UndertowHTTPHandler handler) {
        try {
            final URI uri = nurl.toURI();
            LOG.debug("Adding CXF servant for URI {}", uri);
            final ServiceName serviceName = CamelEndpointDeploymentSchedulerService.deploymentSchedulerServiceName(handler.getHTTPDestination().getClassLoader());
            ServiceController<?> controller = CurrentServiceContainer.getServiceContainer().getRequiredService(serviceName);
            CamelEndpointDeploymentSchedulerService deploymentSchedulerService = (CamelEndpointDeploymentSchedulerService) controller.getValue();
            deploymentSchedulerService.schedule(uri, handler.getHTTPDestination());
            synchronized (uriServiceNameMap) {
                uriServiceNameMap.put(uri, serviceName);
            }
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    public void removeServant(URL nurl) {
        try {
            final URI uri = nurl.toURI();
            LOG.debug("Removing CXF servant for URI {}", uri);
            ServiceName serviceName = null;
            synchronized (uriServiceNameMap) {
                serviceName = uriServiceNameMap.remove(uri);
            }
            if (serviceName != null) {
                ServiceController<?> serviceControler = CurrentServiceContainer.getServiceContainer().getRequiredService(serviceName);
                CamelEndpointDeploymentSchedulerService deploymentSchedulerService = (CamelEndpointDeploymentSchedulerService) serviceControler.getValue();
                deploymentSchedulerService.unschedule(uri);
            }
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

}
