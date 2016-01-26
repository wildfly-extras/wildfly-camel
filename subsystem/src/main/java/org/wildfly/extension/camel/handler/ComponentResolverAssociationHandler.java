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
import org.apache.camel.Component;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.spi.ComponentResolver;
import org.wildfly.extension.camel.ContextCreateHandler;
import org.wildfly.extension.camel.component.WildFlyUndertowComponent;
import org.wildfly.extension.camel.parser.SubsystemRuntimeState;

/**
 * A {@link ContextCreateHandler} that sets the {@link ComponentResolver}
 *
 * @author Thomas.Diesler@jboss.com
 * @since 30-Jul-2015
 */
public final class ComponentResolverAssociationHandler implements ContextCreateHandler {

    private final SubsystemRuntimeState runtimeState;

    public ComponentResolverAssociationHandler(SubsystemRuntimeState runtimeState) {
        this.runtimeState = runtimeState;
    }

    @Override
    public void setup(final CamelContext camelctx) {
        if (camelctx instanceof DefaultCamelContext) {
            DefaultCamelContext defaultctx = (DefaultCamelContext) camelctx;
            ComponentResolver delegate = defaultctx.getComponentResolver();
            defaultctx.setComponentResolver(new WildFlyComponentResolver(delegate));
        }
    }

    class WildFlyComponentResolver implements ComponentResolver {

        final ComponentResolver delegate;

        WildFlyComponentResolver(ComponentResolver delegate) {
            this.delegate = delegate;
        }

        @Override
        public Component resolveComponent(String name, CamelContext context) throws Exception {
            if ("undertow".equals(name)) {
                return new WildFlyUndertowComponent(runtimeState);
            } else {
                return delegate.resolveComponent(name, context);
            }
        }
    }
}
