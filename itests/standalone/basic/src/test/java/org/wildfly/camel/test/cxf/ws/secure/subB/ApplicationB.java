/*
 * #%L
 * Wildfly Camel :: Example :: Camel CXF JAX-WS CDI Secure
 * %%
 * Copyright (C) 2013 - 2017 RedHat
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
package org.wildfly.camel.test.cxf.ws.secure.subB;

import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Named;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSession;

import org.apache.camel.CamelContext;
import org.apache.camel.Processor;
import org.apache.camel.component.cxf.CxfComponent;
import org.apache.camel.component.cxf.CxfEndpoint;

@Named("cxf_cdi_security_app_b")
public class ApplicationB {

    public static final String CXF_ENDPOINT_URI = "https://localhost:8443/webservices-b/greeting-secure-cdi";

    @Inject
    CamelContext camelContext;

    @Named("cxfConsumerEndpointB")
    @Produces
    public CxfEndpoint createCxfConsumerEndpoint() {
        CxfComponent cxfConsumerComponent = new CxfComponent(this.camelContext);
        CxfEndpoint cxfConsumerEndpoint = new CxfEndpoint(CXF_ENDPOINT_URI, cxfConsumerComponent);
        cxfConsumerEndpoint.setBeanId("cxfConsumerEndpointB");
        cxfConsumerEndpoint.setServiceClass(GreetingService.class);
        return cxfConsumerEndpoint;
    }

    @Named("cxfProducerEndpointB")
    @Produces
    public CxfEndpoint createCxfProducerEndpoint() {
        CxfComponent cxfProducerComponent = new CxfComponent(this.camelContext);
        CxfEndpoint cxfProducerEndpoint = new CxfEndpoint(CXF_ENDPOINT_URI, cxfProducerComponent);
        cxfProducerEndpoint.setBeanId("cxfProducerEndpointB");
        cxfProducerEndpoint.setServiceClass(GreetingService.class);

        // Not for use in production
        HostnameVerifier hostnameVerifier = new HostnameVerifier() {
            @Override
            public boolean verify(String hostname, SSLSession session) {
                return true;
            }
        };
        cxfProducerEndpoint.setHostnameVerifier(hostnameVerifier);

        return cxfProducerEndpoint;
    }

    @Named("greetingsProcessorB")
    @Produces
    public Processor produceGreetingsProcessor() {
        return new GreetingsProcessor();
    }

}
