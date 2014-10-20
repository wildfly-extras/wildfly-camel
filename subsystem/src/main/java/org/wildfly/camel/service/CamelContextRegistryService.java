/*
 * #%L
 * Wildfly Camel Subsystem
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


package org.wildfly.camel.service;

import static org.wildfly.camel.CamelLogger.LOGGER;

import org.apache.camel.CamelContext;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.gravia.utils.IllegalStateAssertion;
import org.jboss.msc.service.AbstractService;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceController.Mode;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.ValueService;
import org.jboss.msc.value.ImmediateValue;
import org.wildfly.camel.CamelConstants;
import org.wildfly.camel.CamelContextRegistry;
import org.wildfly.camel.SpringCamelContextFactory;
import org.wildfly.camel.parser.SubsystemState;

/**
 * The {@link CamelContextRegistry} service
 *
 * Ths implementation creates a jboss-msc {@link org.jboss.msc.service.Service}.
 *
 * JBoss services can create a dependency on the {@link CamelContext} service like this
 *
 * <code>
        ServiceName serviceName = CamelConstants.CAMEL_CONTEXT_BASE_NAME.append(contextName);
        builder.addDependency(serviceName, CamelContext.class, service.injectedCamelContext);
 * </code>
 *
 * @author Thomas.Diesler@jboss.com
 * @since 19-Apr-2013
 */
public class CamelContextRegistryService extends AbstractService<CamelContextRegistry> {

    private static final String SPRING_BEANS_HEADER = "<beans " + "xmlns='http://www.springframework.org/schema/beans' "
            + "xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance' "
            + "xsi:schemaLocation='http://www.springframework.org/schema/beans http://www.springframework.org/schema/beans/spring-beans.xsd "
            + "http://camel.apache.org/schema/spring http://camel.apache.org/schema/spring/camel-spring.xsd'>";

    private final SubsystemState subsystemState;
    private CamelContextRegistry contextRegistry;

    public static ServiceController<CamelContextRegistry> addService(ServiceTarget serviceTarget, SubsystemState subsystemState, ServiceVerificationHandler verificationHandler) {
        CamelContextRegistryService service = new CamelContextRegistryService(subsystemState);
        ServiceBuilder<CamelContextRegistry> builder = serviceTarget.addService(CamelConstants.CAMEL_CONTEXT_REGISTRY_SERVICE_NAME, service);
        builder.addListener(verificationHandler);
        return builder.install();
    }

    // Hide ctor
    private CamelContextRegistryService(SubsystemState subsystemState) {
        this.subsystemState = subsystemState;
    }

    @Override
    public void start(StartContext startContext) throws StartException {
        final ServiceContainer serviceContainer = startContext.getController().getServiceContainer();
        final ServiceTarget serviceTarget = startContext.getChildTarget();
        contextRegistry = new DefaultCamelContextRegistry(serviceContainer, serviceTarget);
        for (final String name : subsystemState.getContextDefinitionNames()) {
            CamelContext camelctx;
            try {
                ClassLoader classLoader = CamelContextRegistry.class.getClassLoader();
                String beansXML = getBeansXML(name, subsystemState.getContextDefinition(name));
                camelctx = SpringCamelContextFactory.createSpringCamelContext(beansXML.getBytes(), classLoader);
            } catch (Exception ex) {
                throw new IllegalStateException("Cannot create camel context: " + name, ex); 
            }
            contextRegistry.registerCamelContext(camelctx);
        }
    }

    @Override
    public CamelContextRegistry getValue() {
        return contextRegistry;
    }

    private String getBeansXML(String name, String contextDefinition) {
        // [TODO] allow expressions in system context definition
        String hashReplaced = contextDefinition.replace("#{", "${");
        return SPRING_BEANS_HEADER + "<camelContext id='" + name + "' xmlns='http://camel.apache.org/schema/spring'>" + hashReplaced + "</camelContext></beans>";
    }

    class DefaultCamelContextRegistry implements CamelContextRegistry {

        private final ServiceContainer serviceContainer;
        private final ServiceTarget serviceTarget;

        DefaultCamelContextRegistry(ServiceContainer serviceContainer, ServiceTarget serviceTarget) {
            this.serviceContainer = serviceContainer;
            this.serviceTarget = serviceTarget;
        }

        @Override
        public CamelContext getCamelContext(String name) {
            ServiceController<?> controller = serviceContainer.getService(getInternalServiceName(name));
            return controller != null ? (CamelContext) controller.getValue() : null;
        }

        @Override
        public CamelContextRegistration registerCamelContext(final CamelContext camelctx) {
            String name = camelctx.getName();
            IllegalStateAssertion.assertNull(getCamelContext(name), "Camel context with that name already registered: " + name);

            LOGGER.info("Register camel context: {}", name);

            // Install the {@link CamelContext} as {@link Service}
            ValueService<CamelContext> service = new ValueService<CamelContext>(new ImmediateValue<CamelContext>(camelctx));
            ServiceBuilder<CamelContext> builder = serviceTarget.addService(getInternalServiceName(name), service);
            builder.addDependency(CamelContextRegistryBindingService.getBinderServiceName());
            final ServiceController<CamelContext> controller = builder.install();

            return new CamelContextRegistration() {

                @Override
                public CamelContext getCamelContext() {
                    return camelctx;
                }

                @Override
                public void unregister() {
                    controller.setMode(Mode.REMOVE);
                }
            };
        }

        private ServiceName getInternalServiceName(String name) {
            return CamelConstants.CAMEL_CONTEXT_BASE_NAME.append(name);
        }
    }
}
