/*
 * #%L
 * Wildfly Camel :: Testsuite
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

package org.wildfly.camel.test.common;

import org.jboss.modules.Module;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.modules.ModuleLoadException;
import org.jboss.modules.ModuleLoader;

/**
 * Collection of Environment utilities
 *
 * @author Thomas.Diesler@jboss.com
 * @since 22-May-2015
 */
public final class EnvironmentUtils {

    // hide ctor
    private EnvironmentUtils() {
    }

    public static boolean switchyardSupport() {
        try {
            ModuleLoader moduleLoader = Module.getCallerModuleLoader();
            moduleLoader.loadModule(ModuleIdentifier.create("org.switchyard.runtime"));
            return true;
        } catch (ModuleLoadException ex) {
            return false;
        }
    }
}
