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

import java.util.Collection;

import org.apache.camel.CamelContext;
import org.apache.camel.Consumer;
import org.apache.camel.Endpoint;
import org.apache.camel.Route;
import org.apache.camel.support.LifecycleStrategySupport;
import org.wildfly.extension.camel.ContextCreateHandler;

/**
 * A {@link ContextCreateHandler} for that validates the camel context
 *
 * @author Thomas.Diesler@jboss.com
 * @since 13-Mar-2015
 */
public final class ContextValidationHandler implements ContextCreateHandler {

    @Override
    public void setup(CamelContext camelctx) {
        camelctx.addLifecycleStrategy(new LifecycleStrategySupport() {
            @Override
            public void onRoutesAdd(Collection<Route> routes) {
                for (Route route : routes) {
                    Consumer consumer = route.getConsumer();
                    if (consumer != null) {
                        Endpoint endpoint = consumer.getEndpoint();
                        String eppackage = endpoint.getClass().getPackage().getName();
                        if (eppackage.startsWith("org.apache.camel.component.cxf"))
                            throw new UnsupportedOperationException("CXF consumer endpoint not supported");
                        else if (eppackage.startsWith("org.apache.camel.component.restlet"))
                            throw new UnsupportedOperationException("Restlet consumer endpoint not supported");
                    }
                }
            }
        });
    }
}
