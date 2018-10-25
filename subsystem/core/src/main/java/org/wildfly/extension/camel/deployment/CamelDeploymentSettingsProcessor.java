/*
 * #%L
 * Wildfly Camel :: Subsystem
 * %%
 * Copyright (C) 2013 - 2016 RedHat
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

/**
 * Unattaches the {@link CamelDeploymentSettings.Builder} set by {@link CamelDeploymentSettingsBuilderProcessor}, builds
 * and attaches a new {@link CamelDeploymentSettings} using it.
 *
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 */
public final class CamelDeploymentSettingsProcessor implements DeploymentUnitProcessor {

    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {

        final DeploymentUnit depUnit = phaseContext.getDeploymentUnit();

        CamelDeploymentSettings.Builder depSettingsBuilder = depUnit
                .removeAttachment(CamelDeploymentSettings.BUILDER_ATTACHMENT_KEY);
        if (depSettingsBuilder != null && depUnit.getParent() == null) {
            /*
             * We do this only for parentless deployments because for ones that have a parent the build and attachment
             * is done by CamelDeploymentSettings.Builder.build() if the parent
             */
            final CamelDeploymentSettings depSettings = depSettingsBuilder.build();
            depUnit.putAttachment(CamelDeploymentSettings.ATTACHMENT_KEY, depSettings);
        }

    }

    public void undeploy(final DeploymentUnit depUnit) {
        CamelDeploymentSettings.remove(CamelDeploymentSettingsBuilderProcessor.getDeploymentName(depUnit));
    }

}
