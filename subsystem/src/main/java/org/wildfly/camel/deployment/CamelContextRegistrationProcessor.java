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

import org.apache.camel.CamelContext;
import org.jboss.as.server.deployment.AttachmentKey;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.wildfly.camel.CamelConstants;
import org.wildfly.camel.CamelContextRegistry;
import org.wildfly.camel.CamelContextRegistry.CamelContextRegistration;

/**
 * Register a {@link CamelContext} with the {@link CamelContextRegistry}.
 *
 * @author Thomas.Diesler@jboss.com
 * @since 22-Apr-2013
 */
public class CamelContextRegistrationProcessor implements DeploymentUnitProcessor {

    AttachmentKey<CamelContextRegistration> CAMEL_CONTEXT_REGISTRATION_KEY = AttachmentKey.create(CamelContextRegistration.class);

    @Override
    public void deploy(final DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        DeploymentUnit depUnit = phaseContext.getDeploymentUnit();
        CamelContext camelctx = depUnit.getAttachment(CamelConstants.CAMEL_CONTEXT_KEY);
        if (camelctx == null)
            return;

        // Register the camel context
        CamelContextRegistry registry = depUnit.getAttachment(CamelConstants.CAMEL_CONTEXT_REGISTRY_KEY);
        try {
            CamelContextRegistration registration = registry.registerCamelContext(camelctx);
            depUnit.putAttachment(CAMEL_CONTEXT_REGISTRATION_KEY, registration);
        } catch (Exception ex) {
            throw MESSAGES.cannotStartCamelContext(ex, camelctx);
        }
    }

    @Override
    public void undeploy(final DeploymentUnit depUnit) {
        // Unregister the camel context
        CamelContextRegistration registration = depUnit.getAttachment(CAMEL_CONTEXT_REGISTRATION_KEY);
        if (registration != null) {
            registration.unregister();
        }
    }
}
