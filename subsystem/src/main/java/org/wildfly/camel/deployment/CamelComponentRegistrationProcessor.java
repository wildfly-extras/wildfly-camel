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

import org.apache.camel.spi.ComponentResolver;
import org.jboss.as.server.deployment.AttachmentKey;
import org.jboss.as.server.deployment.AttachmentList;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.modules.Module;
import org.wildfly.camel.CamelComponentRegistry;
import org.wildfly.camel.CamelComponentRegistry.CamelComponentRegistration;
import org.wildfly.camel.CamelConstants;

/**
 * Register a {@link ComponentResolver} service for each deployed component.
 *
 * @author Thomas.Diesler@jboss.com
 * @since 12-Jun-2013
 */
public class CamelComponentRegistrationProcessor implements DeploymentUnitProcessor {

    static final AttachmentKey<AttachmentList<CamelComponentRegistration>> COMPONENT_REGISTRATIONS = AttachmentKey.createList(CamelComponentRegistration.class);

    @Override
    public void deploy(final DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        DeploymentUnit depUnit = phaseContext.getDeploymentUnit();
        Module module = depUnit.getAttachment(Attachments.MODULE);
        CamelComponentRegistry registry = depUnit.getAttachment(CamelConstants.CAMEL_COMPONENT_REGISTRY_KEY);
        for(CamelComponentRegistration creg : registry.registerComponents(module)) {
            depUnit.addToAttachmentList(COMPONENT_REGISTRATIONS, creg);
        }
    }


    @Override
    public void undeploy(final DeploymentUnit depUnit) {
        // Unregister the ComponentResolver services
        for (CamelComponentRegistration sreg : depUnit.getAttachmentList(COMPONENT_REGISTRATIONS)) {
            sreg.unregister();
        }
    }
}
