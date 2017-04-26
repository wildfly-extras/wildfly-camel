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

package org.wildfly.camel.test.undertow;

import java.net.URL;
import java.util.List;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.impl.DefaultCamelContext;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.camel.test.http4.subA.MyServlet;
import org.wildfly.extension.camel.EndpointRegistryClient;

@RunAsClient
@RunWith(Arquillian.class)
public class UndertowProducerIntegrationTest {

    @ArquillianResource
    ManagementClient managementClient;

    @Deployment
    public static WebArchive createDeployment() {
        final WebArchive archive = ShrinkWrap.create(WebArchive.class, "undertow-producer.war");
        archive.addClasses(MyServlet.class);
        return archive;
    }

    @Test
    public void testHttpProducer() throws Exception {

        // Obtain the endpoint URL from the management model
        EndpointRegistryClient registryClient = new EndpointRegistryClient(managementClient.getControllerClient());
        final String[] endpoints = wrapEndpoints(registryClient.getRegisteredEndpoints("/undertow-producer"));

        CamelContext camelctx = new DefaultCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:start").loadBalance().roundRobin().to(endpoints);
            }
        });

        camelctx.start();
        try {
            ProducerTemplate producer = camelctx.createProducerTemplate();
            String result = producer.requestBodyAndHeader("direct:start", null, Exchange.HTTP_QUERY, "name=Kermit", String.class);
            Assert.assertEquals("Hello Kermit", result);
        } finally {
            camelctx.stop();
        }
    }

    private String[] wrapEndpoints(List<URL> urls) {
        String[] result = new String[urls.size()];
        for (int i = 0; i < urls.size(); i++) {
            result[i] = "undertow:" + urls.get(i) + "/myservlet";
        }
        return result;
    }
}
