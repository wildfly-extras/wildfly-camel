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
import org.jboss.vfs.VirtualFile;

/**
 * A DUP that detects switchyard deployments
 *
 * @author Thomas.Diesler@jboss.com
 * @since 20-May-2015
 */
public final class SwitchyardDeploymentProcessor implements DeploymentUnitProcessor {

    static String SWITCHYARD_MARKER_FILE = "META-INF/switchyard.xml";

    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {

        DeploymentUnit depUnit = phaseContext.getDeploymentUnit();
        CamelDeploymentSettings depSettings = depUnit.getAttachment(CamelDeploymentSettings.ATTACHMENT_KEY);
        if (depSettings == null) {
            depSettings = new CamelDeploymentSettings();
            depUnit.putAttachment(CamelDeploymentSettings.ATTACHMENT_KEY, depSettings);
        }

        VirtualFile rootFile = depUnit.getAttachment(Attachments.DEPLOYMENT_ROOT).getRoot();
        if (rootFile.getChild(SWITCHYARD_MARKER_FILE).exists()) {
            depSettings.setEnabled(false);
        }
    }

    public void undeploy(DeploymentUnit context) {
    }

}
