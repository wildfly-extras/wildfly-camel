/*
 * #%L
 * Wildfly Camel :: Subsystem
 * %%
 * Copyright (C) 2013 - 2015 RedHat
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
package org.wildfly.extension.camel.cdi;

import java.util.List;

import org.jboss.as.ee.weld.WeldDeploymentMarker;
import org.jboss.as.server.DeploymentProcessorTarget;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.Phase;
import org.jboss.as.weld.deployment.BeanDeploymentArchiveImpl;
import org.jboss.as.weld.deployment.WeldAttachments;
import org.jboss.weld.bootstrap.spi.BeanDeploymentArchive;
import org.wildfly.extension.camel.deployment.CamelDeploymentSettings;
import org.wildfly.extension.camel.deployment.DeploymentUnitProcessorPlugin;
import org.wildfly.extension.camel.parser.CamelExtension;
import org.wildfly.extension.camel.parser.CamelSubsystemAdd;
import org.wildfly.extension.camel.parser.SubsystemState;

public class CamelCdiBeanArchiveProcessorPlugin implements DeploymentUnitProcessorPlugin {

    @Override
    public void addDeploymentProcessor(DeploymentProcessorTarget processorTarget, SubsystemState subsystemState) {
        processorTarget.addDeploymentProcessor(CamelExtension.SUBSYSTEM_NAME, Phase.INSTALL, CamelSubsystemAdd.INSTALL_CDI_BEAN_ARCHIVE_PROCESSOR, new CamelCdiBeanArchiveProcessor());
    }

    class CamelCdiBeanArchiveProcessor implements DeploymentUnitProcessor {

        @Override
        public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
            DeploymentUnit depUnit = phaseContext.getDeploymentUnit();

            CamelDeploymentSettings depSettings = depUnit.getAttachment(CamelDeploymentSettings.ATTACHMENT_KEY);
            List<DeploymentUnit> subDeployments = depUnit.getAttachmentList(Attachments.SUB_DEPLOYMENTS);

            // Return if camel disabled or not a CDI deployment
            if (!depSettings.isEnabled() || !WeldDeploymentMarker.isPartOfWeldDeployment(depUnit)) {
                return;
            }

            // Return if we're not an EAR deployment with 1 or more sub-deployments
            if (depUnit.getName().endsWith(".ear") && subDeployments.isEmpty()) {
                return;
            }

            // Make sure external bean archives from the camel-cdi module are visible to sub deployments
            List<BeanDeploymentArchiveImpl> deploymentArchives = depUnit.getAttachmentList(WeldAttachments.ADDITIONAL_BEAN_DEPLOYMENT_MODULES);
            BeanDeploymentArchiveImpl rootArchive = depUnit.getAttachment(WeldAttachments.DEPLOYMENT_ROOT_BEAN_DEPLOYMENT_ARCHIVE);

            for (BeanDeploymentArchiveImpl bda : deploymentArchives) {
                if (bda.getBeanArchiveType().equals(BeanDeploymentArchiveImpl.BeanArchiveType.EXTERNAL)) {
                    for (BeanDeploymentArchive topLevelBda : rootArchive.getBeanDeploymentArchives()) {
                        bda.addBeanDeploymentArchive(topLevelBda);
                    }
                }
            }
        }

        @Override
        public void undeploy(DeploymentUnit deploymentUnit) {
        }
    }
}
