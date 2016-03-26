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

import java.util.List;

import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.module.ModuleDependency;
import org.jboss.as.server.deployment.module.ModuleSpecification;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.modules.ModuleLoader;
import org.jboss.modules.filter.PathFilters;

/**
 * A DUP that sets the dependencies required for using Camel
 *
 * @author Thomas.Diesler@jboss.com
 * @since 20-Oct-2014
 */
public final class CamelDependenciesProcessor implements DeploymentUnitProcessor {

    private static final String GRAVIA_MODULE = "org.jboss.gravia";
    private static final String APACHE_CAMEL_MODULE = "org.apache.camel";
    private static final String APACHE_CAMEL_COMPONENT_MODULE = "org.apache.camel.component";
    private static final String WILDFLY_CAMEL_MODULE = "org.wildfly.extension.camel";

    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {

        DeploymentUnit depUnit = phaseContext.getDeploymentUnit();
        CamelDeploymentSettings depSettings = depUnit.getAttachment(CamelDeploymentSettings.ATTACHMENT_KEY);

        // Camel dependencies disabled
        if (!depSettings.isEnabled()) {
            return;
        }

        ModuleLoader moduleLoader = depUnit.getAttachment(Attachments.SERVICE_MODULE_LOADER);
        ModuleSpecification moduleSpec = depUnit.getAttachment(Attachments.MODULE_SPECIFICATION);
        moduleSpec.addUserDependency(new ModuleDependency(moduleLoader, ModuleIdentifier.create(GRAVIA_MODULE), false, false, false, false));
        moduleSpec.addUserDependency(new ModuleDependency(moduleLoader, ModuleIdentifier.create(WILDFLY_CAMEL_MODULE), false, false, false, false));

        // Add camel aggregator dependency
        ModuleDependency moddep = new ModuleDependency(moduleLoader, ModuleIdentifier.create(APACHE_CAMEL_MODULE), false, false, true, false);
        moddep.addImportFilter(PathFilters.getMetaInfFilter(), true);
        moduleSpec.addUserDependency(moddep);

        List<ModuleIdentifier> deploymentDefinedModules = depSettings.getModuleDependencies();
        if (!deploymentDefinedModules.isEmpty()) {
            for (ModuleIdentifier modid : deploymentDefinedModules) {
                moduleSpec.addUserDependency(new ModuleDependency(moduleLoader, modid, false, false, true, false));
            }
        } else {
            moddep = new ModuleDependency(moduleLoader, ModuleIdentifier.create(APACHE_CAMEL_COMPONENT_MODULE), false, false, true, false);
            moddep.addImportFilter(PathFilters.getMetaInfFilter(), true);
            moddep.addImportFilter(PathFilters.isOrIsChildOf("META-INF/cxf"), true);
            moduleSpec.addUserDependency(moddep);

            moddep = new ModuleDependency(moduleLoader, ModuleIdentifier.create("org.apache.camel.component.cdi"), true, false, false, false);
            moddep.addImportFilter(PathFilters.getMetaInfSubdirectoriesFilter(), true);
            moddep.addImportFilter(PathFilters.getMetaInfFilter(), true);
            moduleSpec.addUserDependency(moddep);
        }
    }

    public void undeploy(DeploymentUnit context) {
    }
}
