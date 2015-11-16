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

import javax.naming.InitialContext;

import org.apache.camel.CamelContext;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.impl.JndiRegistry;
import org.apache.camel.spring.SpringCamelContext;
import org.wildfly.extension.camel.ContextCreateHandler;

/**
 * A {@link ContextCreateHandler} that sets the {@link registry}
 *
 * @author Thomas.Diesler@jboss.com
 * @since 16-Oct-2015
 */
public final class JndiRegistryAssociationHandler implements ContextCreateHandler {

    private final InitialContext context;

    public JndiRegistryAssociationHandler(InitialContext context) {
        this.context = context;
    }

    @Override
    public void setup(final CamelContext camelctx) {
        if (camelctx instanceof DefaultCamelContext && !(camelctx instanceof SpringCamelContext)) {
            DefaultCamelContext defaultctx = (DefaultCamelContext) camelctx;
            defaultctx.setRegistry(new WildFlyJndiRegistry(context));
        }
    }

    static class WildFlyJndiRegistry extends JndiRegistry {

        public WildFlyJndiRegistry(InitialContext context) {
            super(context);
        }
    }
}
