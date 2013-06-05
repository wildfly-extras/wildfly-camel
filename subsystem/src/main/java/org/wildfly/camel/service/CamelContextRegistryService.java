/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.wildfly.camel.service;

import static org.wildfly.camel.CamelLogger.LOGGER;
import static org.wildfly.camel.CamelMessages.MESSAGES;

import org.apache.camel.CamelContext;
import org.jboss.as.controller.ServiceVerificationHandler;
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
 * @author Thomas.Diesler@jboss.com
 * @since 19-Apr-2013
 */
public class CamelContextRegistryService extends AbstractService<CamelContextRegistry> {

    private static final String SPRING_BEANS_HEADER = "<beans " +
            "xmlns='http://www.springframework.org/schema/beans' " +
            "xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance' " +
            "xsi:schemaLocation='http://www.springframework.org/schema/beans http://www.springframework.org/schema/beans/spring-beans.xsd " +
            "http://camel.apache.org/schema/spring http://camel.apache.org/schema/spring/camel-spring.xsd'>";

    private final SubsystemState subsystemState;
    private CamelContextRegistry contextRegistry;

    public static ServiceController<CamelContextRegistry> addService(ServiceTarget serviceTarget, SubsystemState subsystemState, ServiceVerificationHandler verificationHandler) {
        CamelContextRegistryService service = new CamelContextRegistryService(subsystemState);
        ServiceBuilder<CamelContextRegistry> builder = serviceTarget.addService(CamelConstants.CAMEL_CONTEXT_REGISTRY_NAME, service);
        builder.addListener(verificationHandler);
        return builder.install();
    }

    // Hide ctor
    private CamelContextRegistryService(SubsystemState subsystemState) {
        this.subsystemState = subsystemState;
    }

    @Override
    public void start(StartContext startContext) throws StartException {
        ServiceContainer serviceContainer = startContext.getController().getServiceContainer();
        final ServiceTarget serviceTarget = startContext.getChildTarget();
        contextRegistry = new DefaultCamelContextRegistry(serviceContainer, serviceTarget);
        for (final String name : subsystemState.getContextDefinitionNames()) {
            LOGGER.infoRegisterCamelContext(name);
            CamelContext camelContext;
            try {
                ClassLoader classLoader = CamelContextRegistry.class.getClassLoader();
                String beansXML = getBeansXML(name, subsystemState.getContextDefinition(name));
                camelContext = SpringCamelContextFactory.createSpringCamelContext(beansXML.getBytes(), classLoader);
            } catch (Exception ex) {
                throw MESSAGES.cannotCreateCamelContext(ex, name);
            }
            contextRegistry.registerCamelContext(camelContext);
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
            ServiceName serviceName = getInternalServiceName(name);
            ServiceController<?> controller = serviceContainer.getService(serviceName);
            return (CamelContext) (controller != null ? controller.getValue() : null);
        }

        @Override
        public CamelContextRegistration registerCamelContext(final CamelContext camelContext) {
            String name = camelContext.getName();
            if (getCamelContext(name) != null)
                throw MESSAGES.camelContextAlreadyRegistered(name);

            LOGGER.infoRegisterCamelContext(name);

            // Install the {@link CamelContext} as {@link Service}
            ValueService<CamelContext> service = new ValueService<CamelContext>(new ImmediateValue<CamelContext>(camelContext));
            ServiceBuilder<CamelContext> builder = serviceTarget.addService(getInternalServiceName(name), service);
            final ServiceController<CamelContext> controller = builder.install();

            return new CamelContextRegistration() {

                @Override
                public CamelContext getCamelContext() {
                    return camelContext;
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
