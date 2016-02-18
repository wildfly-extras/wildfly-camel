/*
 * #%L
 * Wildfly Camel :: Subsystem
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

package org.wildfly.extension.camel;

import org.apache.camel.CamelContext;
import org.apache.camel.Component;
import org.jboss.as.server.DeploymentProcessorTarget;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.ServiceTarget;
import org.wildfly.extension.camel.parser.SubsystemRuntimeState;
import org.wildfly.extension.camel.parser.SubsystemState;

/**
 * A subsystem extension service
 *
 * @author Thomas.Diesler@jboss.com
 * @since 18-Feb-2016
 */
public interface CamelSubsytemExtension {

    public default void addExtensionServices(ServiceTarget serviceTarget, SubsystemRuntimeState runtimeState) {
    }

    public default void addDeploymentProcessor(DeploymentProcessorTarget processorTarget, SubsystemState subsystemState) {
    }

    public default ContextCreateHandler getContextCreateHandler(ServiceContainer serviceContainer, ServiceTarget serviceTarget, SubsystemRuntimeState runtimeState) {
        return null;
    }

    public default void addCamelContext(ServiceTarget serviceTarget, CamelContext camelctx) {
    }

    public default void removeCamelContext(CamelContext camelctx) {
    }

    public default Component resolveComponent(String name, SubsystemRuntimeState runtimeState) {
        return null;
    }
}
