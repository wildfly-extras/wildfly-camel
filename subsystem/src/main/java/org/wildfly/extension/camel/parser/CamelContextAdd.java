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

package org.wildfly.extension.camel.parser;

import java.util.List;

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;
import org.wildfly.extension.camel.CamelConstants;
import org.wildfly.extension.camel.CamelContextRegistry;
import org.wildfly.extension.camel.service.CamelContextRegistryService;

/**
 * @author Thomas.Diesler@jboss.com
 * @since 23-Aug-2013
 */
final class CamelContextAdd extends AbstractAddStepHandler {

    private final SubsystemState subsystemState;

    CamelContextAdd(SubsystemState subsystemState) {
        this.subsystemState = subsystemState;
    }

    @Override
    protected void populateModel(ModelNode operation, ModelNode model) throws OperationFailedException {
        CamelContextResource.VALUE.validateAndSet(operation, model);
    }

    @Override
    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {

        String propName = operation.get(ModelDescriptionConstants.OP_ADDR).asObject().get(ModelConstants.CONTEXT).asString();
        String propValue = CamelContextResource.VALUE.resolveModelAttribute(context, model).asString();
        subsystemState.putContextDefinition(propName, propValue.trim());

        ServiceController<?> container = context.getServiceRegistry(false).getService(CamelConstants.CAMEL_CONTEXT_REGISTRY_SERVICE_NAME);
        if (container != null) {
            CamelContextRegistryService serviceRegistry = CamelContextRegistryService.class.cast(container.getService());
            CamelContextRegistry camelContextRegistry = serviceRegistry.getValue();
            if (camelContextRegistry != null) {
                if (camelContextRegistry.getCamelContext(propName) == null) {
                    serviceRegistry.createCamelContext(CamelContextRegistry.class.getClassLoader(), propName, propValue);
                }
            }
        }
    }

    @Override
    protected void rollbackRuntime(OperationContext context, ModelNode operation, ModelNode model, List<ServiceController<?>> controllers) {
        String propName = operation.get(ModelDescriptionConstants.OP_ADDR).asObject().get(ModelConstants.CONTEXT).asString();
        subsystemState.removeContextDefinition(propName);
    }

}
