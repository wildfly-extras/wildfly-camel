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

package org.wildfly.extension.camel.handler;

import java.security.AccessController;
import java.security.PrivilegedAction;

import org.apache.camel.CamelContext;
import org.jboss.gravia.utils.IllegalStateAssertion;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleClassLoader;
import org.wildfly.extension.camel.ContextCreateHandler;
import org.wildfly.extension.camel.service.CamelContextRegistryService;

/**
 * A {@link ContextCreateHandler} for ApplicationContextClassLoader association
 *
 * @author Thomas.Diesler@jboss.com
 * @since 13-Mar-2015
 */
public final class ModuleClassLoaderAssociationHandler implements ContextCreateHandler {

    @Override
    public void setup(CamelContext camelctx) {

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
        ModuleClassLoader moduleClassLoader = contextModule.getClassLoader();
        camelctx.setApplicationContextClassLoader(moduleClassLoader);
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
