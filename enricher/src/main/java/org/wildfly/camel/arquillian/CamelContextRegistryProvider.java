/*
 * #%L
 * Wildfly Camel Enricher
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
package org.wildfly.camel.arquillian;

import java.lang.annotation.Annotation;

import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.jboss.arquillian.container.test.impl.enricher.resource.OperatesOnDeploymentAwareProvider;
import org.jboss.arquillian.core.api.Instance;
import org.jboss.arquillian.core.api.InstanceProducer;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.arquillian.test.spi.annotation.SuiteScoped;
import org.jboss.arquillian.test.spi.enricher.resource.ResourceProvider;
import org.wildfly.camel.CamelConstants;
import org.wildfly.camel.CamelContextRegistry;

/**
 * {@link OperatesOnDeploymentAwareProvider} implementation to
 * provide {@link CamelContextRegistry} injection to {@link ArquillianResource}-
 * annotated fields.
 *
 * @author Thomas.Diesler@jboss.com
 * @since 19-May-2013
 */
public class CamelContextRegistryProvider implements ResourceProvider {

    @Inject
    @SuiteScoped
    private InstanceProducer<CamelContextRegistry> serviceProducer;

    @Inject
    private Instance<CamelContextRegistry> serviceInstance;

    @Override
    public boolean canProvide(final Class<?> type) {
        return CamelContextRegistry.class.isAssignableFrom(type);
    }

    @Override
    public Object lookup(ArquillianResource resource, Annotation... qualifiers) {
        if (serviceInstance.get() == null) {
            CamelContextRegistry service;
            try {
                InitialContext initialContext = new InitialContext();
                service = (CamelContextRegistry) initialContext.lookup(CamelConstants.CAMEL_CONTEXT_REGISTRY_BINDING_NAME);
            } catch (NamingException ex) {
                throw new IllegalStateException(ex);
            }
            if (service != null) {
                serviceProducer.set(service);
            }
        }
        return serviceInstance.get();
    }
}
