/*
 * #%L
 * Wildfly Camel Subsystem
 * %%
 * Copyright (C) 2013 JBoss by Red Hat
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as 
 * published by the Free Software Foundation, either version 2.1 of the 
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public 
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
