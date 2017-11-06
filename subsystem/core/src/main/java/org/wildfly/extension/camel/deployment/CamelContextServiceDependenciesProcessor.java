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

import org.apache.camel.CamelContext;
import org.jboss.as.ee.component.EEDefaultResourceJndiNames;
import org.jboss.as.ee.component.EEModuleDescription;
import org.jboss.as.ee.structure.DeploymentType;
import org.jboss.as.naming.ManagedReferenceFactory;
import org.jboss.as.naming.ServiceBasedNamingStore;
import org.jboss.as.naming.deployment.ContextNames;
import org.jboss.as.naming.deployment.JndiNamingDependencyProcessor;
import org.jboss.as.naming.service.BinderService;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.modules.Module;
import org.jboss.msc.service.DuplicateServiceException;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.wildfly.extension.camel.service.CamelContextActivationService;

/**
 * Adds dependencies on required container services before the {@link CamelContext} can be started.
 */
public class CamelContextServiceDependenciesProcessor implements DeploymentUnitProcessor {

    private static final ServiceName CAMEL_CONTEXT_ACTIVATION_SERVICE_NAME = ServiceName.of("CamelContextActivationService");

    private static final String DEFAULT_MAIL_JNDI = "java:jboss/mail/Default";
    private static final String DEFAULT_USER_TRANSACTION_JNDI = "java:jboss/UserTransaction";
    private static final String MODULE_CONTEXT_JNDI = "java:module/DefaultContextService";
    private static final String MODULE_DATASOURCE_JNDI = "java:module/DefaultDataSource";
    private static final String MODULE_JMS_CONNECTION_FACTORY_JNDI = "java:module/DefaultJMSConnectionFactory";
    private static final String MODULE_MAIL_JNDI = "java:module/DefaultMailSession";
    private static final String MODULE_MANAGED_EXECUTOR_JNDI = "java:module/DefaultManagedExecutorService";
    private static final String MODULE_MANAGED_THREAD_FACTORY_JNDI = "java:module/DefaultManagedThreadFactory";
    private static final String MODULE_SCHEDULED_EXECUTOR_JNDI = "java:module/DefaultManagedScheduledExecutorService";
    private static final String MODULE_USER_TRANSACTION_JNDI = "java:module/UserTransaction";

    @Override
    public void deploy(final DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {

        final DeploymentUnit depUnit = phaseContext.getDeploymentUnit();
        CamelDeploymentSettings depSettings = depUnit.getAttachment(CamelDeploymentSettings.ATTACHMENT_KEY);

        // Camel disabled
        if (!depSettings.isEnabled()) {
            return;
        }

        Module module = depUnit.getAttachment(Attachments.MODULE);
        String runtimeName = depUnit.getName();

        ServiceName serviceName = depUnit.getServiceName().append(CAMEL_CONTEXT_ACTIVATION_SERVICE_NAME);
        ServiceTarget serviceTarget = phaseContext.getServiceTarget();

        final CamelContextActivationService activationService = new CamelContextActivationService(depSettings.getCamelContextUrls(), module.getClassLoader(), runtimeName);
        ServiceBuilder builder = serviceTarget.addService(serviceName, activationService);

        DeploymentType depType = depUnit.getAttachment(org.jboss.as.ee.structure.Attachments.DEPLOYMENT_TYPE);
        if (depType != null) {
            // This is a known WildFly Camel JavaEE deployment, add a dependency to the JndiNamingDependencyProcessor service
            builder.addDependency(JndiNamingDependencyProcessor.serviceName(depUnit));
        } else {
            // This is some other WildFly Camel deployment type. Standalone JAR or XML file deployment, manually add required dependencies

            // Make sure we have a valid module description
            EEModuleDescription moduleDescription = depUnit.getAttachment(org.jboss.as.ee.component.Attachments.EE_MODULE_DESCRIPTION);
            if(moduleDescription == null) {
                return;
            }

            EEDefaultResourceJndiNames jndiNames = moduleDescription.getDefaultResourceJndiNames();
            installBinding(phaseContext, moduleDescription, builder, MODULE_CONTEXT_JNDI, jndiNames.getContextService());
            installBinding(phaseContext, moduleDescription, builder, MODULE_DATASOURCE_JNDI, jndiNames.getDataSource());
            installBinding(phaseContext, moduleDescription, builder, MODULE_JMS_CONNECTION_FACTORY_JNDI, jndiNames.getJmsConnectionFactory());
            installBinding(phaseContext, moduleDescription, builder, MODULE_MAIL_JNDI, DEFAULT_MAIL_JNDI);
            installBinding(phaseContext, moduleDescription, builder, MODULE_MANAGED_EXECUTOR_JNDI, jndiNames.getManagedExecutorService());
            installBinding(phaseContext, moduleDescription, builder, MODULE_MANAGED_THREAD_FACTORY_JNDI, jndiNames.getManagedThreadFactory());
            installBinding(phaseContext, moduleDescription, builder, MODULE_SCHEDULED_EXECUTOR_JNDI, jndiNames.getManagedScheduledExecutorService());
            installBinding(phaseContext, moduleDescription, builder, MODULE_USER_TRANSACTION_JNDI, DEFAULT_USER_TRANSACTION_JNDI);
        }

        builder.install();
    }

    @Override
    public void undeploy(final DeploymentUnit depUnit) {
    }

    private void installBinding(DeploymentPhaseContext phaseContext, EEModuleDescription moduleDescription, ServiceBuilder builder, String source, String target) {
        if (target != null) {
            ContextNames.BindInfo bindInfo = ContextNames.bindInfoForEnvEntry(moduleDescription.getApplicationName(),
                moduleDescription.getModuleName(), moduleDescription.getModuleName(), false, target);

            try {
                BinderService service = new BinderService(bindInfo.getBindName(), source);
                ServiceBuilder<ManagedReferenceFactory> serviceBuilder = phaseContext.getServiceTarget().addService(bindInfo.getBinderServiceName(), service);
                serviceBuilder.addDependency(bindInfo.getParentContextServiceName(), ServiceBasedNamingStore.class, service.getNamingStoreInjector());
                serviceBuilder.install();
            } catch (DuplicateServiceException e) {
                // Ignore - service already registered
            }

            builder.addDependency(bindInfo.getBinderServiceName());
        }
    }
}
