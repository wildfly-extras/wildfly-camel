/*
 * #%L
 * Wildfly Camel Subsystem
 * %%
 * Copyright (C) 2013 JBoss by Red Hat
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as 
 * published by the Free Software Foundation, either version 2.1 of the 
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public 
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-2.1.html>.
 * #L%
 */


package org.wildfly.camel.service;

import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.gravia.resolver.DefaultResolver;
import org.jboss.gravia.resolver.Resolver;
import org.jboss.msc.service.AbstractService;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.wildfly.camel.CamelConstants;

/**
 * Service providing the {@link Resolver}.
 *
 * @author Thomas.Diesler@jboss.com
 * @since 27-Jun-2013
 */
public class ResolverService extends AbstractService<Resolver> {

    private Resolver resolver;

    public static ServiceController<Resolver> addService(ServiceTarget serviceTarget, ServiceVerificationHandler verificationHandler) {
        ResolverService service = new ResolverService();
        ServiceBuilder<Resolver> builder = serviceTarget.addService(CamelConstants.RESOLVER_SERVICE_NAME, service);
        builder.addListener(verificationHandler);
        return builder.install();
    }

    // Hide ctor
    private ResolverService() {
    }

    @Override
    public void start(StartContext startContext) throws StartException {
        resolver = new DefaultResolver();
    }

    @Override
    public Resolver getValue() throws IllegalStateException {
        return resolver;
    }

}