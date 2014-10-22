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

import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.module.ModuleDependency;
import org.jboss.as.server.deployment.module.ModuleSpecification;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.modules.ModuleLoader;
import org.wildfly.camel.service.CamelComponentRegistryService;

/**
 * A DUP that sets the dependencies required for using Camel
 *
 * @author Thomas.Diesler@jboss.com
 * @since 20-Oct-2014
 */
public final class CamelDependenciesProcessor implements DeploymentUnitProcessor {

    private static final String GRAVIA = "org.jboss.gravia";
    private static final String APACHE_CAMEL = "org.apache.camel";
    private static final String WILDFLY_CAMEL = "org.wildfly.camel";

    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit unit = phaseContext.getDeploymentUnit();
        final ModuleLoader moduleLoader = unit.getAttachment(Attachments.SERVICE_MODULE_LOADER);
        final ModuleSpecification moduleSpec = unit.getAttachment(Attachments.MODULE_SPECIFICATION);
        moduleSpec.addUserDependency(new ModuleDependency(moduleLoader, ModuleIdentifier.create(GRAVIA), false, false, false, false));
        moduleSpec.addUserDependency(new ModuleDependency(moduleLoader, ModuleIdentifier.create(APACHE_CAMEL), false, false, false, false));
        moduleSpec.addUserDependency(new ModuleDependency(moduleLoader, ModuleIdentifier.create(WILDFLY_CAMEL), false, false, false, false));
        /*
        for (String compName : CamelComponentRegistryService.DEFAULT_COMPONENT_NAMES) {
            ModuleIdentifier modid = ModuleIdentifier.create(APACHE_CAMEL + ".component." + compName);
            moduleSpec.addUserDependency(new ModuleDependency(moduleLoader, modid, false, false, false, false));
        }
        */
    }

    public void undeploy(final DeploymentUnit context) {
    }

}
