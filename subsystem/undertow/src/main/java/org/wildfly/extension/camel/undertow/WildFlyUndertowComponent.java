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
package org.wildfly.extension.camel.undertow;

import static org.wildfly.extension.camel.CamelLogger.LOGGER;

import java.net.URI;
import java.net.URISyntaxException;

import org.apache.camel.Consumer;
import org.apache.camel.Processor;
import org.apache.camel.component.undertow.UndertowComponent;
import org.apache.camel.component.undertow.UndertowConsumer;
import org.apache.camel.component.undertow.UndertowEndpoint;
import org.apache.camel.component.undertow.UndertowHost;
import org.apache.camel.component.undertow.UndertowHostKey;
import org.wildfly.camel.utils.ServiceLocator;
import org.wildfly.extension.camel.parser.SubsystemState.RuntimeState;

/**
 * An extension to the {@link UndertowComponent}
 *
 * @author Thomas.Diesler@jboss.com
 * @since 30-Jul-2015
 */
public class WildFlyUndertowComponent extends UndertowComponent {

    private final RuntimeState runtimeState;

    public WildFlyUndertowComponent(RuntimeState runtimeState) {
        this.runtimeState = runtimeState;
    }

    @Override
    protected UndertowEndpoint createEndpointInstance(URI endpointUri, UndertowComponent component) throws URISyntaxException {
        return new WildFlyUndertowEndpoint(endpointUri.toString(), component);
    }

    @Override
    protected UndertowHost createUndertowHost(UndertowHostKey key) {
        return ServiceLocator.getRequiredService(CamelUndertowHostService.SERVICE_NAME, UndertowHost.class);
    }

    class WildFlyUndertowEndpoint extends UndertowEndpoint {

        WildFlyUndertowEndpoint(String uri, UndertowComponent component) throws URISyntaxException {
            super(uri, component);
        }

        @Override
        public Consumer createConsumer(Processor processor) throws Exception {
            return new WildFlyUndertowUndertowConsumer(this, processor);
        }
    }

    class WildFlyUndertowUndertowConsumer extends UndertowConsumer {

        WildFlyUndertowUndertowConsumer(UndertowEndpoint endpoint, Processor processor) throws Exception {
            super(endpoint, processor);
            URI uri = new URI(endpoint.getEndpointUri());
            String host = uri.getHost();
            if (!"localhost".equals(host)) {
                LOGGER.warn("Ignoring configured host: {}", uri);
            }
        }

        @Override
        protected void doStart() throws Exception {
            super.doStart();
            URI httpUri = getEndpoint().getHttpURI();
            String contextPath = httpUri.getPath();
            runtimeState.addHttpContext(contextPath);
        }

        @Override
        protected void doStop() throws Exception {
            URI httpUri = getEndpoint().getHttpURI();
            String contextPath = httpUri.getPath();
            runtimeState.removeHttpContext(contextPath);
            super.doStop();
        }
    }
}
