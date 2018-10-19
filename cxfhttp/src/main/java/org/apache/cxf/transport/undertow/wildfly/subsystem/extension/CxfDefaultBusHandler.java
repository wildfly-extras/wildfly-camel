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

import java.util.Collection;

import org.apache.camel.CamelContext;
import org.apache.camel.Endpoint;
import org.apache.camel.Route;
import org.apache.camel.component.cxf.CxfComponent;
import org.apache.camel.component.cxf.jaxrs.CxfRsEndpoint;
import org.apache.camel.spi.LifecycleStrategy;
import org.apache.camel.support.LifecycleStrategySupport;
import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.transport.http.HttpDestinationFactory;
import org.apache.cxf.transport.undertow.UndertowDestinationFactory;
import org.wildfly.extension.camel.ContextCreateHandler;

/**
 * A {@link ContextCreateHandler} taking care for each newly created {@link CxfRsEndpoint} to have its {@link Bus} set
 * to an instance that was created using the class loader of the Camel CXF component. This is to prevent the usage of
 * the {@link Bus} returned by {@link BusFactory#getThreadDefaultBus()} which may be set (improperly for us) by JBoss WS
 * subsystem.
 *
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 */
public final class CxfDefaultBusHandler implements ContextCreateHandler {

    private final LifecycleStrategy endpointStrategy = new CxfDefaultBusEndpointStrategy();

    @Override
    public void setup(CamelContext camelctx) {
        camelctx.addLifecycleStrategy(endpointStrategy);
    }

    public CxfDefaultBusHandler() {
        super();
    }

    static class CxfDefaultBusEndpointStrategy extends LifecycleStrategySupport {
        private final Bus bus;

        public CxfDefaultBusEndpointStrategy() {
            super();
            final Bus bus;
            final ClassLoader origClassLoader = SecurityActions.getContextClassLoader();
            try {
                SecurityActions.setContextClassLoader(CxfComponent.class.getClassLoader());
                bus = BusFactory.getDefaultBus(true);
            } finally {
                SecurityActions.setContextClassLoader(origClassLoader);
            }
            /* Check if the default bus is the one we want */
            final HttpDestinationFactory httpDestinationFactory = bus.getExtension(HttpDestinationFactory.class);
            if (httpDestinationFactory instanceof UndertowDestinationFactory) {
                this.bus = bus;
            } else {
                throw new IllegalStateException(String.format("Expected %s returning %s, found %s", Bus.class.getName(),
                        UndertowDestinationFactory.class.getName(), httpDestinationFactory.getClass().getName()));
            }
        }

        public void onRoutesAdd(Collection<Route> routes) {
            /*
             * LifecycleStrategySupport.onEndpointAdd() is not called for CxfRsEndpoints created via direct constructor
             * invocation. Therefore we check if the buses on CxfRsEndpoints are correct.
             */
            for (Route route : routes) {
                final Endpoint endpoint = route.getEndpoint();
                if (endpoint instanceof CxfRsEndpoint) {
                    final CxfRsEndpoint rsEnspoint = (CxfRsEndpoint) endpoint;
                    final Bus endpointBus = rsEnspoint.getBus();
                    if (endpointBus == null || (endpointBus != bus && !(endpointBus
                            .getExtension(HttpDestinationFactory.class) instanceof UndertowDestinationFactory))) {
                        /* Not a correct bus instance */
                        throw new IllegalStateException("A " + CxfRsEndpoint.class.getName() + " used in route " + route
                                + " either does not have " + Bus.class.getName() + " set or the "
                                + Bus.class.getSimpleName() + " set was not created using correct context class loader."
                                + " This is known to happen for " + CxfRsEndpoint.class.getName()
                                + " instances created by direct constructor invocation."
                                + " Consider using camelContext.getEndpoint(\"cxfrs:http[s]://my-host/my-endpoint\", CxfRsEndpoint.class) instead"
                                + " or add your manually created endpoint to the context management manually using CamelContext.addEndpoint(String uri, Endpoint endpoint)");
                    }
                }
            }
        }

        @Override
        public void onEndpointAdd(Endpoint endpoint) {
            if (endpoint instanceof CxfRsEndpoint) {
                final CxfRsEndpoint rsEndPoint = (CxfRsEndpoint) endpoint;
                /* We set the bus only if the user has not provided one himself */
                if (rsEndPoint.getBus() == null) {
                    rsEndPoint.setBus(bus);
                }
            }
        }

    }
}
