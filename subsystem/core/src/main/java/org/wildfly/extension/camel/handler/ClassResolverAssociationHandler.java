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

import org.apache.camel.CamelContext;
import org.jboss.gravia.utils.IllegalStateAssertion;
import org.jboss.modules.ModuleClassLoader;
import org.wildfly.extension.camel.ContextCreateHandler;

/**
 * A {@link ContextCreateHandler} for ClassResolver association
 *
 * @author Thomas.Diesler@jboss.com
 * @since 13-Mar-2015
 */
public final class ClassResolverAssociationHandler implements ContextCreateHandler {

    @Override
    public void setup(CamelContext camelctx) {

        // Verify that the application context class loader is a ModuleClassLoader
        ClassLoader classLoader = camelctx.getApplicationContextClassLoader();
        IllegalStateAssertion.assertTrue(classLoader instanceof ModuleClassLoader, "Invalid class loader association: " + classLoader);

        ModuleClassLoader moduleClassLoader = (ModuleClassLoader) classLoader;
        camelctx.setClassResolver(new WildFlyClassResolver(moduleClassLoader));
    }
}
