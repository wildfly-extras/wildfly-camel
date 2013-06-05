/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.as.camel.service;

import org.apache.camel.impl.DefaultCamelContext;
import org.jboss.as.camel.CamelConstants;
import org.jboss.as.camel.CamelContextFactory;
import org.jboss.as.camel.WildflyCamelContext;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.msc.service.AbstractService;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;

/**
 * The {@link CamelContextFactory} service
 *
 * @author Thomas.Diesler@jboss.com
 * @since 05-Jun-2013
 */
public class CamelContextFactoryService extends AbstractService<CamelContextFactory> {

    private CamelContextFactory contextFactory;

    public static ServiceController<CamelContextFactory> addService(ServiceTarget serviceTarget, ServiceVerificationHandler verificationHandler) {
        CamelContextFactoryService service = new CamelContextFactoryService();
        ServiceBuilder<CamelContextFactory> builder = serviceTarget.addService(CamelConstants.CAMEL_CONTEXT_FACTORY_NAME, service);
        builder.addListener(verificationHandler);
        return builder.install();
    }

    // Hide ctor
    private CamelContextFactoryService() {
    }

    @Override
    public void start(StartContext startContext) throws StartException {
        contextFactory = new DefaultCamelContextFactory();
    }

    @Override
    public CamelContextFactory getValue() {
        return contextFactory;
    }

    static final class DefaultCamelContextFactory implements CamelContextFactory {

        @Override
        public DefaultCamelContext createDefaultCamelContext() throws Exception {
            return new WildflyCamelContext(null);
        }

        @Override
        public DefaultCamelContext createDefaultCamelContext(ClassLoader classsLoader) throws Exception {
            return new WildflyCamelContext(classsLoader);
        }
    }
}
