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

package org.wildfly.camel.test.http4;

import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.impl.DefaultCamelContext;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.camel.test.common.http.HttpRequest;
import org.wildfly.extension.camel.CamelAware;

@CamelAware
@RunWith(Arquillian.class)
public class Https4IntegrationTest {

    @Deployment
    public static WebArchive createDeployment() {
        return ShrinkWrap.create(WebArchive.class, "Https4IntegrationTest.war")
            .addClass(HttpRequest.class);
    }

    @Test
    public void testHttpsGetRequest() throws Exception {

        CamelContext camelctx = new DefaultCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("undertow:http://localhost:8080/hello")
                .setBody(constant("Hello Kermit"));
            }
        });

        camelctx.start();
        try {
            ProducerTemplate template = camelctx.createProducerTemplate();

            // Force WildFly to generate a self-signed SSL cert & keystore
            HttpRequest.get("https://localhost:8443").throwExceptionOnFailure(false).getResponse();

            // Use the generated keystore
            String keystorePath = System.getProperty("jboss.server.config.dir") + "/application.keystore";
            System.setProperty("javax.net.ssl.trustStorePassword", "password");
            System.setProperty("javax.net.ssl.trustStore", keystorePath);

            // Verify we can hit the secure endpoint
            String result = template.requestBody("https4://localhost:8443/hello", null, String.class);
            Assert.assertEquals("Hello Kermit", result);
        } finally {
            System.clearProperty("javax.net.ssl.trustStorePassword");
            System.clearProperty("javax.net.ssl.trustStore");
            camelctx.stop();
        }
    }

}
