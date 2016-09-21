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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.jboss.as.controller.AbstractRuntimeOnlyHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ReloadRequiredRemoveStepHandler;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleListAttributeDefinition;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;
import org.jboss.as.controller.operations.common.GenericSubsystemDescribeHandler;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * @author Thomas.Diesler@jboss.com
 * @since 23-Aug-2013
 */
final class CamelRootResource extends SimpleResourceDefinition {

    private static final PathElement SUBSYSTEM_PATH = PathElement.pathElement(SUBSYSTEM, CamelExtension.SUBSYSTEM_NAME);
    private static final ResourceDescriptionResolver RESOLVER = CamelResolvers.getResolver(CamelExtension.SUBSYSTEM_NAME);
    private static final SubsystemState subsystemState = new SubsystemState();

    final boolean registerRuntimeOnly;

    CamelRootResource(boolean registerRuntimeOnly) {
        super(SUBSYSTEM_PATH, RESOLVER, new CamelSubsystemAdd(subsystemState), ReloadRequiredRemoveStepHandler.INSTANCE);
        this.registerRuntimeOnly = registerRuntimeOnly;
    }

    @Override
    public void registerOperations(ManagementResourceRegistration resourceRegistration) {
        super.registerOperations(resourceRegistration);
        resourceRegistration.registerOperationHandler(GenericSubsystemDescribeHandler.DEFINITION, GenericSubsystemDescribeHandler.INSTANCE);
    }

    @Override
    public void registerChildren(ManagementResourceRegistration resourceRegistration) {
        resourceRegistration.registerSubModel(new CamelContextResource(subsystemState));
        if (registerRuntimeOnly) {
            AttributeDefinition eldef = new SimpleAttributeDefinitionBuilder("dummy", ModelType.STRING, false).build();
            AttributeDefinition attdef = new SimpleListAttributeDefinition.Builder(ModelConstants.ENDPOINTS, eldef).setStorageRuntime().build();
            resourceRegistration.registerReadOnlyAttribute(attdef, new CamelRuntimeOnlyHandler());
        }
    }

    class CamelRuntimeOnlyHandler extends AbstractRuntimeOnlyHandler {

        @Override
        protected void executeRuntimeStep(OperationContext context, ModelNode operation) throws OperationFailedException {
            String operationName = operation.require(ModelDescriptionConstants.OP).asString();
            if (ModelDescriptionConstants.READ_ATTRIBUTE_OPERATION.equals(operationName)) {
                handleReadAttributeOperation(context, operation);
            }
        }

        private void handleReadAttributeOperation(OperationContext context, ModelNode operation) {
            String name = operation.require(ModelDescriptionConstants.NAME).asString();
            if (ModelConstants.ENDPOINTS.equals(name)) {
                List<ModelNode> values = new ArrayList<>();
                for (URL aux : subsystemState.getRuntimeState().getEndpointURLs()) {
                    values.add(new ModelNode(aux.toString()));
                }
                context.getResult().set(values);
            }
            context.completeStep(OperationContext.RollbackHandler.NOOP_ROLLBACK_HANDLER);
        }
    }
}
