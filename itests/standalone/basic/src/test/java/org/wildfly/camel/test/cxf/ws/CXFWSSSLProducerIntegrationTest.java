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
package org.wildfly.camel.test.cxf.ws;

import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.ServiceStatus;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.camel.test.common.http.HttpRequest;
import org.wildfly.camel.test.common.types.Endpoint;
import org.wildfly.camel.test.cxf.ws.subA.EndpointImpl;
import org.wildfly.extension.camel.CamelAware;
import org.wildfly.extension.camel.CamelContextRegistry;

@CamelAware
@RunWith(Arquillian.class)
public class CXFWSSSLProducerIntegrationTest {

    @ArquillianResource
    CamelContextRegistry contextRegistry;

    @Deployment(name="cxf-consumer.war", testable = false, order = 1)
    public static WebArchive createCxfConsumerDeployment() {
        return ShrinkWrap.create(WebArchive.class, "cxf-consumer.war")
            .addClasses(Endpoint.class, EndpointImpl.class);
    }

    @Deployment(order = 2)
    public static JavaArchive createDeployment() throws Exception {
        // Force WildFly to generate a self-signed SSL cert & keystore
        HttpRequest.get("https://localhost:8443").throwExceptionOnFailure(false).getResponse();

        return ShrinkWrap.create(JavaArchive.class, "cxf-ws-ssl-producer-tests")
            .addClass(Endpoint.class)
            .addAsResource("cxf/spring/cxfws-ssl-producer-camel-context.xml", "cxfws-ssl-producer-camel-context.xml");
    }

    @Test
    public void testCxfWSSSLProducer() {
        CamelContext camelctx = contextRegistry.getCamelContext("cxfws-ssl-context");
        Assert.assertNotNull("Expected cxfrs-producer-context to not be null", camelctx);
        Assert.assertEquals(ServiceStatus.Started, camelctx.getStatus());

        ProducerTemplate template = camelctx.createProducerTemplate();
        String result = template.requestBody("direct:start", "Kermit", String.class);
        Assert.assertEquals("Hello Kermit", result);
    }
}
