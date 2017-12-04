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

import org.apache.camel.CamelContext;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.modules.Module;
import org.wildfly.extension.camel.CamelConstants;
import org.wildfly.extension.camel.SpringCamelContextFactory;

/**
 * Processes deployments that can create a {@link CamelContext}.
 *
 * @author Thomas.Diesler@jboss.com
 * @since 22-Apr-2013
 */
public class CamelContextCreateProcessor implements DeploymentUnitProcessor {

    @Override
    public void deploy(final DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {

        final DeploymentUnit depUnit = phaseContext.getDeploymentUnit();

        final Module module = depUnit.getAttachment(Attachments.MODULE);
        final String runtimeName = depUnit.getName();

        // Add the camel contexts to the deployment
        CamelDeploymentSettings depSettings = depUnit.getAttachment(CamelDeploymentSettings.ATTACHMENT_KEY);
        for (URL contextURL : depSettings.getCamelContextUrls()) {
            ClassLoader tccl = Thread.currentThread().getContextClassLoader();
            try {
                Thread.currentThread().setContextClassLoader(module.getClassLoader());
                for (CamelContext camelctx : SpringCamelContextFactory.createCamelContextList(contextURL, module.getClassLoader())) {
                    depUnit.addToAttachmentList(CamelConstants.CAMEL_CONTEXT_KEY, camelctx);
                }
            } catch (Exception ex) {
                throw new IllegalStateException("Cannot create camel context: " + runtimeName, ex);
            } finally {
                Thread.currentThread().setContextClassLoader(tccl);
            }
        }
    }

    @Override
    public void undeploy(final DeploymentUnit depUnit) {
    }
}
