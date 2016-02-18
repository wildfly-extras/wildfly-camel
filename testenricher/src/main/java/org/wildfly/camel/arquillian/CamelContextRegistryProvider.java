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

import org.jboss.arquillian.core.api.Instance;
import org.jboss.arquillian.core.api.InstanceProducer;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.arquillian.test.spi.annotation.SuiteScoped;
import org.jboss.arquillian.test.spi.enricher.resource.ResourceProvider;
import org.jboss.gravia.runtime.ServiceLocator;
import org.wildfly.extension.camel.CamelContextRegistry;

/**
 * Implementation to provide {@link CamelContextRegistry} injection to {@link ArquillianResource}-
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
            CamelContextRegistry service = ServiceLocator.getRequiredService(CamelContextRegistry.class);
            serviceProducer.set(service);
        }
        return serviceInstance.get();
    }
}
