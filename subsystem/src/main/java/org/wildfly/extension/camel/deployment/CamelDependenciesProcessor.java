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
import org.jboss.vfs.VirtualFile;
import org.wildfly.extension.camel.CamelConstants;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Map;
import java.util.Properties;

/**
 * A DUP that sets the dependencies required for using Camel
 *
 * @author Thomas.Diesler@jboss.com
 * @since 20-Oct-2014
 */
public final class CamelDependenciesProcessor implements DeploymentUnitProcessor {

    private static final String GRAVIA = "org.jboss.gravia";
    private static final String APACHE_CAMEL = "org.apache.camel";
    private static final String APACHE_CAMEL_COMPONENT = "org.apache.camel.component";
    private static final String WILDFLY_CAMEL = "org.wildfly.extension.camel";
    private static final String MODULE_PREFIX = "module:";
    private static final String CAMEL_PREFIX = "camel-";

    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        
        DeploymentUnit unit = phaseContext.getDeploymentUnit();
        
        // No camel module dependencies for hawtio
        String runtimeName = unit.getName();
        if (runtimeName.startsWith("hawtio") && runtimeName.endsWith(".war"))
            return;
        
        ModuleLoader moduleLoader = unit.getAttachment(Attachments.SERVICE_MODULE_LOADER);
        ModuleSpecification moduleSpec = unit.getAttachment(Attachments.MODULE_SPECIFICATION);
        moduleSpec.addUserDependency(new ModuleDependency(moduleLoader, ModuleIdentifier.create(GRAVIA), false, false, false, false));
        moduleSpec.addUserDependency(new ModuleDependency(moduleLoader, ModuleIdentifier.create(WILDFLY_CAMEL), false, false, false, false));

        // Add camel-core and the configured components
        moduleSpec.addUserDependency(new ModuleDependency(moduleLoader, ModuleIdentifier.create(APACHE_CAMEL), false, false, true, false));

        Properties componentModules = new Properties();
        componentModules.setProperty("module:"+APACHE_CAMEL_COMPONENT, "");

        // Allow deployments to customize which camel components are added to the classpath
        try {
            if ( !runtimeName.endsWith(CamelConstants.CAMEL_CONTEXT_FILE_SUFFIX) ) {
                VirtualFile rootFile = unit.getAttachment(Attachments.DEPLOYMENT_ROOT).getRoot();
                VirtualFile child = rootFile.getChild(CamelConstants.CAMEL_COMPONENTS_FILE_NAME);
                if( child.isFile() ) {
                    componentModules.clear();
                    URL url = child.asFileURL();
                    InputStream is = url.openStream();
                    try {
                        componentModules.load(is);
                    } finally {
                        is.close();
                    }

                    // TODO figure out if we can validate all
                    // user configured modules exist and provide a
                    // helpful error pointing at their config file, if
                    // it's invalid.
                }
            }
        } catch (IOException ex) {
        }

        for (Map.Entry<Object, Object> entry : componentModules.entrySet()) {
            String name = entry.getKey().toString();


            if( name.startsWith(MODULE_PREFIX) ) {
                // if the name starts with 'module:' then it's a fully qualified module name.
                name = name.substring(MODULE_PREFIX.length());
            } else {
                // else, it's a Camel component name like 'camel-mqtt'. Lets convert it
                // to a module name.
                if( name.startsWith(CAMEL_PREFIX) ) {
                    name = name.substring(CAMEL_PREFIX.length());
                }
                name = APACHE_CAMEL_COMPONENT + "." + name;
            }
            moduleSpec.addUserDependency(new ModuleDependency(moduleLoader, ModuleIdentifier.create(name), false, false, true, false));
        }

        // Camel-CDI Integration
        ModuleDependency moddep = new ModuleDependency(moduleLoader, ModuleIdentifier.create("org.apache.camel.component.cdi"), false, false, false, false);
        moddep.addImportFilter(PathFilters.getMetaInfSubdirectoriesFilter(), true);
        moddep.addImportFilter(PathFilters.getMetaInfFilter(), true);
        moduleSpec.addUserDependency(moddep);
        moddep = new ModuleDependency(moduleLoader, ModuleIdentifier.create("org.apache.deltaspike.core.api"), false, false, false, false);
        moddep.addImportFilter(PathFilters.getMetaInfSubdirectoriesFilter(), true);
        moddep.addImportFilter(PathFilters.getMetaInfFilter(), true);
        moduleSpec.addUserDependency(moddep);
        moddep = new ModuleDependency(moduleLoader, ModuleIdentifier.create("org.apache.deltaspike.core.impl"), false, false, false, false);
        moddep.addImportFilter(PathFilters.getMetaInfSubdirectoriesFilter(), true);
        moddep.addImportFilter(PathFilters.getMetaInfFilter(), true);
        moduleSpec.addUserDependency(moddep);
    }

    public void undeploy(DeploymentUnit context) {
    }

}
