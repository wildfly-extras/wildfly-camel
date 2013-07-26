/*
 * #%L
 * Wildfly Camel Subsystem
 * %%
 * Copyright (C) 2013 JBoss by Red Hat
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as 
 * published by the Free Software Foundation, either version 2.1 of the 
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public 
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-2.1.html>.
 * #L%
 */


package org.wildfly.camel.parser;

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
import org.wildfly.camel.deployment.CamelComponentRegistrationProcessor;
import org.wildfly.camel.deployment.CamelContextActivationProcessor;
import org.wildfly.camel.deployment.CamelContextCreateProcessor;
import org.wildfly.camel.deployment.CamelContextRegistrationProcessor;
import org.wildfly.camel.deployment.CamelIntegrationProcessor;
import org.wildfly.camel.deployment.RepositoryContentInstallProcessor;
import org.wildfly.camel.service.CamelBootstrapService;
import org.wildfly.camel.service.CamelComponentRegistryService;
import org.wildfly.camel.service.CamelContextFactoryBindingService;
import org.wildfly.camel.service.CamelContextFactoryService;
import org.wildfly.camel.service.CamelContextRegistryBindingService;
import org.wildfly.camel.service.CamelContextRegistryService;
import org.wildfly.camel.service.EnvironmentService;
import org.wildfly.camel.service.ProvisionerService;
import org.wildfly.camel.service.RepositoryService;
import org.wildfly.camel.service.ResolverService;

/**
 * The Camel subsystem add update handler.
 *
 * @author Thomas.Diesler@jboss.com
 * @since 19-Apr-2013
 */
final class CamelSubsystemAdd extends AbstractBoottimeAddStepHandler {

    public static final int PARSE_CAMEL_ITEGRATION_PROVIDER           = Phase.PARSE_OSGI_SUBSYSTEM_ACTIVATOR + 0x01;
    public static final int POST_MODULE_CAMEL_CONTEXT_CREATE          = Phase.POST_MODULE_LOCAL_HOME + 0x01;
    public static final int INSTALL_REPOSITORY_CONTENT                = Phase.INSTALL_BUNDLE_ACTIVATE + 0x01;
    public static final int INSTALL_CAMEL_COMPONENT_REGISTRATION      = Phase.INSTALL_BUNDLE_ACTIVATE + 0x02;
    public static final int INSTALL_CAMEL_CONTEXT_REGISTRATION        = Phase.INSTALL_BUNDLE_ACTIVATE + 0x03;
    public static final int INSTALL_CAMEL_CONTEXT_ACTIVATION          = Phase.INSTALL_BUNDLE_ACTIVATE + 0x04;

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

        // Register subsystem services
        context.addStep(new OperationStepHandler() {
            @Override
            public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
                newControllers.add(CamelBootstrapService.addService(context.getServiceTarget(), verificationHandler));
                newControllers.add(CamelComponentRegistryService.addService(context.getServiceTarget(), verificationHandler));
                newControllers.add(CamelContextFactoryService.addService(context.getServiceTarget(), verificationHandler));
                newControllers.add(CamelContextFactoryBindingService.addService(context.getServiceTarget(), verificationHandler));
                newControllers.add(CamelContextRegistryService.addService(context.getServiceTarget(), subsystemState, verificationHandler));
                newControllers.add(CamelContextRegistryBindingService.addService(context.getServiceTarget(), verificationHandler));
                newControllers.add(EnvironmentService.addService(context.getServiceTarget(), verificationHandler));
                newControllers.add(ResolverService.addService(context.getServiceTarget(), verificationHandler));
                newControllers.add(RepositoryService.addService(context.getServiceTarget(), verificationHandler));
                newControllers.add(ProvisionerService.addService(context.getServiceTarget(), verificationHandler));
                context.completeStep(OperationContext.RollbackHandler.NOOP_ROLLBACK_HANDLER);
            }
        }, OperationContext.Stage.RUNTIME);

        // Register deployment unit processors
        context.addStep(new AbstractDeploymentChainStep() {
            @Override
            public void execute(DeploymentProcessorTarget processorTarget) {
                processorTarget.addDeploymentProcessor(CamelExtension.SUBSYSTEM_NAME, Phase.PARSE, PARSE_CAMEL_ITEGRATION_PROVIDER, new CamelIntegrationProcessor());
                processorTarget.addDeploymentProcessor(CamelExtension.SUBSYSTEM_NAME, Phase.POST_MODULE, POST_MODULE_CAMEL_CONTEXT_CREATE, new CamelContextCreateProcessor());
                processorTarget.addDeploymentProcessor(CamelExtension.SUBSYSTEM_NAME, Phase.INSTALL, INSTALL_CAMEL_COMPONENT_REGISTRATION, new CamelComponentRegistrationProcessor());
                processorTarget.addDeploymentProcessor(CamelExtension.SUBSYSTEM_NAME, Phase.INSTALL, INSTALL_CAMEL_CONTEXT_REGISTRATION, new CamelContextRegistrationProcessor());
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
