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

import java.net.URL;

import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.modules.Module;
import org.wildfly.extension.camel.CamelConstants;
import org.wildfly.extension.camel.SpringCamelContextBootstrap;

/**
 * Creates a {@link SpringCamelContextBootstrap} bootstrapper for each Camel Spring XML file within the deployment.
 */
public class CamelContextBootstrapProcessor implements DeploymentUnitProcessor {

    @Override
    public void deploy(final DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {

        final DeploymentUnit depUnit = phaseContext.getDeploymentUnit();

        final Module module = depUnit.getAttachment(Attachments.MODULE);
        final String runtimeName = depUnit.getName();

        // Add the camel context bootstraps to the deployment
        CamelDeploymentSettings depSettings = depUnit.getAttachment(CamelDeploymentSettings.ATTACHMENT_KEY);
        for (URL contextURL : depSettings.getCamelContextUrls()) {
            ClassLoader tccl = SecurityActions.getContextClassLoader();
            try {
                SecurityActions.setContextClassLoader(module.getClassLoader());
                SpringCamelContextBootstrap bootstrap = new SpringCamelContextBootstrap(contextURL, module.getClassLoader());
                depUnit.addToAttachmentList(CamelConstants.CAMEL_CONTEXT_BOOTSTRAP_KEY, bootstrap);
            } catch (Exception ex) {
                throw new IllegalStateException("Cannot create camel context: " + runtimeName, ex);
            } finally {
                SecurityActions.setContextClassLoader(tccl);
            }
        }
    }

    @Override
    public void undeploy(final DeploymentUnit depUnit) {
    }
}
