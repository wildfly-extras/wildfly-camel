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
import java.util.concurrent.atomic.AtomicBoolean;

import org.jboss.arquillian.container.test.impl.enricher.resource.OperatesOnDeploymentAwareProvider;
import org.jboss.arquillian.core.api.Instance;
import org.jboss.arquillian.core.api.InstanceProducer;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.arquillian.test.spi.annotation.SuiteScoped;
import org.jboss.arquillian.test.spi.enricher.resource.ResourceProvider;
import org.jboss.as.server.CurrentServiceContainer;
import org.jboss.gravia.provision.Provisioner;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;

/**
 * {@link OperatesOnDeploymentAwareProvider} implementation to
 * provide {@link Provisioner} injection to {@link ArquillianResource}-
 * annotated fields.
 *
 * @author Thomas.Diesler@jboss.com
 * @since 16-May-2013
 */
public class ProvisionerProvider implements ResourceProvider {

    private AtomicBoolean initialized = new AtomicBoolean();
    private ServiceContainer serviceContainer;

    @Inject
    @SuiteScoped
    private InstanceProducer<Provisioner> provisionerProducer;

    @Inject
    private Instance<Provisioner> provisioner;

    @Override
    public boolean canProvide(final Class<?> type) {
        return Provisioner.class.isAssignableFrom(type);
    }

    @Override
    public Object lookup(ArquillianResource resource, Annotation... qualifiers) {

        if (initialized.compareAndSet(false, true))
            serviceContainer = CurrentServiceContainer.getServiceContainer();

        ServiceName serviceName = ServiceName.parse("jboss.wildfly.camel.ResourceProvisioner");
        ServiceController<?> controller = serviceContainer.getService(serviceName);
        if (controller == null)
            return null;

        Provisioner service = (Provisioner) controller.getValue();
        provisionerProducer.set(service);
        return provisioner.get();
    }
}
