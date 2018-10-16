/*
 * #%L
 * Wildfly Camel :: Subsystem
 * %%
 * Copyright (C) 2013 - 2018 RedHat
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


package org.apache.cxf.transport.undertow.wildfly.subsystem.extension;

import static java.lang.System.getSecurityManager;
import static java.security.AccessController.doPrivileged;

import org.wildfly.security.manager.action.GetContextClassLoaderAction;
import org.wildfly.security.manager.action.SetContextClassLoaderAction;

/**
 * Privileged actions used by this package.
 *
 * @author Thomas.Diesler@jboss.com
 */
class SecurityActions {

    private SecurityActions() {
    }

    static ClassLoader getContextClassLoader() {
        return getSecurityManager() == null ? Thread.currentThread().getContextClassLoader() : doPrivileged(GetContextClassLoaderAction.getInstance());
    }

    static void setContextClassLoader(ClassLoader classLoader) {
        if (getSecurityManager() == null) {
            Thread.currentThread().setContextClassLoader(classLoader);
        } else {
            doPrivileged(new SetContextClassLoaderAction(classLoader));
        }
    }
}
