/*
 * #%L
 * Wildfly Camel :: Subsystem
 * %%
 * Copyright (C) 2013 - 2017 RedHat
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

import org.jboss.as.ee.structure.DeploymentType;
import org.jboss.as.ee.structure.DeploymentTypeMarker;
import org.jboss.as.server.deployment.AttachmentList;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.module.ResourceRoot;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.Index;

/**
 * DeploymentUnitProcessor that rejects deployments that contain Camel libraries
 */
public class CamelLibraryFilterProcessor implements DeploymentUnitProcessor {

    private static final String MESSAGE_CAMEL_LIBS_DETECTED = "Apache Camel library (%s) was detected within the deployment. "
        + "Either provide a deployment replacing embedded libraries with Camel module dependencies. "
        + "Or disable the Camel subsystem for the current deployment by adding a "
        + "jboss-deployment-structure.xml or jboss-all.xml descriptor to it.";

    @Override
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        DeploymentUnit depUnit = phaseContext.getDeploymentUnit();
        CamelDeploymentSettings depSettings = depUnit.getAttachment(CamelDeploymentSettings.ATTACHMENT_KEY);

        // Skip filtering if this is not a recognized Camel deployment or processing an EAR
        if (!depSettings.isEnabled() || DeploymentTypeMarker.isType(DeploymentType.EAR, depUnit)) {
            return;
        }

        AttachmentList<ResourceRoot> resourceRoots = depUnit.getAttachment(Attachments.RESOURCE_ROOTS);
        if (resourceRoots != null) {
            for (ResourceRoot root : resourceRoots) {
                if (hasClassesFromPackage(root.getAttachment(Attachments.ANNOTATION_INDEX), "org.apache.camel")) {
                    throw new IllegalStateException(String.format(MESSAGE_CAMEL_LIBS_DETECTED, root.getRootName()));
                }
            }
        }
    }

    @Override
    public void undeploy(DeploymentUnit context) {
    }

    private boolean hasClassesFromPackage(final Index index, final String pck) {
        for (ClassInfo ci : index.getKnownClasses()) {
            if (ci.name().toString().startsWith(pck)) {
                return true;
            }
        }
        return false;
    }
}
