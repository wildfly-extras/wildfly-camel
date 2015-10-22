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

import java.io.IOException;
import java.net.URL;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.cxf.transport.undertow.AbstractHTTPServerEngine;
import org.apache.cxf.transport.undertow.UndertowHTTPDestination;
import org.apache.cxf.transport.undertow.UndertowHTTPHandler;
import org.jboss.gravia.runtime.ServiceLocator;
import org.wildfly.extension.undertow.Host;

import io.undertow.server.HttpHandler;
import io.undertow.servlet.Servlets;
import io.undertow.servlet.api.DeploymentInfo;
import io.undertow.servlet.api.DeploymentManager;
import io.undertow.servlet.api.ServletInfo;
import io.undertow.servlet.core.ManagedServlet;

class WildflyHTTPServerEngine extends AbstractHTTPServerEngine {

    private final Host defaultHost;
    private DeploymentManager manager;

    WildflyHTTPServerEngine(String protocol, String host, int port) {
        super(protocol, host, port);
        defaultHost = ServiceLocator.getRequiredService(Host.class);
    }

    public void addServant(URL nurl, UndertowHTTPHandler handler) {

        ServletInfo servletInfo = Servlets.servlet("DefaultServlet", DefaultServlet.class).addMapping("/*");

        DeploymentInfo servletBuilder = Servlets.deployment()
                .setClassLoader(WildflyHTTPServerEngine.class.getClassLoader())
                .setContextPath(nurl.getPath())
                .setDeploymentName("cxfdestination.war")
                .addServlets(servletInfo);

        manager = Servlets.defaultContainer().addDeployment(servletBuilder);
        manager.deploy();


        try {
            HttpHandler servletHandler = manager.start();
            defaultHost.registerDeployment(manager.getDeployment(), servletHandler);

            UndertowHTTPDestination destination = handler.getHTTPDestination();
            destination.setServletContext(manager.getDeployment().getServletContext());

            ManagedServlet managedServlet = manager.getDeployment().getServlets().getManagedServlet("DefaultServlet");
            DefaultServlet servletInstance = (DefaultServlet) managedServlet.getServlet().getInstance();
            servletInstance.setHTTPDestination(destination);
        } catch (ServletException ex) {
            throw new IllegalStateException(ex);
        }
    }

    public void removeServant(URL nurl) {
        if (manager != null) {
            defaultHost.unregisterDeployment(manager.getDeployment());
        }
    }

    @SuppressWarnings("serial")
    static class DefaultServlet extends HttpServlet {

        private UndertowHTTPDestination destination;

        void setHTTPDestination(UndertowHTTPDestination destination) {
            this.destination = destination;
        }

        @Override
        protected void service(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
            destination.doService(req, res);
        }
    }
}
