/*
 * #%L
 * Wildfly Camel :: Subsystem
 * %%
 * Copyright (C) 2013 - 2018 RedHat
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
import org.jboss.as.server.deployment.DeploymentResourceSupport;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.web.common.WarMetaData;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceName;
import org.wildfly.extension.camel.CamelLogger;
import org.wildfly.extension.camel.service.CamelEndpointDeployerService;
import org.wildfly.extension.undertow.DeploymentDefinition;
import org.wildfly.extension.undertow.UndertowExtension;
import org.wildfly.extension.undertow.UndertowService;
import org.wildfly.extension.undertow.deployment.UndertowDeploymentInfoService;

/**
 * Adds {@link CamelEndpointDeployerService}.
 *
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 */
public class CamelEndpointDeployerProcessor implements DeploymentUnitProcessor {

    @Override
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        CamelDeploymentSettings depSettings = deploymentUnit.getAttachment(CamelDeploymentSettings.ATTACHMENT_KEY);

        if (!depSettings.isEnabled()) {
            return;
        }
        final WarMetaData warMetaData = deploymentUnit.getAttachment(WarMetaData.ATTACHMENT_KEY);
        if (warMetaData == null) {
            /* ignore non-war deployments */
            CamelLogger.LOGGER.debug("{} ignores non-WAR deployment {}",
                    CamelEndpointDeployerProcessor.class.getSimpleName(), deploymentUnit.getName());
            return;
        }

        final DeploymentResourceSupport deploymentResourceSupport = deploymentUnit
                .getAttachment(Attachments.DEPLOYMENT_RESOURCE_SUPPORT);
        final ModelNode node = deploymentResourceSupport.getDeploymentSubsystemModel(UndertowExtension.SUBSYSTEM_NAME);
        final String hostName = node.get(DeploymentDefinition.VIRTUAL_HOST.getName()).asString();
        final String serverName = node.get(DeploymentDefinition.SERVER.getName()).asString();
        final String path = node.get(DeploymentDefinition.CONTEXT_ROOT.getName()).asString();

        final ServiceName deploymentServiceName = UndertowService.deploymentServiceName(serverName, hostName, path);
        final ServiceName deploymentInfoServiceName = deploymentServiceName
                .append(UndertowDeploymentInfoService.SERVICE_NAME);
        final ServiceName hostServiceName = UndertowService.virtualHostName(serverName, hostName);

        CamelEndpointDeployerService.addService(deploymentUnit, phaseContext.getServiceTarget(),
                deploymentInfoServiceName, hostServiceName);
    }

    @Override
    public void undeploy(DeploymentUnit context) {

    }
}
