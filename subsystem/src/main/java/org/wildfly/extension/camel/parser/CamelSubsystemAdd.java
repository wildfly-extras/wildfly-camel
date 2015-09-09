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

import java.util.ArrayList;
import java.util.List;

import org.jboss.as.controller.AbstractBoottimeAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.server.AbstractDeploymentChainStep;
import org.jboss.as.server.DeploymentProcessorTarget;
import org.jboss.as.server.deployment.Phase;
import org.jboss.as.server.deployment.jbossallxml.JBossAllXmlParserRegisteringProcessor;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;
import org.wildfly.extension.camel.deployment.CamelContextActivationProcessor;
import org.wildfly.extension.camel.deployment.CamelContextCreateProcessor;
import org.wildfly.extension.camel.deployment.CamelDependenciesProcessor;
import org.wildfly.extension.camel.deployment.CamelDeploymentSettings;
import org.wildfly.extension.camel.deployment.CamelIntegrationParser;
import org.wildfly.extension.camel.deployment.CamelIntegrationProcessor;
import org.wildfly.extension.camel.deployment.PackageScanResolverProcessor;
import org.wildfly.extension.camel.deployment.CamelEnablementProcessor;
import org.wildfly.extension.camel.service.CamelBootstrapService;
import org.wildfly.extension.camel.service.CamelContextFactoryBindingService;
import org.wildfly.extension.camel.service.CamelContextFactoryService;
import org.wildfly.extension.camel.service.CamelContextRegistryBindingService;
import org.wildfly.extension.camel.service.CamelContextRegistryService;
import org.wildfly.extension.camel.service.ContextCreateHandlerRegistryService;
import org.wildfly.extension.gravia.parser.GraviaSubsystemBootstrap;

/**
 * The Camel subsystem add update handler.
 *
 * @author Thomas.Diesler@jboss.com
 * @since 19-Apr-2013
 */
final class CamelSubsystemAdd extends AbstractBoottimeAddStepHandler {

    public static final int STRUCTURE_REGISTER_CAMEL_INTEGRATION = Phase.STRUCTURE_PARSE_JBOSS_ALL_XML - 0x01;

    public static final int PARSE_CAMEL_INTEGRATION_PROVIDER = Phase.PARSE_OSGI_SUBSYSTEM_ACTIVATOR + 0x01;

    public static final int DEPENDENCIES_CAMEL_ENABLEMENT = Phase.DEPENDENCIES_LOGGING + 0x01;
    public static final int DEPENDENCIES_CAMEL = Phase.DEPENDENCIES_LOGGING + 0x02;

    public static final int POST_MODULE_PACKAGE_SCAN_RESOLVER = Phase.POST_MODULE_LOCAL_HOME + 0x01;
    public static final int POST_MODULE_CAMEL_CONTEXT_CREATE = Phase.POST_MODULE_LOCAL_HOME + 0x02;

    public static final int INSTALL_CAMEL_CONTEXT_ACTIVATION = Phase.INSTALL_BUNDLE_ACTIVATE + 0x01;

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

        final GraviaSubsystemBootstrap graviaSubsystem = new ReducedGraviaSubsystemBootstrap();
        final JBossAllXmlParserRegisteringProcessor<CamelDeploymentSettings> parser =
                new JBossAllXmlParserRegisteringProcessor<>(CamelIntegrationParser.ROOT_ELEMENT, CamelDeploymentSettings.ATTACHMENT_KEY, new CamelIntegrationParser());

        graviaSubsystem.getSubsystemServices(context);
        CamelBootstrapService.addService(context.getServiceTarget());
        CamelContextFactoryService.addService(context.getServiceTarget());
        CamelContextFactoryBindingService.addService(context.getServiceTarget());
        CamelContextRegistryService.addService(context.getServiceTarget(), subsystemState);
        CamelContextRegistryBindingService.addService(context.getServiceTarget());
        ContextCreateHandlerRegistryService.addService(context.getServiceTarget());

        // Register deployment unit processors
        context.addStep(new AbstractDeploymentChainStep() {
            @Override
            public void execute(DeploymentProcessorTarget processorTarget) {
                processorTarget.addDeploymentProcessor(CamelExtension.SUBSYSTEM_NAME, Phase.STRUCTURE, STRUCTURE_REGISTER_CAMEL_INTEGRATION, parser);
                processorTarget.addDeploymentProcessor(CamelExtension.SUBSYSTEM_NAME, Phase.PARSE, STRUCTURE_REGISTER_CAMEL_INTEGRATION, new CamelIntegrationProcessor());
                processorTarget.addDeploymentProcessor(CamelExtension.SUBSYSTEM_NAME, Phase.DEPENDENCIES, DEPENDENCIES_CAMEL_ENABLEMENT, new CamelEnablementProcessor());
                processorTarget.addDeploymentProcessor(CamelExtension.SUBSYSTEM_NAME, Phase.DEPENDENCIES, DEPENDENCIES_CAMEL, new CamelDependenciesProcessor());
                processorTarget.addDeploymentProcessor(CamelExtension.SUBSYSTEM_NAME, Phase.POST_MODULE, POST_MODULE_CAMEL_CONTEXT_CREATE, new CamelContextCreateProcessor());
                processorTarget.addDeploymentProcessor(CamelExtension.SUBSYSTEM_NAME, Phase.POST_MODULE, POST_MODULE_PACKAGE_SCAN_RESOLVER, new PackageScanResolverProcessor());
                processorTarget.addDeploymentProcessor(CamelExtension.SUBSYSTEM_NAME, Phase.INSTALL, INSTALL_CAMEL_CONTEXT_ACTIVATION, new CamelContextActivationProcessor());
            }
        }, OperationContext.Stage.RUNTIME);
    }

    @Override
    protected boolean requiresRuntimeVerification() {
        return false;
    }

    final class ReducedGraviaSubsystemBootstrap extends GraviaSubsystemBootstrap {

        public List<ServiceController<?>> getSubsystemServices(OperationContext context) {
            List<ServiceController<?>> controllers = new ArrayList<ServiceController<?>>();
            controllers.add(getRuntimeService(context));
            controllers.add(getSystemContextService(context));
            return controllers;
        }
    }
}
