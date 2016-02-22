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

import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.wildfly.extension.camel.CamelConstants;
import org.wildfly.extension.gravia.GraviaConstants;

/**
 * Allways make some camel integration services available
 *
 * @author Thomas.Diesler@jboss.com
 * @since 14-Jun-2013
 */
public class CamelIntegrationProcessor implements DeploymentUnitProcessor {

    @Override
    public void deploy(final DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        DeploymentUnit depUnit = phaseContext.getDeploymentUnit();
        CamelDeploymentSettings depSettings = depUnit.getAttachment(CamelDeploymentSettings.ATTACHMENT_KEY);

        // No camel dependencies if camel is disabled
        if (!depSettings.isEnabled()) {
            return;
        }

        phaseContext.addDeploymentDependency(CamelConstants.CONTEXT_CREATE_HANDLER_REGISTRY_SERVICE_NAME, CamelConstants.CONTEXT_CREATE_HANDLER_REGISTRY_KEY);
        phaseContext.addDeploymentDependency(CamelConstants.CAMEL_CONTEXT_REGISTRY_SERVICE_NAME, CamelConstants.CAMEL_CONTEXT_REGISTRY_KEY);
        phaseContext.addDeploymentDependency(CamelConstants.CAMEL_CONTEXT_FACTORY_SERVICE_NAME, CamelConstants.CAMEL_CONTEXT_FACTORY_KEY);
        phaseContext.addDeploymentDependency(GraviaConstants.RUNTIME_SERVICE_NAME, GraviaConstants.RUNTIME_KEY);
    }

    @Override
    public void undeploy(final DeploymentUnit depUnit) {
    }
}
