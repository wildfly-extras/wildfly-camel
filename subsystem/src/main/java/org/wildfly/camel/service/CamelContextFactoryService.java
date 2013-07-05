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

import java.util.Hashtable;

import javax.naming.Binding;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.Name;
import javax.naming.NameClassPair;
import javax.naming.NameParser;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.OperationNotSupportedException;

import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.naming.ManagedReferenceFactory;
import org.jboss.as.naming.ManagedReferenceInjector;
import org.jboss.as.naming.ServiceBasedNamingStore;
import org.jboss.as.naming.deployment.ContextNames;
import org.jboss.as.naming.service.BinderService;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.AbstractService;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceController.Mode;
import org.jboss.msc.service.ServiceRegistry;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.wildfly.camel.CamelComponentRegistry;
import org.wildfly.camel.CamelConstants;
import org.wildfly.camel.CamelContextFactory;
import org.wildfly.camel.WildflyCamelContext;

/**
 * The {@link CamelContextFactory} service
 *
 * @author Thomas.Diesler@jboss.com
 * @since 05-Jun-2013
 */
public class CamelContextFactoryService extends AbstractService<CamelContextFactory> {

    private final InjectedValue<CamelComponentRegistry> injectedComponentRegistry = new InjectedValue<CamelComponentRegistry>();
    private CamelContextFactory contextFactory;

    public static ServiceController<CamelContextFactory> addService(ServiceTarget serviceTarget, ServiceVerificationHandler verificationHandler) {
        CamelContextFactoryService service = new CamelContextFactoryService();
        ServiceBuilder<CamelContextFactory> builder = serviceTarget.addService(CamelConstants.CAMEL_CONTEXT_FACTORY_NAME, service);
        builder.addDependency(CamelConstants.CAMEL_COMPONENT_REGISTRY_NAME, CamelComponentRegistry.class, service.injectedComponentRegistry);
        builder.addListener(verificationHandler);
        return builder.install();
    }

    // Hide ctor
    private CamelContextFactoryService() {
    }

    @Override
    public void start(StartContext startContext) throws StartException {
        CamelComponentRegistry componentRegistry = injectedComponentRegistry.getValue();
        ServiceContainer serviceContainer = startContext.getController().getServiceContainer();
        contextFactory = new DefaultCamelContextFactory(serviceContainer, startContext.getChildTarget(), componentRegistry);
    }

    @Override
    public CamelContextFactory getValue() {
        return contextFactory;
    }

    static final class DefaultCamelContextFactory implements CamelContextFactory {

        private final ServiceRegistry serviceRegistry;
        private final ServiceTarget serviceTarget;
        private final CamelComponentRegistry componentRegistry;

        DefaultCamelContextFactory(ServiceRegistry serviceRegistry, ServiceTarget serviceTarget, CamelComponentRegistry componentRegistry) {
            this.serviceRegistry = serviceRegistry;
            this.serviceTarget = serviceTarget;
            this.componentRegistry = componentRegistry;
        }

        @Override
        public WildflyCamelContext createWilflyCamelContext() throws Exception {
            return createWildflyCamelContext(null);
        }

        @Override
        public WildflyCamelContext createWildflyCamelContext(ClassLoader classsLoader) throws Exception {
            return setup(new WildflyCamelContext(componentRegistry));
        }

        private WildflyCamelContext setup(WildflyCamelContext context) {
            try {
                context.setNamingContext(new NamingContext(serviceRegistry, serviceTarget));
            } catch (NamingException ex) {
                throw MESSAGES.cannotInitializeNamingContext(ex);
            }
            return context;
        }
    }

    static class NamingContext implements Context {

        private final ServiceRegistry serviceRegistry;
        private final ServiceTarget serviceTarget;
        private final Context context;

        private NamingContext(ServiceRegistry serviceRegistry, ServiceTarget serviceTarget) throws NamingException {
            this.serviceRegistry = serviceRegistry;
            this.serviceTarget = serviceTarget;
            this.context = new InitialContext();
        }

        @Override
        public void bind(Name name, Object obj) throws NamingException {
            addBinderService(name.toString(), obj);
        }

        @Override
        public void bind(String name, Object obj) throws NamingException {
            addBinderService(name, obj);
        }

