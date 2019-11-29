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

import static org.wildfly.extension.camel.CamelLogger.LOGGER;

import java.util.List;
import java.util.function.Consumer;

import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.annotation.CompositeIndex;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.AnnotationTarget.Kind;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.Type;

/**
 * Takes over an available {@link CamelDeploymentSettings.Builder} or creates and attaches one, adding all information
 * inferrable from the {@link DeploymentUnit} to it.
 *
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 */
public final class CamelDeploymentSettingsBuilderProcessor implements DeploymentUnitProcessor {

    private static final String[] ACTIVATION_ANNOTATIONS = {
        "org.wildfly.extension.camel.CamelAware",
        "org.apache.camel.cdi.Uri",
        "org.apache.camel.cdi.ImportResource",
        "javax.inject.Inject",
        "javax.inject.Named",
        "javax.enterprise.context.ApplicationScoped",
        "javax.enterprise.inject.Produces",
    };
    private static final String CAMEL_PACKAGE_PREFIX = "org.apache.camel";

    public static String getDeploymentName(final DeploymentUnit depUnit) {
        DeploymentUnit parent = depUnit.getParent();
        return parent != null ? parent.getName() + "." + depUnit.getName() : depUnit.getName();
    }

    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {

        final DeploymentUnit depUnit = phaseContext.getDeploymentUnit();

        CamelDeploymentSettings.Builder depSettingsBuilder = depUnit.getAttachment(CamelDeploymentSettings.BUILDER_ATTACHMENT_KEY);
        if (depSettingsBuilder == null) {
            depSettingsBuilder = new CamelDeploymentSettings.Builder();
            depUnit.putAttachment(CamelDeploymentSettings.BUILDER_ATTACHMENT_KEY, depSettingsBuilder);
        } else if (depSettingsBuilder.isDisabledByJbossAll()) {
            // Camel is explicitly disabled in jboss-all.xml
            return;
        }

        depSettingsBuilder.deploymentName(getDeploymentName(depUnit));
        depSettingsBuilder.deploymentValid(isDeploymentValid(depUnit));
        depSettingsBuilder.camelActivationAnnotationPresent(hasCamelActivationAnnotations(depUnit));

        final DeploymentUnit parentDepUnit = depUnit.getParent();
        if (parentDepUnit != null) {
            final CamelDeploymentSettings.Builder parentDepSettingsBuilder = parentDepUnit.getAttachment(CamelDeploymentSettings.BUILDER_ATTACHMENT_KEY);
            if (parentDepSettingsBuilder != null) {
                // This consumer is called by CamelDeploymentSettings.Builder.build() right after the child settings are built
                Consumer<CamelDeploymentSettings> consumer = (CamelDeploymentSettings ds) -> {
                    depUnit.removeAttachment(CamelDeploymentSettings.BUILDER_ATTACHMENT_KEY);
                    depUnit.putAttachment(CamelDeploymentSettings.ATTACHMENT_KEY, ds);
                };
                parentDepSettingsBuilder.child(depSettingsBuilder, consumer);
            }
        }
    }

    public void undeploy(final DeploymentUnit depUnit) {
    }

    private boolean isDeploymentValid(final DeploymentUnit depUnit) {

        boolean result = true;

        // Skip wiring wfc for hawtio and resource adapter deployments
        String runtimeName = depUnit.getName();
        if (runtimeName.startsWith("hawtio") && runtimeName.endsWith(".war") || runtimeName.endsWith(".rar")) {
            result = false;
        }

        return result;
    }

    private boolean hasCamelActivationAnnotations(final DeploymentUnit depUnit) {
        boolean result = false;

        // Search for Camel activation annotations
        for (String annotationClassName : ACTIVATION_ANNOTATIONS) {
            if (annotationClassName.equals("org.wildfly.extension.camel.CamelAware")) {
                AnnotationInstance annotation = getAnnotation(depUnit, annotationClassName);
                if (annotation != null) {
                    LOGGER.debug("@CamelAware annotation found");
                    AnnotationValue value = annotation.value();
                    result = value != null ? value.asBoolean() : true;
                    if (result) {
                        break;
                    }
                }
            } else {
                List<AnnotationInstance> annotations = getAnnotations(depUnit, annotationClassName);
                if (!annotations.isEmpty()) {
                    if (annotationClassName.startsWith("org.apache.camel.cdi")) {
                        LOGGER.debug("{} Camel CDI annotation found", annotations.get(0).toString(true));
                        result = true;
                        break;
                    } else {
                        // For non camel-cdi activation annotations we need to check whether the annotated types implement camel APIs
                        for (AnnotationInstance instance : annotations) {
                            if (annotationTargetIsCamelApi(instance.target())){
                                LOGGER.debug("{} annotation {} found on Camel API", instance.target().kind(), instance.name());
                                result = true;
                                break;
                            }
                        }
                    }
                }
            }
        }

        return result;
    }

    private boolean annotationTargetIsCamelApi(AnnotationTarget target) {
        /**
         * Verify whether an annotation is applied to org.apache.camel types. If the declaring class
         * has the org.apache.camel package prefix, it is ignored since we are only interested in
         * annotations declared within user classes.
         */
        if (target != null) {
            if (target.kind().equals(Kind.CLASS)) {
                ClassInfo classInfo = target.asClass();
                if (isCamelApi(classInfo.name())) {
                    return false;
                }

                Type superType = classInfo.superClassType();
                if (superType != null && isCamelApi(superType.name())) {
                    return true;
                }

                return classInfo.interfaceNames().stream().anyMatch(interfaceName -> isCamelApi(interfaceName));
            } else if (target.kind().equals(Kind.FIELD)) {
                FieldInfo fieldInfo = target.asField();
                return !isCamelApi(fieldInfo.declaringClass().name()) && isCamelApi(fieldInfo.type().name());
            } else if (target.kind().equals(Kind.METHOD)) {
                MethodInfo methodInfo = target.asMethod();
                return !isCamelApi(methodInfo.declaringClass().name()) && isCamelApi(methodInfo.returnType().name());
            }
        }
        return false;
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

    private boolean isCamelApi(DotName name) {
        return name.toString().startsWith(CAMEL_PACKAGE_PREFIX);
    }
}
