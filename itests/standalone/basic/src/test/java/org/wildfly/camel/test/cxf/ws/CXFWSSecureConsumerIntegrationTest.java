/*
 * #%L
 * Wildfly Camel :: Testsuite
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
package org.wildfly.camel.test.cxf.ws;

import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.cxf.CxfComponent;
import org.apache.camel.component.cxf.CxfEndpoint;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.support.jsse.KeyManagersParameters;
import org.apache.camel.support.jsse.KeyStoreParameters;
import org.apache.camel.support.jsse.SSLContextClientParameters;
import org.apache.camel.support.jsse.SSLContextParameters;
import org.apache.camel.support.jsse.TrustManagersParameters;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.camel.test.common.http.HttpRequest;
import org.wildfly.camel.test.common.http.HttpRequest.HttpResponse;
import org.wildfly.camel.test.common.types.Endpoint;
import org.wildfly.extension.camel.CamelAware;

@CamelAware
@RunWith(Arquillian.class)
public class CXFWSSecureConsumerIntegrationTest {

    private static final String WS_ENDPOINT_PATH = "/EndpointService/EndpointPort";
    private static final String INSECURE_WS_ENDPOINT_URL = "http://localhost:8080" + WS_ENDPOINT_PATH;
    private static final String SECURE_WS_ENDPOINT_URL = "https://localhost:8443" + WS_ENDPOINT_PATH;

    @Deployment
    public static WebArchive deployment() {
        return ShrinkWrap.create(WebArchive.class, "cxf-ws-secure-consumer-tests.war")
            .addClasses(Endpoint.class, HttpRequest.class);
    }

    @Test
    public void testCXFSecureConsumer() throws Exception {
        CamelContext camelContext = new DefaultCamelContext();

        CxfComponent cxfComponent = camelContext.getComponent("cxf", CxfComponent.class);

        CxfEndpoint cxfProducer = createCxfEndpoint(SECURE_WS_ENDPOINT_URL, cxfComponent);
        cxfProducer.setSslContextParameters(createSSLContextParameters());

        CxfEndpoint cxfConsumer = createCxfEndpoint(SECURE_WS_ENDPOINT_URL, cxfComponent);

        camelContext.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:start")
                .to(cxfProducer);

                from(cxfConsumer)
                .transform(simple("Hello ${body}"));
            }
        });

        try {
            // Force WildFly to generate a self-signed SSL cert & keystore
            HttpRequest.get("https://localhost:8443").throwExceptionOnFailure(false).getResponse();

            camelContext.start();

            ProducerTemplate template = camelContext.createProducerTemplate();
            String result = template.requestBody("direct:start", "Kermit", String.class);

            Assert.assertEquals("Hello Kermit", result);

            // Verify that if we attempt to use HTTP, we get a 302 redirect to the HTTPS endpoint URL
            HttpResponse response = HttpRequest.get(INSECURE_WS_ENDPOINT_URL + "?wsdl")
                .throwExceptionOnFailure(false)
                .followRedirects(false)
                .getResponse();
            Assert.assertEquals(302, response.getStatusCode());
            Assert.assertEquals(response.getHeader("Location"), SECURE_WS_ENDPOINT_URL + "?wsdl");
        } finally {
            camelContext.stop();
        }
    }

    private CxfEndpoint createCxfEndpoint(String endpointUrl, CxfComponent component) throws Exception {
        CxfEndpoint cxfEndpoint = new CxfEndpoint(endpointUrl, component);
        cxfEndpoint.setServiceClass(Endpoint.class.getName());
        return cxfEndpoint;
    }

    private SSLContextParameters createSSLContextParameters() {
        KeyStoreParameters ksp = new KeyStoreParameters();
        ksp.setResource(System.getProperty("jboss.server.config.dir") + "/application.keystore");
        ksp.setPassword("password");

        KeyManagersParameters kmp = new KeyManagersParameters();
        kmp.setKeyStore(ksp);
        kmp.setKeyPassword("password");

        SSLContextClientParameters sslContextClientParameters = new SSLContextClientParameters();
        SSLContextParameters sslContextParameters = new SSLContextParameters();
        sslContextParameters.setClientParameters(sslContextClientParameters);
        sslContextParameters.setKeyManagers(kmp);

        TrustManagersParameters tmp = new TrustManagersParameters();
        tmp.setKeyStore(ksp);
        sslContextParameters.setTrustManagers(tmp);

        return sslContextParameters;
    }
}
