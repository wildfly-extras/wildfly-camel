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

import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.wildfly.camel.CamelConstants;
import org.wildfly.extension.gravia.GraviaConstants;

/**
 * Allways make some camel integration services available
 *
 * @author Thomas.Diesler@jboss.com
 * @since 14-Jun-2013
 */
public class CamelIntegrationProcessor implements DeploymentUnitProcessor {

    @Override
    public void deploy(final DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        phaseContext.addDeploymentDependency(CamelConstants.CAMEL_CONTEXT_REGISTRY_SERVICE_NAME, CamelConstants.CAMEL_CONTEXT_REGISTRY_KEY);
        phaseContext.addDeploymentDependency(CamelConstants.CAMEL_COMPONENT_REGISTRY_SERVICE_NAME, CamelConstants.CAMEL_COMPONENT_REGISTRY_KEY);
        phaseContext.addDeploymentDependency(GraviaConstants.REPOSITORY_SERVICE_NAME, GraviaConstants.REPOSITORY_KEY);
        phaseContext.addDeploymentDependency(GraviaConstants.RUNTIME_SERVICE_NAME, GraviaConstants.RUNTIME_KEY);
    }

    @Override
    public void undeploy(final DeploymentUnit depUnit) {
    }
}
