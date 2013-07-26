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
import org.wildfly.camel.CamelContextFactory;

/**
 * {@link OperatesOnDeploymentAwareProvider} implementation to
 * provide {@link CamelContextFactory} injection to {@link ArquillianResource}-
 * annotated fields.
 *
 * @author Thomas.Diesler@jboss.com
 * @since 19-May-2013
 */
public class CamelContextFactoryProvider implements ResourceProvider {

    @Inject
    @SuiteScoped
    private InstanceProducer<CamelContextFactory> serviceProducer;

    @Inject
    private Instance<CamelContextFactory> serviceInstance;

    @Override
    public boolean canProvide(final Class<?> type) {
        return CamelContextFactory.class.isAssignableFrom(type);
    }

    @Override
    public Object lookup(ArquillianResource resource, Annotation... qualifiers) {
        if (serviceInstance.get() == null) {
            CamelContextFactory service;
            try {
                InitialContext initialContext = new InitialContext();
                service = (CamelContextFactory) initialContext.lookup(CamelConstants.CAMEL_CONTEXT_FACTORY_BINDING_NAME);
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
