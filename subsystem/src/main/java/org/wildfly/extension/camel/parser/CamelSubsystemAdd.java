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

import java.util.List;

import org.jboss.as.controller.AbstractBoottimeAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.server.AbstractDeploymentChainStep;
import org.jboss.as.server.DeploymentProcessorTarget;
import org.jboss.as.server.deployment.Phase;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;
import org.wildfly.extension.camel.deployment.BindyAnnotationProcessor;
import org.wildfly.extension.camel.deployment.CamelContextActivationProcessor;
import org.wildfly.extension.camel.deployment.CamelContextCreateProcessor;
import org.wildfly.extension.camel.deployment.CamelDependenciesProcessor;
import org.wildfly.extension.camel.deployment.CamelIntegrationProcessor;
import org.wildfly.extension.camel.deployment.PackageScanResolverProcessor;
import org.wildfly.extension.camel.deployment.RepositoryContentInstallProcessor;
import org.wildfly.extension.camel.service.CamelBootstrapService;
import org.wildfly.extension.camel.service.CamelContextFactoryBindingService;
import org.wildfly.extension.camel.service.CamelContextFactoryService;
import org.wildfly.extension.camel.service.CamelContextRegistryBindingService;
import org.wildfly.extension.camel.service.CamelContextRegistryService;
import org.wildfly.extension.gravia.parser.GraviaSubsystemBootstrap;

/**
 * The Camel subsystem add update handler.
 *
 * @author Thomas.Diesler@jboss.com
 * @since 19-Apr-2013
 */
final class CamelSubsystemAdd extends AbstractBoottimeAddStepHandler {

    public static final int PARSE_CAMEL_ITEGRATION_PROVIDER           = Phase.PARSE_WAB_CONTEXT_FACTORY + 0x01;
    public static final int PARSE_BINDY_ANNOTATION_PROCESSOR          = PARSE_CAMEL_ITEGRATION_PROVIDER + 0x01;
    
    public static final int DEPENDENCIES_CAMEL              	      = Phase.DEPENDENCIES_LOGGING + 0x01;
    
    public static final int POST_MODULE_CAMEL_CONTEXT_CREATE          = Phase.POST_MODULE_LOCAL_HOME + 0x01;
    public static final int POST_MODULE_PACKAGE_SCAN_RESOLVER         = POST_MODULE_CAMEL_CONTEXT_CREATE + 0x01;
    
    public static final int INSTALL_REPOSITORY_CONTENT                = Phase.INSTALL_BUNDLE_ACTIVATE + 0x01;
    public static final int INSTALL_CAMEL_CONTEXT_ACTIVATION          = INSTALL_REPOSITORY_CONTENT + 0x01;

    private final SubsystemState subsystemState;

    public CamelSubsystemAdd(SubsystemState subsystemState) {
        this.subsystemState = subsystemState;
    }

    @Override
    protected void populateModel(ModelNode operation, ModelNode model) {
        model.setEmptyObject();
    }

    @Override
    protected void performBoottime(final OperationContext context, final ModelNode operation, final ModelNode model, final ServiceVerificationHandler verificationHandler, final List<ServiceController<?>> newControllers) {

        final GraviaSubsystemBootstrap graviaSubsystem = new GraviaSubsystemBootstrap();
        
        // Register subsystem services
        context.addStep(new OperationStepHandler() {
            @Override
            public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
                newControllers.addAll(graviaSubsystem.getSubsystemServices(context, verificationHandler));
                newControllers.add(CamelBootstrapService.addService(context.getServiceTarget(), verificationHandler));
                newControllers.add(CamelContextFactoryService.addService(context.getServiceTarget(), verificationHandler));
                newControllers.add(CamelContextFactoryBindingService.addService(context.getServiceTarget(), verificationHandler));
                newControllers.add(CamelContextRegistryService.addService(context.getServiceTarget(), subsystemState, verificationHandler));
                newControllers.add(CamelContextRegistryBindingService.addService(context.getServiceTarget(), verificationHandler));
                context.completeStep(OperationContext.RollbackHandler.NOOP_ROLLBACK_HANDLER);
            }
        }, OperationContext.Stage.RUNTIME);

        // Register deployment unit processors
        context.addStep(new AbstractDeploymentChainStep() {
            @Override
            public void execute(DeploymentProcessorTarget processorTarget) {
                graviaSubsystem.addDeploymentUnitProcessors(processorTarget);
                processorTarget.addDeploymentProcessor(CamelExtension.SUBSYSTEM_NAME, Phase.PARSE, PARSE_CAMEL_ITEGRATION_PROVIDER, new CamelIntegrationProcessor());
                processorTarget.addDeploymentProcessor(CamelExtension.SUBSYSTEM_NAME, Phase.PARSE, PARSE_BINDY_ANNOTATION_PROCESSOR, new BindyAnnotationProcessor());
                processorTarget.addDeploymentProcessor(CamelExtension.SUBSYSTEM_NAME, Phase.DEPENDENCIES, DEPENDENCIES_CAMEL, new CamelDependenciesProcessor());
                processorTarget.addDeploymentProcessor(CamelExtension.SUBSYSTEM_NAME, Phase.POST_MODULE, POST_MODULE_CAMEL_CONTEXT_CREATE, new CamelContextCreateProcessor());
                processorTarget.addDeploymentProcessor(CamelExtension.SUBSYSTEM_NAME, Phase.POST_MODULE, POST_MODULE_PACKAGE_SCAN_RESOLVER, new PackageScanResolverProcessor());
                processorTarget.addDeploymentProcessor(CamelExtension.SUBSYSTEM_NAME, Phase.INSTALL, INSTALL_CAMEL_CONTEXT_ACTIVATION, new CamelContextActivationProcessor());
                processorTarget.addDeploymentProcessor(CamelExtension.SUBSYSTEM_NAME, Phase.INSTALL, INSTALL_REPOSITORY_CONTENT, new RepositoryContentInstallProcessor());
            }
        }, OperationContext.Stage.RUNTIME);
    }

    @Override
    protected boolean requiresRuntimeVerification() {
        return false;
    }
}
