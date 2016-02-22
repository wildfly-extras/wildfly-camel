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

import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jboss.as.server.deployment.AttachmentKey;
import org.jboss.modules.ModuleIdentifier;

import static org.wildfly.extension.camel.CamelLogger.LOGGER;

/**
 * Created by chirino on 2/23/15.
 */
public final class CamelDeploymentSettings {

    public static final AttachmentKey<CamelDeploymentSettings> ATTACHMENT_KEY = AttachmentKey.create(CamelDeploymentSettings.class);

    private List<CamelDeploymentSettings> children = new ArrayList<>();
    private List<ModuleIdentifier> dependencies = new ArrayList<>();
    private List<URL> camelContextUrls = new ArrayList<>();
    private boolean disabledByJbossAll;
    private boolean deploymentValid;
    private boolean camelAnnotationPresent;

    public boolean isEnabled() {
        // Disabling camel in jboss-all.xml takes precedence over other enablement criteria
        if (disabledByJbossAll) {
            return false;
        }

        // Verify that we have a valid deployment before performing other enablement checks
        if (deploymentValid) {
            if (!this.camelContextUrls.isEmpty()) {
                LOGGER.info("Camel context descriptors found");
                return true;
            }

            // Valid child implies valid parent
            for (CamelDeploymentSettings childDepSettings : this.children) {
                if (childDepSettings.isEnabled()) {
                    return true;
                }
            }

            // @ContextName or @CamelAware annotations are present
            if (camelAnnotationPresent) {
                return true;
            }

            // Declaration of individual camel components to enable in jboss-all.xml
            if (!dependencies.isEmpty()) {
                return true;
            }
        }

        return false;
    }

    public List<ModuleIdentifier> getModuleDependencies() {
        return Collections.unmodifiableList(dependencies);
    }

    public void addModuleDependency(String moduleSpec) {
        dependencies.add(ModuleIdentifier.create(moduleSpec));
        disabledByJbossAll = false;
    }

    public List<URL> getCamelContextUrls() {
        return Collections.unmodifiableList(camelContextUrls);
    }

    public void addCamelContextUrl(URL url) {
        camelContextUrls.add(url);
    }

    public boolean isDeploymentValid() {
        return deploymentValid;
    }

    public void setDeploymentValid(boolean deploymentValid) {
        this.deploymentValid = deploymentValid;
    }

    public List<CamelDeploymentSettings> getChildren() {
        return Collections.unmodifiableList(children);
    }

    public void addChild(CamelDeploymentSettings child) {
        children.add(child);
    }

    public boolean isDisabledByJbossAll() {
        return disabledByJbossAll;
    }

    public void setDisabledByJbossAll(boolean disabledByJbossAll) {
        this.disabledByJbossAll = disabledByJbossAll;
    }

    public boolean isCamelAnnotationPresent() {
        return camelAnnotationPresent;
    }

    public void setCamelAnnotationPresent(boolean camelAnnotationPresent) {
        this.camelAnnotationPresent = camelAnnotationPresent;
    }
}
