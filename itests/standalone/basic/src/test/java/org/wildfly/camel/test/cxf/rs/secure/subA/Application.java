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
package org.wildfly.camel.test.cxf.rs.secure.subA;

import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Named;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSession;

import org.apache.camel.CamelContext;
import org.apache.camel.Processor;
import org.apache.camel.cdi.ContextName;
import org.apache.camel.component.cxf.jaxrs.CxfRsComponent;
import org.apache.camel.component.cxf.jaxrs.CxfRsEndpoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wildfly.camel.test.cxf.rs.secure.CXFRSBasicSecureProducerIntegrationTest;

@Named("cxf_cdi_security_app")
public class Application {

    public static final String CXF_ENDPOINT_BASE_URI = "https://localhost:8443/rest";
    public static final String CXF_ENDPOINT_URI = CXF_ENDPOINT_BASE_URI + "/greet/hi";
    public static final String CXF_ENDPOINT_SUB_BASE_URI = CXF_ENDPOINT_BASE_URI + "/sub";
    public static final String CXF_ENDPOINT_SUB_URI = CXF_ENDPOINT_SUB_BASE_URI + "/greet/hi";
    public static final String CXF_ENDPOINT_REL_BASE_URI = "https://localhost:8443/"+ CXFRSBasicSecureProducerIntegrationTest.APP_NAME +"/rel-greeting-secure-cdi";
    public static final String CXF_ENDPOINT_REL_URI = CXF_ENDPOINT_REL_BASE_URI + "/greet/hi";

    private static final Logger log = LoggerFactory.getLogger(Application.class);

    @Inject
    @ContextName("cxfrs-secure-cdi-camel-context")
    CamelContext camelContext;

    @Named("cxfConsumerEndpoint")
    @Produces
    public CxfRsEndpoint createCxfConsumerEndpoint() throws Exception {
        CxfRsEndpoint cxfConsumerEndpoint = this.camelContext.getEndpoint("cxfrs:" + CXF_ENDPOINT_BASE_URI, CxfRsEndpoint.class);
        cxfConsumerEndpoint.setBeanId("cxfConsumerEndpoint");
        cxfConsumerEndpoint.addResourceClass(GreetingsService.class);
        return cxfConsumerEndpoint;
    }

    @Named("cxfConsumerEndpointSub")
    @Produces
    public CxfRsEndpoint createCxfConsumerEndpointSub() {
        CxfRsEndpoint cxfConsumerEndpoint = this.camelContext.getEndpoint("cxfrs:" + CXF_ENDPOINT_SUB_BASE_URI, CxfRsEndpoint.class);
        cxfConsumerEndpoint.setBeanId("cxfConsumerEndpointSub");
        cxfConsumerEndpoint.addResourceClass(GreetingsService.class);
        return cxfConsumerEndpoint;
    }

    @Named("cxfConsumerEndpointRel")
    @Produces
    public CxfRsEndpoint createCxfConsumerEndpointRel() {
        CxfRsEndpoint cxfConsumerEndpoint = this.camelContext.getEndpoint("cxfrs:" + CXF_ENDPOINT_REL_BASE_URI, CxfRsEndpoint.class);
        cxfConsumerEndpoint.setBeanId("cxfConsumerEndpointRel");
        cxfConsumerEndpoint.addResourceClass(GreetingsService.class);
        return cxfConsumerEndpoint;
    }

    @Named("cxfProducerEndpoint")
    @Produces
    public CxfRsEndpoint createCxfProducerEndpoint() {
        CxfRsComponent cxfProducerComponent = new CxfRsComponent(this.camelContext);
        CxfRsEndpoint cxfProducerEndpoint = new CxfRsEndpoint(CXF_ENDPOINT_BASE_URI, cxfProducerComponent);
        cxfProducerEndpoint.setBeanId("cxfProducerEndpoint");
        cxfProducerEndpoint.addResourceClass(GreetingsService.class);

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
    public CxfRsEndpoint createCxfProducerEndpointSub() {
        CxfRsComponent cxfProducerComponent = new CxfRsComponent(this.camelContext);
        CxfRsEndpoint cxfProducerEndpoint = new CxfRsEndpoint(CXF_ENDPOINT_SUB_BASE_URI, cxfProducerComponent);
        cxfProducerEndpoint.setBeanId("cxfProducerEndpointSub");
        cxfProducerEndpoint.addResourceClass(GreetingsService.class);

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
    public CxfRsEndpoint createCxfProducerEndpointRel() {
        CxfRsComponent cxfProducerComponent = new CxfRsComponent(this.camelContext);
        CxfRsEndpoint cxfProducerEndpoint = new CxfRsEndpoint(CXF_ENDPOINT_REL_BASE_URI, cxfProducerComponent);
        cxfProducerEndpoint.setBeanId("cxfProducerEndpointRel");
        cxfProducerEndpoint.addResourceClass(GreetingsService.class);

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
