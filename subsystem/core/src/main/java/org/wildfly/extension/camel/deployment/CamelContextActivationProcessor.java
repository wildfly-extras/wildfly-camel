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

import static org.wildfly.extension.camel.CamelLogger.LOGGER;

import java.util.List;

import org.jboss.as.naming.deployment.ContextNames;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.wildfly.extension.camel.CamelConstants;
import org.wildfly.extension.camel.SpringCamelContextBootstrap;
import org.wildfly.extension.camel.service.CamelContextActivationService;

/**
 * Creates a {@link CamelContextActivationService} for the deployment with service dependencies for
 * all JNDI bindings required by the Camel Spring application.
 */
public class CamelContextActivationProcessor implements DeploymentUnitProcessor {

    private static final ServiceName CAMEL_CONTEXT_ACTIVATION_SERVICE_NAME = ServiceName.of("CamelContextActivationService");

    @Override
    public void deploy(final DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {

        DeploymentUnit depUnit = phaseContext.getDeploymentUnit();
        CamelDeploymentSettings depSettings = depUnit.getAttachment(CamelDeploymentSettings.ATTACHMENT_KEY);

        if (!depSettings.isEnabled()) {
            return;
        }

        String runtimeName = depUnit.getName();

        ServiceTarget serviceTarget = phaseContext.getServiceTarget();
        ServiceName camelActivationServiceName = depUnit.getServiceName().append(CAMEL_CONTEXT_ACTIVATION_SERVICE_NAME.append(runtimeName));

        List<SpringCamelContextBootstrap> camelctxBootstrapList = depUnit.getAttachmentList(CamelConstants.CAMEL_CONTEXT_BOOTSTRAP_KEY);
        CamelContextActivationService activationService = new CamelContextActivationService(camelctxBootstrapList, runtimeName);
        ServiceBuilder builder = serviceTarget.addService(camelActivationServiceName, activationService);

        // Ensure all camel contexts in the deployment are started before constructing servlets etc
        depUnit.addToAttachmentList(Attachments.WEB_DEPENDENCIES, camelActivationServiceName);

        // Add JNDI binding dependencies to CamelContextActivationService
        for (SpringCamelContextBootstrap bootstrap : camelctxBootstrapList) {
            for (String jndiName : bootstrap.getJndiNames()) {
                if (jndiName.startsWith("${")) {
                    // Don't add the binding if it appears to be a Spring property placeholder value
                    // these can't be resolved before refresh() has been called on the ApplicationContext
                    LOGGER.warn("Skipping JNDI binding dependency for property placeholder value: {}", jndiName);
                } else {
                    LOGGER.debug("Add CamelContextActivationService JNDI binding dependency for {}", jndiName);
                    installBindingDependency(builder, jndiName);
                }
            }
        }

        builder.install();
    }

    @Override
    public void undeploy(final DeploymentUnit depUnit) {
    }

    private void installBindingDependency(ServiceBuilder builder, String jndiName) {
        if (jndiName != null) {
            ContextNames.BindInfo bindInfo = ContextNames.bindInfoFor(jndiName);
            builder.addDependency(bindInfo.getBinderServiceName());
        }
    }
}
