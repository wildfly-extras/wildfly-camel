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

import org.apache.camel.CamelContext;
import org.jboss.as.controller.AbstractRemoveStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;
import org.wildfly.extension.camel.CamelConstants;
import org.wildfly.extension.camel.CamelContextRegistry;
import org.wildfly.extension.camel.service.CamelContextRegistryService;

import static org.wildfly.extension.camel.CamelLogger.LOGGER;

/**
 * @author Thomas.Diesler@jboss.com
 * @since 23-Aug-2013
 */
final class CamelContextRemove extends AbstractRemoveStepHandler {

    private final SubsystemState subsystemState;

    CamelContextRemove(SubsystemState subsystemState) {
        this.subsystemState = subsystemState;
    }

    @Override
    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
        final String propName = operation.get(ModelDescriptionConstants.OP_ADDR).asObject().get(ModelConstants.CONTEXT).asString();
        final String oldContextDefinition = subsystemState.removeContextDefinition(propName);
        context.completeStep(new OperationContext.RollbackHandler() {
            @Override
            public void handleRollback(OperationContext context, ModelNode operation) {
                subsystemState.putContextDefinition(propName, oldContextDefinition);
            }
        });

        ServiceController<?> container = context.getServiceRegistry(false).getService(CamelConstants.CAMEL_CONTEXT_REGISTRY_SERVICE_NAME);
        if (container != null) {
            CamelContextRegistryService serviceRegistry = CamelContextRegistryService.class.cast(container.getService());
            CamelContextRegistry camelContextRegistry = serviceRegistry.getValue();
            if (camelContextRegistry != null) {
                CamelContext camelctx = camelContextRegistry.getCamelContext(propName);
                try {
                    if (camelctx != null) {
                        camelctx.stop();
                    }
                } catch (Exception e) {
                    LOGGER.warn("Cannot stop camel context: " + camelctx.getName(), e);
                    throw new OperationFailedException(e);
                }
            }
        }
    }
}
