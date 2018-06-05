/*
 * #%L
 * Wildfly Camel :: Testsuite
 * %%
 * Copyright (C) 2013 - 2015 RedHat
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
package org.wildfly.camel.test.cxf.rs;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.MediaType;

import org.apache.camel.CamelContext;
import org.apache.camel.ServiceStatus;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.gravia.resource.ManifestBuilder;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.camel.test.common.http.HttpRequest;
import org.wildfly.camel.test.common.http.HttpRequest.HttpResponse;
import org.wildfly.camel.test.common.types.GreetingService;
import org.wildfly.extension.camel.CamelAware;
import org.wildfly.extension.camel.CamelContextRegistry;

@CamelAware
@RunWith(Arquillian.class)
public class CXFRSSecureConsumerIntegrationTest {

    private static String RS_ENDPOINT_PATH = "/cxfrestful/greet/hello/Kermit";
    private static String INSECURE_RS_ENDPOINT = "http://localhost:8080" + RS_ENDPOINT_PATH;
    private static String SECURE_RS_ENDPOINT = "https://localhost:8443" + RS_ENDPOINT_PATH;

    @ArquillianResource
    CamelContextRegistry contextRegistry;

    @Deployment
    public static JavaArchive deployment() {
        return ShrinkWrap.create(JavaArchive.class, "cxfrs-secure-consumer-tests")
            .addClasses(GreetingService.class, HttpRequest.class)
            .addAsResource("cxf/spring/cxfrs-secure-consumer-camel-context.xml", "cxfrs-secure-consumer-camel-context.xml")
            .setManifest(() -> {
                ManifestBuilder builder = new ManifestBuilder();
                builder.addManifestHeader("Dependencies", "org.jboss.resteasy.resteasy-jaxrs");
                return builder.openStream();
            });
    }

    @Test
    public void testCXFSecureConsumer() throws Exception {

        CamelContext camelctx = contextRegistry.getCamelContext("cxfrs-secure-undertow");
        Assert.assertNotNull("Expected cxfrs-secure-undertow to not be null", camelctx);
        Assert.assertEquals(ServiceStatus.Started, camelctx.getStatus());

        ClassLoader tccl = Thread.currentThread().getContextClassLoader();
        try {
            // The JAXRS Client API needs to see resteasy
            Thread.currentThread().setContextClassLoader(getClass().getClassLoader());

            // Force WildFly to generate a self-signed SSL cert & keystore
            HttpRequest.get("https://localhost:8443").throwExceptionOnFailure(false).getResponse();

            // Use the generated keystore
            String keystorePath = System.getProperty("jboss.server.config.dir") + "/application.keystore";
            System.setProperty("javax.net.ssl.trustStorePassword", "password");
            System.setProperty("javax.net.ssl.trustStore", keystorePath);

            Client client = ClientBuilder.newClient();

            Object result = client.target(SECURE_RS_ENDPOINT).request(MediaType.APPLICATION_JSON).get(String.class);
            Assert.assertEquals("Hello Kermit", result);

            // Verify that if we attempt to use HTTP, we get a 302 redirect to the HTTPS endpoint URL
            HttpResponse response = HttpRequest.get(INSECURE_RS_ENDPOINT)
                .throwExceptionOnFailure(false)
                .followRedirects(false)
                .getResponse();
            Assert.assertEquals(302, response.getStatusCode());
            Assert.assertEquals(response.getHeader("Location"), SECURE_RS_ENDPOINT);
        } finally {
            System.clearProperty("javax.net.ssl.trustStorePassword");
            System.clearProperty("javax.net.ssl.trustStore");
            Thread.currentThread().setContextClassLoader(tccl);
        }
    }
}
