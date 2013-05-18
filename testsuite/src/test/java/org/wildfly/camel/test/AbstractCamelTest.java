/*
 * JBoss, Home of Professional Open Source
 * Copyright 2009, Red Hat Middleware LLC, and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.wildfly.camel.test;

import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.camel.CamelConstants;
import org.jboss.as.camel.CamelContextRegistry;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.ServiceController;

/**
 * Support for WildFly-Camel integration tests.
 *
 * @author thomas.diesler@jboss.com
 * @since 16-May-2013
 */
public abstract class AbstractCamelTest {

    @ArquillianResource
    ServiceContainer serviceContainer;

    public CamelContextRegistry getCamelContextRegistry() throws Exception {
        ServiceController<?> controller = serviceContainer.getRequiredService(CamelConstants.CAMEL_CONTEXT_REGISTRY_NAME);
        return (CamelContextRegistry) controller.getValue();
    }
}
