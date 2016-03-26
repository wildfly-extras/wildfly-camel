/*
 * #%L
 * Wildfly Camel :: Subsystem
 * %%
 * Copyright (C) 2013 - 2016 RedHat
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.annotation.CompositeIndex;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.DotName;
import org.jboss.vfs.VirtualFile;
import org.wildfly.extension.camel.CamelConstants;

import static org.wildfly.extension.camel.CamelLogger.LOGGER;

public final class CamelDeploymentSettingsProcessor implements DeploymentUnitProcessor {

    private static Map<String, CamelDeploymentSettings> deploymentSettingsMap = new HashMap<>();

    public static CamelDeploymentSettings getDeploymentSettings(String name) {
        synchronized (deploymentSettingsMap) {
            return deploymentSettingsMap.get(name);
        }
    }

    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {

        final DeploymentUnit depUnit = phaseContext.getDeploymentUnit();

        CamelDeploymentSettings depSettings = depUnit.getAttachment(CamelDeploymentSettings.ATTACHMENT_KEY);
        if (depSettings == null) {
            depSettings = new CamelDeploymentSettings();
            depUnit.putAttachment(CamelDeploymentSettings.ATTACHMENT_KEY, depSettings);
        } else {
            if (depSettings.isDisabledByJbossAll()) {
                // Camel is explicitly disabled in jboss-all.xml
                return;
            }
        }

        depSettings.setDeploymentValid(isDeploymentValid(depUnit));
        depSettings.setCamelAnnotationPresent(hasCamelActivationAnnotations(depUnit));

        synchronized (deploymentSettingsMap) {
            deploymentSettingsMap.put(getDeploymentName(depUnit), depSettings);
        }

        final DeploymentUnit parent = depUnit.getParent();
        if (parent != null) {
            final String parentDeploymentName = getDeploymentName(parent);
            CamelDeploymentSettings parentDepSettings = getDeploymentSettings(parentDeploymentName);
            if (parentDepSettings != null) {
                parentDepSettings.addChild(depSettings);
            }
        }
    }

    public void undeploy(final DeploymentUnit depUnit) {
        synchronized (deploymentSettingsMap) {
            deploymentSettingsMap.remove(getDeploymentName(depUnit));
        }
    }

    private String getDeploymentName(final DeploymentUnit depUnit) {
        DeploymentUnit parent = depUnit.getParent();
        return parent != null ? parent.getName() + "." + depUnit.getName() : depUnit.getName();
    }

    private boolean isDeploymentValid(final DeploymentUnit depUnit) {

        boolean result = true;

        // Skip wiring wfc for SwitchYard deployments
        VirtualFile rootFile = depUnit.getAttachment(Attachments.DEPLOYMENT_ROOT).getRoot();
        if (rootFile.getChild(CamelConstants.SWITCHYARD_MARKER_FILE).exists()) {
            result = false;
        }

        // Skip wiring wfc for hawtio and resource adapter deployments
        String runtimeName = depUnit.getName();
        if (runtimeName.startsWith("hawtio") && runtimeName.endsWith(".war") || runtimeName.endsWith(".rar")) {
            result = false;
        }

        return result;
    }

    private boolean hasCamelActivationAnnotations(final DeploymentUnit depUnit) {

        boolean result = false;

        // Search for CamelAware annotations
        AnnotationInstance annotation = getAnnotation(depUnit, "org.wildfly.extension.camel.CamelAware");
        if (annotation != null) {
            LOGGER.debug("@CamelAware annotation found");
            AnnotationValue value = annotation.value();
            result = value != null ? value.asBoolean() : true;
        }

        // Search for Camel CDI component annotations
        List<AnnotationInstance> annotations = getAnnotations(depUnit, "org.apache.camel.cdi.ContextName");
        if (!annotations.isEmpty()) {
            LOGGER.debug("@ContextName annotation found");
            result = true;
        }
        annotations = getAnnotations(depUnit, "org.apache.camel.cdi.Uri");
        if (!annotations.isEmpty()) {
            LOGGER.debug("@Uri annotation found");
            result = true;
        }

        return result;
    }

    private List<AnnotationInstance> getAnnotations(DeploymentUnit depUnit, String className) {
        CompositeIndex index = depUnit.getAttachment(Attachments.COMPOSITE_ANNOTATION_INDEX);
        return index.getAnnotations(DotName.createSimple(className));
    }

    private AnnotationInstance getAnnotation(DeploymentUnit depUnit, String className) {
        List<AnnotationInstance> annotations = getAnnotations(depUnit, className);
        if (annotations.size() > 1) {
            LOGGER.warn("Multiple annotations found: {}", annotations);
        }
        return annotations.size() > 0 ? annotations.get(0) : null;
    }
}
