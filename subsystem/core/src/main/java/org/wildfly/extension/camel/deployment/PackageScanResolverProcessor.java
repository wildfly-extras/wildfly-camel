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

package org.wildfly.extension.camel.deployment;

import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.modules.ModuleClassLoader;
import org.wildfly.extension.camel.CamelConstants;
import org.wildfly.extension.camel.ContextCreateHandler;
import org.wildfly.extension.camel.ContextCreateHandlerRegistry;
import org.wildfly.extension.camel.handler.PackageScanClassResolverAssociationHandler;

/**
 * Add a PackageScanClassResolver to the CamelContext
 *
 * @author Thomas.Diesler@jboss.com
 * @since 08-Jan-2015
 */
public class PackageScanResolverProcessor implements DeploymentUnitProcessor {

    public final void deploy(final DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        DeploymentUnit depUnit = phaseContext.getDeploymentUnit();
        CamelDeploymentSettings depSettings = depUnit.getAttachment(CamelDeploymentSettings.ATTACHMENT_KEY);

        // Camel disabled
        if (!depSettings.isEnabled()) {
            return;
        }

        ContextCreateHandlerRegistry createHandlerRegistry = depUnit.getAttachment(CamelConstants.CONTEXT_CREATE_HANDLER_REGISTRY_KEY);
        ModuleClassLoader moduleClassLoader = depUnit.getAttachment(Attachments.MODULE).getClassLoader();
        ContextCreateHandler contextCreateHandler = new PackageScanClassResolverAssociationHandler(moduleClassLoader);
        depSettings.setClassResolverAssociationHandler(contextCreateHandler);
        createHandlerRegistry.addContextCreateHandler(moduleClassLoader, contextCreateHandler);
    }

    public void undeploy(DeploymentUnit depUnit) {
        ContextCreateHandlerRegistry createHandlerRegistry = depUnit.getAttachment(CamelConstants.CONTEXT_CREATE_HANDLER_REGISTRY_KEY);
        CamelDeploymentSettings depSettings = depUnit.getAttachment(CamelDeploymentSettings.ATTACHMENT_KEY);
        if (createHandlerRegistry != null) {
            ModuleClassLoader classLoader = depUnit.getAttachment(Attachments.MODULE).getClassLoader();
            createHandlerRegistry.removeContextCreateHandler(classLoader, depSettings.getClassResolverAssociationHandler());
        }
    }
}
