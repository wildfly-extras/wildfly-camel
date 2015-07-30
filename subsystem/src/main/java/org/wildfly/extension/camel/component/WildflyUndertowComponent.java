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
package org.wildfly.extension.camel.component;

import static org.wildfly.extension.camel.CamelLogger.LOGGER;

import java.net.URI;
import java.net.URISyntaxException;

import org.apache.camel.Consumer;
import org.apache.camel.Processor;
import org.apache.camel.component.undertow.UndertowComponent;
import org.apache.camel.component.undertow.UndertowConsumer;
import org.apache.camel.component.undertow.UndertowEndpoint;
import org.apache.camel.component.undertow.UndertowHost;
import org.jboss.gravia.runtime.ServiceLocator;

/**
 * An extension to the {@link UndertowComponent}
 *
 * @author Thomas.Diesler@jboss.com
 * @since 30-Jul-2015
 */
public class WildflyUndertowComponent extends UndertowComponent {

    @Override
    protected UndertowEndpoint createEndpointInstance(URI endpointUri, UndertowComponent component) throws URISyntaxException {
        return new WildflyUndertowEndpoint(endpointUri.toString(), component);
    }

    @Override
    public void startServer(UndertowConsumer consumer) {
        // do nothing
    }

    class WildflyUndertowEndpoint extends UndertowEndpoint {

        WildflyUndertowEndpoint(String uri, UndertowComponent component) throws URISyntaxException {
            super(uri, component);
        }

        @Override
        public Consumer createConsumer(Processor processor) throws Exception {
            return new WildflyUndertowUndertowConsumer(this, processor);
        }
    }

    class WildflyUndertowUndertowConsumer extends UndertowConsumer {

        WildflyUndertowUndertowConsumer(UndertowEndpoint endpoint, Processor processor) throws Exception {
            super(endpoint, processor);
            URI uri = new URI(endpoint.getEndpointUri());
            String host = uri.getHost();
            int port = uri.getPort();
            if (!"localhost".equals(host) || port > 0) {
                LOGGER.warn("Ignoring configured host/port: {}", uri);
            }
        }

        @Override
        protected UndertowHost createUndertowHost() {
            return ServiceLocator.getRequiredService(UndertowHost.class);
        }
    }
}