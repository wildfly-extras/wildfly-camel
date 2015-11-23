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

import static org.wildfly.extension.camel.CamelLogger.LOGGER;

import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jboss.as.server.deployment.AttachmentList;
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

/**
 * A DUP that determines whether to enable the camel subsystem for a given deployment
 *
 * @author Thomas.Diesler@jboss.com
 * @since 20-May-2015
 */
public final class CamelEnablementProcessor implements DeploymentUnitProcessor {

    private static Map<String, DeploymentUnit> deploymentMap = new HashMap<>();

    public static DeploymentUnit getDeploymentUnitForName(String name) {
        synchronized (deploymentMap) {
            return deploymentMap.get(name);
        }
    }

    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {

        DeploymentUnit depUnit = phaseContext.getDeploymentUnit();
        CamelDeploymentSettings depSettings = depUnit.getAttachment(CamelDeploymentSettings.ATTACHMENT_KEY);
        if (depSettings == null) {
            depSettings = new CamelDeploymentSettings();
            depUnit.putAttachment(CamelDeploymentSettings.ATTACHMENT_KEY, depSettings);
        }

        synchronized (deploymentMap) {
            deploymentMap.put(getCanonicalDepUnitName(depUnit), depUnit);
        }

        // Skip wiring wfc for SwitchYard deployments
        VirtualFile rootFile = depUnit.getAttachment(Attachments.DEPLOYMENT_ROOT).getRoot();
        if (rootFile.getChild(CamelConstants.SWITCHYARD_MARKER_FILE).exists()) {
            depSettings.setEnabled(false);
            return;
        }

        // Skip wiring wfc for hawtio and resource adapter deployments
        String runtimeName = depUnit.getName();
        if ((runtimeName.startsWith("hawtio") && runtimeName.endsWith(".war")) || runtimeName.endsWith(".rar")) {
            depSettings.setEnabled(false);
            return;
        }

        // If this is an EAR deployment, set camel enabled if any sub deployment meets activation criteria
        AttachmentList<DeploymentUnit> subDeployments = depUnit.getAttachment(Attachments.SUB_DEPLOYMENTS);
        if (depUnit.getName().endsWith(".ear") && subDeployments != null) {
            for (DeploymentUnit subDepUnit : subDeployments) {
                CamelDeploymentSettings subDepSettings = new CamelDeploymentSettings();
                enableCamelIfRequired(subDepUnit, subDepSettings);
                if (subDepSettings.isEnabled()) {
                    depSettings.setEnabled(true);
                    return;
                }
            }
        }

        enableCamelIfRequired(depUnit, depSettings);
    }

    public void undeploy(DeploymentUnit depUnit) {
        synchronized (deploymentMap) {
            deploymentMap.remove(getCanonicalDepUnitName(depUnit));
        }
    }

    private void enableCamelIfRequired(DeploymentUnit depUnit, CamelDeploymentSettings depSettings) {
        AnnotationInstance annotation = getAnnotation(depUnit, "org.wildfly.extension.camel.CamelAware");
        if (annotation != null) {
            LOGGER.info("@CamelAware annotation found");
            AnnotationValue value = annotation.value();
            depSettings.setEnabled(value != null ? value.asBoolean() : true);
            return;
        }

        List<AnnotationInstance> annotations = getAnnotations(depUnit, "org.apache.camel.cdi.ContextName");
        if (!annotations.isEmpty()) {
            LOGGER.info("@ContextName annotation found");
            depSettings.setEnabled(true);
        }

        List<URL> contextURLs = depUnit.getAttachmentList(CamelConstants.CAMEL_CONTEXT_DESCRIPTORS_KEY);
        if (!contextURLs.isEmpty()) {
            LOGGER.info("Camel context descriptors found");
            depSettings.setEnabled(true);
        }
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

    private String getCanonicalDepUnitName(DeploymentUnit depUnit) {
        DeploymentUnit parent = depUnit.getParent();
        return parent != null ? parent.getName() + "." + depUnit.getName() : depUnit.getName();
    }
}
