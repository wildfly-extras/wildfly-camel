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
package org.wildfly.camel.test.cxf.ws.secure.subA;

import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Named;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSession;

import org.apache.camel.CamelContext;
import org.apache.camel.Processor;
import org.apache.camel.cdi.ContextName;
import org.apache.camel.component.cxf.CxfComponent;
import org.apache.camel.component.cxf.CxfEndpoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wildfly.camel.test.cxf.ws.secure.CXFWSBasicSecureProducerIntegrationTest;

@Named("cxf_cdi_security_app")
public class Application {

    public static final String CXF_ENDPOINT_URI = "https://localhost:8443/webservices/greeting-secure-cdi";
    public static final String CXF_ENDPOINT_SUB_URI = "https://localhost:8443/webservices/greeting-secure-cdi/sub";
    public static final String CXF_ENDPOINT_REL_URI = "https://localhost:8443/"+ CXFWSBasicSecureProducerIntegrationTest.APP_NAME +"/rel-greeting-secure-cdi";
    private static final Logger log = LoggerFactory.getLogger(Application.class);

    @Inject
    @ContextName("cxfws-secure-cdi-camel-context")
    CamelContext camelContext;

    @Named("cxfConsumerEndpoint")
    @Produces
    public CxfEndpoint createCxfConsumerEndpoint() {
        CxfComponent cxfConsumerComponent = new CxfComponent(this.camelContext);
        CxfEndpoint cxfConsumerEndpoint = new CxfEndpoint(CXF_ENDPOINT_URI, cxfConsumerComponent);
        cxfConsumerEndpoint.setBeanId("cxfConsumerEndpoint");
        cxfConsumerEndpoint.setServiceClass(GreetingService.class);
        return cxfConsumerEndpoint;
    }

    @Named("cxfConsumerEndpointSub")
    @Produces
    public CxfEndpoint createCxfConsumerEndpointSub() {
        CxfComponent cxfConsumerComponent = new CxfComponent(this.camelContext);
        CxfEndpoint cxfConsumerEndpoint = new CxfEndpoint(CXF_ENDPOINT_SUB_URI, cxfConsumerComponent);
        cxfConsumerEndpoint.setBeanId("cxfConsumerEndpointSub");
        cxfConsumerEndpoint.setServiceClass(GreetingService.class);
        return cxfConsumerEndpoint;
    }

    @Named("cxfConsumerEndpointRel")
    @Produces
    public CxfEndpoint createCxfConsumerEndpointRel() {
        CxfComponent cxfConsumerComponent = new CxfComponent(this.camelContext);
        CxfEndpoint cxfConsumerEndpoint = new CxfEndpoint(CXF_ENDPOINT_REL_URI, cxfConsumerComponent);
        cxfConsumerEndpoint.setBeanId("cxfConsumerEndpointRel");
        cxfConsumerEndpoint.setServiceClass(GreetingService.class);
        return cxfConsumerEndpoint;
    }

    @Named("cxfProducerEndpoint")
    @Produces
    public CxfEndpoint createCxfProducerEndpoint() {
        CxfComponent cxfProducerComponent = new CxfComponent(this.camelContext);
        CxfEndpoint cxfProducerEndpoint = new CxfEndpoint(CXF_ENDPOINT_URI, cxfProducerComponent);
        cxfProducerEndpoint.setBeanId("cxfProducerEndpoint");
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

    @Named("cxfProducerEndpointSub")
    @Produces
    public CxfEndpoint createCxfProducerEndpointSub() {
        CxfComponent cxfProducerComponent = new CxfComponent(this.camelContext);
        CxfEndpoint cxfProducerEndpoint = new CxfEndpoint(CXF_ENDPOINT_SUB_URI, cxfProducerComponent);
        cxfProducerEndpoint.setBeanId("cxfProducerEndpointSub");
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

    @Named("cxfProducerEndpointRel")
    @Produces
    public CxfEndpoint createCxfProducerEndpointRel() {
        CxfComponent cxfProducerComponent = new CxfComponent(this.camelContext);
        CxfEndpoint cxfProducerEndpoint = new CxfEndpoint(CXF_ENDPOINT_REL_URI, cxfProducerComponent);
        cxfProducerEndpoint.setBeanId("cxfProducerEndpointRel");
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
    @Named("greetingsProcessor")
    @Produces
    public Processor produceGreetingsProcessor() {
        return new GreetingsProcessor();
    }

}
