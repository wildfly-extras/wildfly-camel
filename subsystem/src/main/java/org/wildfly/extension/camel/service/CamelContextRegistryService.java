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

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.EventObject;
import java.util.HashMap;
import java.util.Map;

import org.apache.camel.CamelContext;
import org.apache.camel.cdi.CdiCamelContext;
import org.apache.camel.management.event.CamelContextStartingEvent;
import org.apache.camel.management.event.CamelContextStoppedEvent;
import org.apache.camel.spi.Container;
import org.apache.camel.spi.ManagementStrategy;
import org.apache.camel.support.EventNotifierSupport;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.gravia.runtime.ModuleContext;
import org.jboss.gravia.runtime.Runtime;
import org.jboss.gravia.runtime.ServiceRegistration;
import org.jboss.gravia.utils.IllegalStateAssertion;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleClassLoader;
import org.jboss.msc.service.AbstractService;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.wildfly.extension.camel.CamelConstants;
import org.wildfly.extension.camel.CamelContextRegistry;
import org.wildfly.extension.camel.SpringCamelContextFactory;
import org.wildfly.extension.camel.WildFlyClassResolver;
import org.wildfly.extension.camel.parser.SubsystemState;
import org.wildfly.extension.gravia.GraviaConstants;

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
    private final InjectedValue<Runtime> injectedRuntime = new InjectedValue<Runtime>();

    private CamelContextRegistry contextRegistry;
    private ServiceRegistration<CamelContextRegistry> registration;

    public static ServiceController<CamelContextRegistry> addService(ServiceTarget serviceTarget, SubsystemState subsystemState, ServiceVerificationHandler verificationHandler) {
        CamelContextRegistryService service = new CamelContextRegistryService(subsystemState);
        ServiceBuilder<CamelContextRegistry> builder = serviceTarget.addService(CamelConstants.CAMEL_CONTEXT_REGISTRY_SERVICE_NAME, service);
        builder.addDependency(GraviaConstants.RUNTIME_SERVICE_NAME, Runtime.class, service.injectedRuntime);
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

        // Register the service with gravia
        Runtime runtime = injectedRuntime.getValue();
        ModuleContext syscontext = runtime.getModuleContext();
        registration = syscontext.registerService(CamelContextRegistry.class, contextRegistry, null);

        ClassLoader classLoader = CamelContextRegistry.class.getClassLoader();
        for (final String name : subsystemState.getContextDefinitionNames()) {
            ClassLoader tccl = Thread.currentThread().getContextClassLoader();
            try {
                Thread.currentThread().setContextClassLoader(classLoader);
                String beansXML = getBeansXML(name, subsystemState.getContextDefinition(name));
                SpringCamelContextFactory.createSpringCamelContext(beansXML.getBytes(), classLoader);
            } catch (Exception ex) {
                throw new IllegalStateException("Cannot create camel context: " + name, ex);
            } finally {
                Thread.currentThread().setContextClassLoader(tccl);
            }
        }
    }

    @Override
    public void stop(StopContext context) {
        if (registration != null) {
            registration.unregister();
        }
    }

    @Override
    public CamelContextRegistry getValue() {
        return contextRegistry;
    }

    private String getBeansXML(String name, String contextDefinition) {
        String hashReplaced = contextDefinition.replace("#{", "${");
        return SPRING_BEANS_HEADER + "<camelContext id='" + name + "' xmlns='http://camel.apache.org/schema/spring'>" + hashReplaced + "</camelContext></beans>";
    }

    static class DefaultCamelContextRegistry implements CamelContextRegistry, Container {

        final Map<CamelContext, ContextRegistration> contexts = new HashMap<>();

        class ContextRegistration {
            final Module module;
            final ClassLoader tcclOnStart;

            ContextRegistration(Module module) {
                this.tcclOnStart = Thread.currentThread().getContextClassLoader();
                this.module = module;
            }
        }

        DefaultCamelContextRegistry() {
            // Set the Camel Container singleton
            Container.Instance.set(this);
        }

        @Override
        public CamelContext getContext(String name) {
            CamelContext result = null;
            synchronized (contexts) {
                for (CamelContext camelctx : contexts.keySet()) {
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

            // Ignore CDI camel contexts
            if (camelctx instanceof CdiCamelContext)
                return;

            Module contextModule = null;

            // Case #1: The context has already been initialized
            ClassLoader applicationClassLoader = camelctx.getApplicationContextClassLoader();
            if (applicationClassLoader instanceof ModuleClassLoader) {
                contextModule = ((ModuleClassLoader) applicationClassLoader).getModule();
            }

            // Case #2: The context is a system context
            if (contextModule == null) {
                ClassLoader thiscl = CamelContextRegistryService.class.getClassLoader();
                ClassLoader tccl = Thread.currentThread().getContextClassLoader();
                if (tccl == thiscl) {
                    contextModule = ((ModuleClassLoader) thiscl).getModule();
                }
            }

            // Case #3: The context is created as part of a deployment
            if (contextModule == null) {
                ClassLoader tccl = Thread.currentThread().getContextClassLoader();
                if (tccl instanceof ModuleClassLoader) {
                    Module tcm = ((ModuleClassLoader) tccl).getModule();
                    if (tcm.getIdentifier().getName().startsWith("deployment.")) {
                        contextModule = tcm;
                    }
                }
            }

            // Case #4: The context is created through user API
            if (contextModule == null) {
                Class<?> callingClass = CallerContext.getCallingClass();
                contextModule = ((ModuleClassLoader) callingClass.getClassLoader()).getModule();
            }

            IllegalStateAssertion.assertNotNull(contextModule, "Cannot obtain module for: " + camelctx);
            final Module[] moduleHolder = new Module[] { contextModule };

            ManagementStrategy mgmtStrategy = camelctx.getManagementStrategy();
            mgmtStrategy.addEventNotifier(new EventNotifierSupport() {

                public void notify(EventObject event) throws Exception {

                    // Starting
                    if (event instanceof CamelContextStartingEvent) {
                        CamelContextStartingEvent camelevt = (CamelContextStartingEvent) event;
                        CamelContext camelctx = camelevt.getContext();
                        addContext(camelctx, moduleHolder[0]);
                        LOGGER.info("Camel context starting: {}", camelctx);
                    }

                    // Stopped
                    else if (event instanceof CamelContextStoppedEvent) {
                        CamelContextStoppedEvent camelevt = (CamelContextStoppedEvent) event;
                        CamelContext camelctx = camelevt.getContext();
                        removeContext(camelctx);
                        LOGGER.info("Camel context stopped: {}", camelctx);
                    }
                }

                public boolean isEnabled(EventObject event) {
                    return true;
                }
            });

            // Initial context setup and registration
            ModuleClassLoader contextClassLoader = moduleHolder[0].getClassLoader();
            camelctx.setClassResolver(new WildFlyClassResolver(contextClassLoader));
            camelctx.setApplicationContextClassLoader(contextClassLoader);
            addContext(camelctx, null);
        }

        private ContextRegistration addContext(CamelContext camelctx, Module module) {
            synchronized (contexts) {
                ContextRegistration ctxreg = new ContextRegistration(module);
                contexts.put(camelctx, ctxreg);
                return ctxreg;
            }
        }

        private void removeContext(CamelContext camelctx) {
            synchronized (contexts) {
                contexts.remove(camelctx);
            }
        }
    }

    static final class CallerContext {

        // Hide ctor
        private CallerContext() {
        }

        private static Hack hack = AccessController.doPrivileged(new PrivilegedAction<Hack>() {
            public Hack run() {
                return new Hack();
            }
        });

        static Class<?> getCallingClass() {
            Class<?> stack[] = hack.getClassContext();
            int i = 3;
            while (stack[i] == stack[2]) {
                if (++i >= stack.length)
                    return null;
            }
            Class<?> result = stack[i];
            while (ignoreCaller(result.getName())) {
                result = stack[++i];
            }
            return result;
        }

        private static boolean ignoreCaller(String caller) {
            boolean result = caller.startsWith("org.wildfly.extension.camel");
            result |= caller.startsWith("org.springframework");
            result |= caller.startsWith("org.apache.camel");
            return result;
        }

        private static final class Hack extends SecurityManager {
            protected Class<?>[] getClassContext() {
                return super.getClassContext();
            }
        }
    }
}
