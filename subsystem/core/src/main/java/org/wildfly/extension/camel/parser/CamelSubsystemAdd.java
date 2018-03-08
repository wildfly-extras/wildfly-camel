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

package org.wildfly.extension.camel.parser;

import java.util.function.Consumer;

import org.jboss.as.controller.AbstractBoottimeAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.server.AbstractDeploymentChainStep;
import org.jboss.as.server.DeploymentProcessorTarget;
import org.jboss.as.server.deployment.Phase;
import org.jboss.dmr.ModelNode;
import org.wildfly.extension.camel.CamelSubsytemExtension;
import org.wildfly.extension.camel.deployment.CamelContextActivationProcessor;
import org.wildfly.extension.camel.deployment.CamelContextBootstrapProcessor;
import org.wildfly.extension.camel.deployment.CamelContextDescriptorsProcessor;
import org.wildfly.extension.camel.deployment.CamelDependenciesProcessor;
import org.wildfly.extension.camel.deployment.CamelDeploymentSettingsProcessor;
import org.wildfly.extension.camel.deployment.CamelIntegrationProcessor;
import org.wildfly.extension.camel.deployment.PackageScanResolverProcessor;
import org.wildfly.extension.camel.service.CamelBootstrapService;
import org.wildfly.extension.camel.service.CamelContextFactoryService;
import org.wildfly.extension.camel.service.CamelContextRegistryService;
import org.wildfly.extension.camel.service.ContextCreateHandlerRegistryService;
import org.wildfly.extension.gravia.parser.GraviaSubsystemBootstrap;

/**
 * The Camel subsystem add update handler.
 *
 * @author Thomas.Diesler@jboss.com
 * @since 19-Apr-2013
 */
public final class CamelSubsystemAdd extends AbstractBoottimeAddStepHandler {

    public static final int STRUCTURE_REGISTER_CAMEL_INTEGRATION = Phase.STRUCTURE_PARSE_JBOSS_ALL_XML - 0x01;

    public static final int PARSE_DEPLOYMENT_SETTINGS = Phase.PARSE_COMPOSITE_ANNOTATION_INDEX + 0x01;
    public static final int PARSE_CAMEL_CONTEXT_DESCRIPTORS = PARSE_DEPLOYMENT_SETTINGS + 0x01;

    public static final int DEPENDENCIES_CAMEL_INTEGRATION = Phase.DEPENDENCIES_LOGGING + 0x01;
    public static final int DEPENDENCIES_CAMEL_WIRINGS = DEPENDENCIES_CAMEL_INTEGRATION + 0x01;

    public static final int INSTALL_PACKAGE_SCAN_RESOLVER = Phase.INSTALL_WS_DEPLOYMENT_ASPECTS + 0x01;
    public static final int INSTALL_CDI_BEAN_ARCHIVE_PROCESSOR = INSTALL_PACKAGE_SCAN_RESOLVER + 0x01;
    public static final int INSTALL_CAMEL_CONTEXT_CREATE = INSTALL_CDI_BEAN_ARCHIVE_PROCESSOR + 0x01;
    public static final int INSTALL_CONTEXT_ACTIVATION = INSTALL_CAMEL_CONTEXT_CREATE + 0x01;

    private final SubsystemState subsystemState;

    public CamelSubsystemAdd(SubsystemState subsystemState) {
        this.subsystemState = subsystemState;
    }

    @Override
    protected void populateModel(ModelNode operation, ModelNode model) {
        model.setEmptyObject();
    }

    @Override
    protected void performBoottime(final OperationContext context, final ModelNode operation, final ModelNode model) {

        final GraviaSubsystemBootstrap gravia = new GraviaSubsystemBootstrap();

        gravia.getSubsystemServices(context);
        CamelBootstrapService.addService(context.getServiceTarget());
        CamelContextFactoryService.addService(context.getServiceTarget());
        CamelContextRegistryService.addService(context.getServiceTarget(), subsystemState);
        ContextCreateHandlerRegistryService.addService(context.getServiceTarget(), subsystemState);

        subsystemState.processExtensions(new Consumer<CamelSubsytemExtension>() {
            @Override
            public void accept(CamelSubsytemExtension plugin) {
                plugin.addExtensionServices(context.getServiceTarget(), subsystemState);
            }
        });

        // Register deployment unit processors
        context.addStep(new AbstractDeploymentChainStep() {
            @Override
            public void execute(final DeploymentProcessorTarget processorTarget) {
                gravia.addDeploymentUnitProcessors(processorTarget);
                processorTarget.addDeploymentProcessor(CamelExtension.SUBSYSTEM_NAME, Phase.PARSE, PARSE_DEPLOYMENT_SETTINGS, new CamelDeploymentSettingsProcessor());
                processorTarget.addDeploymentProcessor(CamelExtension.SUBSYSTEM_NAME, Phase.PARSE, PARSE_CAMEL_CONTEXT_DESCRIPTORS, new CamelContextDescriptorsProcessor());
                processorTarget.addDeploymentProcessor(CamelExtension.SUBSYSTEM_NAME, Phase.DEPENDENCIES, DEPENDENCIES_CAMEL_INTEGRATION, new CamelIntegrationProcessor());
                processorTarget.addDeploymentProcessor(CamelExtension.SUBSYSTEM_NAME, Phase.DEPENDENCIES, DEPENDENCIES_CAMEL_WIRINGS, new CamelDependenciesProcessor());
                processorTarget.addDeploymentProcessor(CamelExtension.SUBSYSTEM_NAME, Phase.INSTALL, INSTALL_PACKAGE_SCAN_RESOLVER, new PackageScanResolverProcessor());
                processorTarget.addDeploymentProcessor(CamelExtension.SUBSYSTEM_NAME, Phase.INSTALL, INSTALL_CAMEL_CONTEXT_CREATE, new CamelContextBootstrapProcessor());
                processorTarget.addDeploymentProcessor(CamelExtension.SUBSYSTEM_NAME, Phase.INSTALL, INSTALL_CONTEXT_ACTIVATION, new CamelContextActivationProcessor());
                subsystemState.processExtensions(new Consumer<CamelSubsytemExtension>() {
                    @Override
                    public void accept(CamelSubsytemExtension plugin) {
                        plugin.addDeploymentProcessor(processorTarget, subsystemState);
                    }
                });
            }
        }, OperationContext.Stage.RUNTIME);
    }
}
