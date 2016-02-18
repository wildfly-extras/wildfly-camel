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

import org.jboss.as.controller.AbstractWriteAttributeHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.dmr.ModelNode;

/**
 * @author Thomas.Diesler@jboss.com
 * @since 23-Aug-2013
 */
final class CamelContextWrite extends AbstractWriteAttributeHandler<Object> {
    static final CamelContextWrite INSTANCE = new CamelContextWrite();

    private CamelContextWrite() {
        super(CamelContextResource.VALUE);
    }

    @Override
    protected boolean requiresRuntime(OperationContext context) {
        return context.isNormalServer();
    }

    @Override
    protected boolean applyUpdateToRuntime(OperationContext context, ModelNode operation, String attributeName, ModelNode resolvedValue, ModelNode currentValue, HandbackHolder<Object> handbackHolder) throws OperationFailedException {
        return doUpdate(context, operation, resolvedValue);
    }

    @Override
    protected void revertUpdateToRuntime(OperationContext context, ModelNode operation, String attributeName, ModelNode valueToRestore, ModelNode valueToRevert, Object handback) throws OperationFailedException {
        doUpdate(context, operation, valueToRestore);
    }

    private boolean doUpdate(OperationContext context, ModelNode operation, ModelNode value) {
        //String propName = operation.get(ModelDescriptionConstants.OP_ADDR).asObject().get(ModelConstants.CONTEXT).asString();
        //String propValue = value.asString();
        return true;
    }
}