        private ServiceController<?> addBinderService(String name, Object obj) {
            final ContextNames.BindInfo bindInfo = ContextNames.bindInfoFor(name);
            BinderService binderService = new BinderService(bindInfo.getBindName()) {
                @Override
                public synchronized void start(StartContext context) throws StartException {
                    super.start(context);
                    LOGGER.infoBoundCamelNamingObject(bindInfo.getAbsoluteJndiName());
                }

                @Override
                public synchronized void stop(StopContext context) {
                    LOGGER.infoUnbindCamelNamingObject(bindInfo.getAbsoluteJndiName());
                    super.stop(context);
                }
            };
            Injector<ManagedReferenceFactory> injector = binderService.getManagedObjectInjector();
            new ManagedReferenceInjector<Object>(injector).inject(obj);
            ServiceBuilder<?> builder = serviceTarget.addService(bindInfo.getBinderServiceName(), binderService);
            builder.addDependency(bindInfo.getParentContextServiceName(), ServiceBasedNamingStore.class, binderService.getNamingStoreInjector());
            return builder.install();
        }

        @Override
        public void unbind(Name name) throws NamingException {
            removeBinderService(name.toString());
        }

        @Override
        public void unbind(String name) throws NamingException {
            removeBinderService(name);
        }

        private ServiceController<?> removeBinderService(String name) {
            final ContextNames.BindInfo bindInfo = ContextNames.bindInfoFor(name);
            ServiceController<?> controller = serviceRegistry.getService(bindInfo.getBinderServiceName());
            if (controller != null) {
                controller.setMode(Mode.REMOVE);
            }
            return controller;
        }

        @Override
        public Name composeName(Name name, Name prefix) throws NamingException {
            return context.composeName(name, prefix);
        }

        @Override
        public String composeName(String name, String prefix) throws NamingException {
            return context.composeName(name, prefix);
        }

        @Override
        public Hashtable<?, ?> getEnvironment() throws NamingException {
            return context.getEnvironment();
        }

        @Override
        public String getNameInNamespace() throws NamingException {
            return context.getNameInNamespace();
        }

        @Override
        public NameParser getNameParser(Name name) throws NamingException {
            return context.getNameParser(name);
        }

        @Override
        public NameParser getNameParser(String name) throws NamingException {
            return context.getNameParser(name);
        }

        @Override
        public NamingEnumeration<NameClassPair> list(Name name) throws NamingException {
            return context.list(name);
        }

        @Override
        public NamingEnumeration<NameClassPair> list(String name) throws NamingException {
            return context.list(name);
        }

        @Override
        public NamingEnumeration<Binding> listBindings(Name name) throws NamingException {
            return context.listBindings(name);
        }

        @Override
        public NamingEnumeration<Binding> listBindings(String name) throws NamingException {
            return context.listBindings(name);
        }

        @Override
        public Object lookup(Name name) throws NamingException {
            return context.lookup(name);
        }

        @Override
        public Object lookup(String name) throws NamingException {
            return context.lookup(name);
        }

        @Override
        public Object lookupLink(Name name) throws NamingException {
            return context.lookupLink(name);
        }

        @Override
        public Object lookupLink(String name) throws NamingException {
            return context.lookupLink(name);
        }

        // Not supported opertations

        @Override
        public Object addToEnvironment(String propName, Object propVal) throws NamingException {
            throw new OperationNotSupportedException();
        }

        @Override
        public Object removeFromEnvironment(String propName) throws NamingException {
            throw new OperationNotSupportedException();
        }

        @Override
        public void close() throws NamingException {
            throw new OperationNotSupportedException();
        }

        @Override
        public Context createSubcontext(Name name) throws NamingException {
            throw new OperationNotSupportedException();
        }

        @Override
        public Context createSubcontext(String name) throws NamingException {
            throw new OperationNotSupportedException();
        }

        @Override
        public void destroySubcontext(Name name) throws NamingException {
            throw new OperationNotSupportedException();
        }

        @Override
        public void destroySubcontext(String name) throws NamingException {
            throw new OperationNotSupportedException();
        }

        @Override
        public void rebind(Name name, Object obj) throws NamingException {
            throw new OperationNotSupportedException();
        }

        @Override
        public void rebind(String name, Object obj) throws NamingException {
            throw new OperationNotSupportedException();
        }

        @Override
        public void rename(Name oldName, Name newName) throws NamingException {
            throw new OperationNotSupportedException();
        }

        @Override
        public void rename(String oldName, String newName) throws NamingException {
            throw new OperationNotSupportedException();
        }
    }
}
