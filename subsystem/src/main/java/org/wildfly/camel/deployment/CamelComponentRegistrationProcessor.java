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
