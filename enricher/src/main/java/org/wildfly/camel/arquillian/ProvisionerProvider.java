/*
 * #%L
 * Wildfly Camel :: Enricher
 * %%
 * Copyright (C) 2013 - 2014 RedHat
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
package org.wildfly.camel.arquillian;

import java.lang.annotation.Annotation;
import java.util.concurrent.atomic.AtomicBoolean;

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
 * Implementation to provide {@link Provisioner} injection to {@link ArquillianResource}-
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

        ServiceName serviceName = ServiceName.parse("jboss.wildfly.gravia.Provisioner");
        ServiceController<?> controller = serviceContainer.getService(serviceName);
        if (controller == null)
            return null;

        Provisioner service = (Provisioner) controller.getValue();
        provisionerProducer.set(service);
        return provisioner.get();
    }
}
