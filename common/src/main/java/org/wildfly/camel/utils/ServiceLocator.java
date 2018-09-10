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
package org.wildfly.camel.utils;

import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;

public class ServiceLocator {

    private static volatile ServiceContainer serviceContainer;

    public static ServiceContainer getServiceContainer() {
        return serviceContainer;
    }

    public static void setServiceContainer(ServiceContainer serviceContainer) {
        ServiceLocator.serviceContainer = serviceContainer;
    }

    public static <T extends Object> T getRequiredService(ServiceName serviceName, Class<T> type) {
        ServiceController<?> serviceController = serviceContainer.getRequiredService(serviceName);
        T service = (T) serviceController.getValue();
        return service;
    }

}
