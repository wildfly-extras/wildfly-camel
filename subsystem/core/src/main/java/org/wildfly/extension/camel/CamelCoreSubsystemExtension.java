/*
 * #%L
 * Wildfly Camel :: Subsystem
 * %%
 * Copyright (C) 2013 - 2015 RedHat
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
package org.wildfly.extension.camel;

import java.util.HashMap;
import java.util.Map;

import org.apache.camel.CamelContext;
import org.jboss.as.server.DeploymentProcessorTarget;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.Phase;
import org.jboss.as.server.deployment.jbossallxml.JBossAllXmlParserRegisteringProcessor;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceController.Mode;
import org.jboss.msc.service.ServiceTarget;
import org.wildfly.extension.camel.deployment.CamelDeploymentSettings;
import org.wildfly.extension.camel.deployment.CamelIntegrationParser;
import org.wildfly.extension.camel.handler.NamingContextAssociationHandler;
import org.wildfly.extension.camel.parser.CamelExtension;
import org.wildfly.extension.camel.parser.CamelSubsystemAdd;
import org.wildfly.extension.camel.parser.SubsystemState;
import org.wildfly.extension.camel.service.CamelContextBindingService;
import org.wildfly.extension.camel.service.CamelContextFactoryBindingService;
import org.wildfly.extension.camel.service.CamelContextRegistryBindingService;

public class CamelCoreSubsystemExtension implements CamelSubsytemExtension {

    private final Map<CamelContext, ServiceController<?>> contexts = new HashMap<>();

    @Override
    public void addExtensionServices(ServiceTarget serviceTarget, SubsystemState subsystemState) {
        CamelContextFactoryBindingService.addService(serviceTarget);
        CamelContextRegistryBindingService.addService(serviceTarget);
    }

    @Override
    public void addDeploymentProcessor(DeploymentProcessorTarget processorTarget, SubsystemState subsystemState) {
        DeploymentUnitProcessor parser = new JBossAllXmlParserRegisteringProcessor<CamelDeploymentSettings>(CamelIntegrationParser.ROOT_ELEMENT, CamelDeploymentSettings.ATTACHMENT_KEY, new CamelIntegrationParser());
        processorTarget.addDeploymentProcessor(CamelExtension.SUBSYSTEM_NAME, Phase.STRUCTURE, CamelSubsystemAdd.STRUCTURE_REGISTER_CAMEL_INTEGRATION, parser);
    }

    @Override
    public ContextCreateHandler getContextCreateHandler(ServiceContainer serviceContainer, ServiceTarget serviceTarget, SubsystemState subsystemState) {
        return new NamingContextAssociationHandler(serviceContainer, serviceTarget);
    }

    @Override
    public void addCamelContext(ServiceTarget serviceTarget, CamelContext camelctx) {
        ServiceController<?> controller = CamelContextBindingService.addService(serviceTarget, camelctx);
        synchronized (contexts) {
            contexts.put(camelctx, controller);
        }
    }

    @Override
    public void removeCamelContext(CamelContext camelctx) {
        ServiceController<?> controller;
        synchronized (contexts) {
            controller = contexts.get(camelctx);
            contexts.remove(camelctx);
        }
        if (controller != null) {
            controller.setMode(Mode.REMOVE);
        }
    }
}
