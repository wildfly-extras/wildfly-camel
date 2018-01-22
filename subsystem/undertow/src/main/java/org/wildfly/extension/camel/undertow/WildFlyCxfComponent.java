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

import org.apache.camel.Consumer;
import org.apache.camel.Processor;
import org.apache.camel.component.cxf.CxfComponent;
import org.apache.camel.component.cxf.CxfConsumer;
import org.apache.camel.component.cxf.CxfEndpoint;
import org.apache.camel.component.cxf.CxfSpringEndpoint;

/**
 * An extension to the {@link CxfComponent}
 *
 * @author Thomas.Diesler@jboss.com
 * @since 12-Oct-2015
 */
public class WildFlyCxfComponent extends CxfComponent {

    @Override
    protected CxfEndpoint createCxfEndpoint(String remaining) {
        return new WildflyCxfEndpoint(remaining, this);
    }

    @Override
    protected CxfEndpoint createCxfSpringEndpoint(String beanId) throws Exception {
        return super.createCxfSpringEndpoint(beanId);
    }

    class WildflyCxfEndpoint extends CxfEndpoint {

        WildflyCxfEndpoint(String remaining, WildFlyCxfComponent component) {
            super(remaining, component);
        }

        @Override
        public Consumer createConsumer(Processor processor) throws Exception {
            return new WildflyCxfConsumer(this, processor);
        }
    }

    class WildflyCxfSpringEndpoint extends CxfSpringEndpoint {

        @Override
        public Consumer createConsumer(Processor processor) throws Exception {
            return new WildflyCxfConsumer(this, processor);
        }
    }

    class WildflyCxfConsumer extends CxfConsumer {

        WildflyCxfConsumer(CxfEndpoint endpoint, Processor processor) throws Exception {
            super(endpoint, processor);
            URI uri = new URI(endpoint.getEndpointUri());
            String host = uri.getHost();
            int port = uri.getPort();
            if (!"localhost".equals(host) || port > 0) {
                LOGGER.warn("Ignoring configured host/port: {}", uri);
            }
        }
    }
}
