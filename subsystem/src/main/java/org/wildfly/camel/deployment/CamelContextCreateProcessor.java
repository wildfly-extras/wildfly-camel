/*
 * #%L
 * Wildfly Camel Subsystem
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


package org.wildfly.camel.deployment;

import static org.wildfly.camel.CamelMessages.MESSAGES;

import java.io.IOException;
import java.net.URL;
import org.apache.camel.CamelContext;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.modules.Module;
import org.jboss.vfs.VirtualFile;
import org.wildfly.camel.CamelConstants;
import org.wildfly.camel.SpringCamelContextFactory;

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
        final String runtimeName = depUnit.getName();

        URL contextDefinitionURL = null;
        try {
            if (runtimeName.endsWith(CamelConstants.CAMEL_CONTEXT_FILE_SUFFIX)) {
                contextDefinitionURL = depUnit.getAttachment(Attachments.DEPLOYMENT_CONTENTS).asFileURL();
            } else {
                VirtualFile rootFile = depUnit.getAttachment(Attachments.DEPLOYMENT_ROOT).getRoot();
                VirtualFile child = rootFile.getChild(CamelConstants.CAMEL_CONTEXT_FILE_NAME);
                contextDefinitionURL = child.isFile() ? child.asFileURL() : null;
            }
        } catch (IOException ex) {
            throw MESSAGES.cannotCreateCamelContext(ex, runtimeName);
        }

        if (contextDefinitionURL == null)
            return;

        // Create the camel context
        CamelContext camelctx;
        try {
            Module module = depUnit.getAttachment(Attachments.MODULE);
            camelctx = SpringCamelContextFactory.createSpringCamelContext(contextDefinitionURL, module.getClassLoader());
        } catch (Exception ex) {
            throw MESSAGES.cannotCreateCamelContext(ex, runtimeName);
        }

        // Add the camel context to the deployemnt
        depUnit.putAttachment(CamelConstants.CAMEL_CONTEXT_KEY, camelctx);
    }

    @Override
    public void undeploy(final DeploymentUnit depUnit) {
        depUnit.removeAttachment(CamelConstants.CAMEL_CONTEXT_KEY);
    }
}
