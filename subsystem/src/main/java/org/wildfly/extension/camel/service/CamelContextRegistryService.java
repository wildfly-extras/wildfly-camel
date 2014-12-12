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


package org.wildfly.extension.camel.service;

import static org.wildfly.extension.camel.CamelLogger.LOGGER;

import java.util.EventObject;
import java.util.HashSet;
import java.util.Set;

import org.apache.camel.CamelContext;
import org.apache.camel.cdi.CdiCamelContext;
import org.apache.camel.management.event.CamelContextStartedEvent;
import org.apache.camel.management.event.CamelContextStoppedEvent;
import org.apache.camel.spi.Container;
import org.apache.camel.spi.ManagementStrategy;
import org.apache.camel.support.EventNotifierSupport;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.msc.service.AbstractService;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.wildfly.extension.camel.CamelConstants;
import org.wildfly.extension.camel.CamelContextRegistry;
import org.wildfly.extension.camel.SpringCamelContextFactory;
import org.wildfly.extension.camel.parser.SubsystemState;

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
        contextRegistry = new DefaultCamelContextRegistry();
        for (final String name : subsystemState.getContextDefinitionNames()) {
            try {
                ClassLoader classLoader = CamelContextRegistry.class.getClassLoader();
                String beansXML = getBeansXML(name, subsystemState.getContextDefinition(name));
                SpringCamelContextFactory.createSpringCamelContext(beansXML.getBytes(), classLoader);
            } catch (Exception ex) {
                throw new IllegalStateException("Cannot create camel context: " + name, ex); 
            }
        }
    }

    @Override
    public CamelContextRegistry getValue() {
        return contextRegistry;
    }

    private String getBeansXML(String name, String contextDefinition) {
        // [FIXME #2] support expressions in subsystem configuration
        String hashReplaced = contextDefinition.replace("#{", "${");
        return SPRING_BEANS_HEADER + "<camelContext id='" + name + "' xmlns='http://camel.apache.org/schema/spring'>" + hashReplaced + "</camelContext></beans>";
    }

    class DefaultCamelContextRegistry implements CamelContextRegistry, Container {

        final Set<CamelContext> contexts = new HashSet<>();
            
        DefaultCamelContextRegistry() {
            // Set the Camel Container singleton
            Container.Instance.set(this);
        }

        @Override
        public CamelContext getContext(String name) {
            CamelContext result = null;
            synchronized (contexts) {
                for (CamelContext camelctx : contexts) {
                    if (camelctx.getName().equals(name)) {
                        result = camelctx;
                        break;
                    }
                }
            }
            return result;
        }

        @Override
        public void manage(CamelContext camelctx) {
            registerCamelContext(camelctx);
        }
        
        private void registerCamelContext(CamelContext camelctx) {
            
            // Ignore CDI camel contexts
            if (camelctx instanceof CdiCamelContext) 
                return;
            
            ManagementStrategy mgmtStrategy = camelctx.getManagementStrategy();
            mgmtStrategy.addEventNotifier(new EventNotifierSupport() {
                
                public void notify(EventObject event) throws Exception {
                    if (event instanceof CamelContextStartedEvent) {
                        CamelContextStartedEvent camelevt = (CamelContextStartedEvent) event;
                        CamelContext camelctx = camelevt.getContext();
                        LOGGER.info("Camel context started: {}", camelctx);
                        addContext(camelctx);
                    } else if (event instanceof CamelContextStoppedEvent) {
                        CamelContextStoppedEvent camelevt = (CamelContextStoppedEvent) event;
                        CamelContext camelctx = camelevt.getContext();
                        LOGGER.info("Camel context stopped: {}", camelctx);
                        removeContext(camelctx);
                    }
                }
                
                public boolean isEnabled(EventObject event) {
                    return event instanceof CamelContextStartedEvent || event instanceof CamelContextStoppedEvent;
                }
            });
            
            addContext(camelctx);
        }

        private void addContext(CamelContext camelctx) {
            synchronized (contexts) {
                contexts.add(camelctx);
            }
        }

        private void removeContext(CamelContext camelctx) {
            synchronized (contexts) {
                contexts.remove(camelctx);
            }
        }
    }
}
