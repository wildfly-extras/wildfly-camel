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

import java.net.URL;

import javax.xml.namespace.QName;
import javax.xml.ws.Service;

import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.ServiceStatus;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.camel.test.common.types.Endpoint;
import org.wildfly.extension.camel.CamelAware;
import org.wildfly.extension.camel.CamelContextRegistry;

/**
 * Test WebService endpoint access with the cxf component.
 *
 * @author thomas.diesler@jboss.com
 * @since 11-Jun-2013
 */
@CamelAware
@RunWith(Arquillian.class)
public class CXFWSConsumerIntegrationTest {

    @ArquillianResource
    CamelContextRegistry contextRegistry;

    @Deployment
    public static WebArchive deployment() {
        final WebArchive archive = ShrinkWrap.create(WebArchive.class, "cxf-ws-consumer-tests.war");
        archive.addClasses(Endpoint.class);
        archive.addAsResource("cxf/spring/cxfws-consumer-camel-context.xml", "cxfws-consumer-camel-context.xml");
        return archive;
    }

    @Test
    public void testCXFConsumer() throws Exception {

        CamelContext camelctx = contextRegistry.getCamelContext("cxfws-undertow");
        Assert.assertNotNull("Expected cxfws-undertow to not be null", camelctx);
        Assert.assertEquals(ServiceStatus.Started, camelctx.getStatus());

        QName qname = new QName("http://wildfly.camel.test.cxf", "EndpointService");
        Service service = Service.create(new URL("http://localhost:8080/EndpointService/EndpointPort?wsdl"), qname);
        Endpoint endpoint = service.getPort(Endpoint.class);
        Assert.assertNotNull("Endpoint not null", endpoint);

        Assert.assertEquals("Hello Kermit", endpoint.echo("Kermit"));
    }

    @Test
    public void testCXFRoundtrip() throws Exception {

        CamelContext camelctx = contextRegistry.getCamelContext("cxfws-undertow");
        Assert.assertNotNull("Expected cxfws-undertow to not be null", camelctx);
        Assert.assertEquals(ServiceStatus.Started, camelctx.getStatus());

        ProducerTemplate producer = camelctx.createProducerTemplate();
        String result = producer.requestBody("direct:start", "Kermit", String.class);
        Assert.assertEquals("Hello Kermit", result);
    }
}
