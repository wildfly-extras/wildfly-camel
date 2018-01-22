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

import java.io.IOException;
import java.net.URL;

import org.apache.camel.CamelContext;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.annotation.CompositeIndex;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.DotName;
import org.jboss.vfs.VirtualFile;
import org.jboss.vfs.VirtualFileFilter;
import org.wildfly.extension.camel.CamelConstants;

/**
 * Processes deployments that can create a {@link CamelContext}.
 *
 * @author Thomas.Diesler@jboss.com
 * @since 22-Apr-2013
 */
public class CamelContextDescriptorsProcessor implements DeploymentUnitProcessor {

    @Override
    public void deploy(final DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {

        final DeploymentUnit depUnit = phaseContext.getDeploymentUnit();

        CamelDeploymentSettings depSettings = depUnit.getAttachment(CamelDeploymentSettings.ATTACHMENT_KEY);
        if (depSettings.isDisabledByJbossAll() || !depSettings.isDeploymentValid() || depUnit.getParent() != null) {
            return;
        }

        final String runtimeName = depUnit.getName();
        try {
            if (runtimeName.endsWith(CamelConstants.CAMEL_CONTEXT_FILE_SUFFIX)) {
                URL fileURL = depUnit.getAttachment(Attachments.DEPLOYMENT_CONTENTS).asFileURL();
                addConditionally(depUnit, fileURL);
            } else {
                VirtualFileFilter filter = new VirtualFileFilter() {
                    public boolean accepts(VirtualFile child) {
                        return child.isFile() && child.getName().endsWith(CamelConstants.CAMEL_CONTEXT_FILE_SUFFIX);
                    }
                };
                VirtualFile rootFile = depUnit.getAttachment(Attachments.DEPLOYMENT_ROOT).getRoot();
                for (VirtualFile vfile : rootFile.getChildrenRecursively(filter)) {
                    addConditionally(depUnit, vfile.asFileURL());
                }
            }
        } catch (IOException ex) {
            throw new IllegalStateException("Cannot create camel context: " + runtimeName, ex);
        }
    }

    public void addConditionally(DeploymentUnit depUnit, URL fileURL) {

        boolean skipResource = false;
        CamelDeploymentSettings depSettings = depUnit.getAttachment(CamelDeploymentSettings.ATTACHMENT_KEY);
        CompositeIndex index = depUnit.getAttachment(Attachments.COMPOSITE_ANNOTATION_INDEX);

        // [#1251] Add support for Spring based CamelContext injection
        for (AnnotationInstance aninst : index.getAnnotations(DotName.createSimple("org.apache.camel.cdi.ImportResource"))) {
            for (String resname : aninst.value().asStringArray()) {
                skipResource |= fileURL.getPath().endsWith(resname);
            }
        }

        if (skipResource == false) {
            depSettings.addCamelContextUrl(fileURL);
        }
    }

    @Override
    public void undeploy(final DeploymentUnit depUnit) {
    }
}
