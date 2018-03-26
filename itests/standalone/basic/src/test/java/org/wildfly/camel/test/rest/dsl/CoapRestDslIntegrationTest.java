/*
 * #%L
 * Wildfly Camel :: Testsuite
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
package org.wildfly.camel.test.rest.dsl;

import java.util.Collections;
import java.util.Map;

import org.apache.camel.CamelContext;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.impl.DefaultCamelContext;
import org.eclipse.californium.core.CoapClient;
import org.eclipse.californium.core.CoapResponse;
import org.eclipse.californium.core.coap.MediaTypeRegistry;
import org.eclipse.californium.core.network.config.NetworkConfig;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.gravia.resource.ManifestBuilder;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.camel.test.common.http.HttpRequest;
import org.wildfly.camel.test.common.http.HttpRequest.HttpResponse;
import org.wildfly.camel.test.common.utils.AvailablePortFinder;

@RunWith(Arquillian.class)
public class CoapRestDslIntegrationTest extends AbstractRestDslIntegrationTest {

    private int port = AvailablePortFinder.getNextAvailable();

    @Deployment
    public static JavaArchive createDeployment() {
        return ShrinkWrap.create(JavaArchive.class, "camel-coap-rest-dsl-tests.jar")
            .addClasses(HttpRequest.class, AvailablePortFinder.class, AbstractRestDslIntegrationTest.class)
            .setManifest(() -> {
                ManifestBuilder builder = new ManifestBuilder();
                builder.addManifestHeader("Dependencies", "org.eclipse.californium");
                return builder.openStream();
            });
    }

    @Override
    protected String getComponentName() {
        return "coap";
    }

    @Override
    protected int getPort() {
        return this.port;
    }

    @Override
    protected HttpClient createHttpClient() {
        return new HttpClient() {
            private String coapUri = "coap://localhost:%d/%s?coapMethod=%s";

            @Override
            public HttpResponse getResponse(String url, String method) throws Exception {
                NetworkConfig.createStandardWithoutFile();
                CoapClient client = new CoapClient(String.format(coapUri, port, url, method));
                CoapResponse coapResponse = null;

                switch (method) {
                    case "DELETE":
                        coapResponse = client.delete();
                        break;
                    case "GET":
                        coapResponse = client.get();
                        break;
                    case "OPTIONS":
                        // There is no concept of OPTIONS with CoAP
                        coapResponse = client.get();
                        break;
                    case "PUT":
                        coapResponse = client.put("", MediaTypeRegistry.TEXT_PLAIN);
                        break;
                    case "POST":
                        coapResponse = client.post("", MediaTypeRegistry.TEXT_PLAIN);
                        break;
                }

                return new DelegatingCoapResponse(coapResponse);
            }

            class DelegatingCoapResponse extends HttpResponse {
                private final CoapResponse response;

                private DelegatingCoapResponse(CoapResponse response) {
                    this.response = response;
                }

                @Override
                public String getBody() {
                    return this.response.getResponseText();
                }

                @Override
                public int getStatusCode() {
                    switch (this.response.getCode()) {
                        case CONTENT:
                            return 200;
                        case VALID:
                            return 200;
                        case NOT_FOUND:
                            return 404;
                        case METHOD_NOT_ALLOWED:
                            return 405;
                        case INTERNAL_SERVER_ERROR:
                            return 500;
                        default:
                            return this.response.getCode().value;
                    }
                }

                @Override
                public Map<String, String> getHeaders() {
                    return Collections.emptyMap();
                }
            }
        };
    }

    @Override
    @Ignore("[CAMEL-12401] CoAP component should support REST context path configuration")
    public void testRestDslWithContextPath() throws Exception {
    }

    @Test
    public void testRestDslCorsEnabled() throws Exception {
        CamelContext camelctx = new DefaultCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                restConfiguration()
                    .component("coap")
                    .host("localhost")
                    .port(port)
                    .enableCORS(true);

                rest()
                    .get("/test")
                        .route()
                            .setBody(constant("GET: /test"))
                        .endRest();
            }
        });

        camelctx.start();
        try {
            // CORS is not supported by CoAP
            HttpResponse response = client.getResponse("test", "OPTIONS");
            Assert.assertEquals(200, response.getStatusCode());
            Assert.assertEquals("GET: /test", response.getBody());
        } finally {
            camelctx.stop();
        }
    }
}
