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

package org.wildfly.camel.test.camel.cxf;

import java.net.URL;

import javax.xml.namespace.QName;
import javax.xml.ws.Service;

import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.ServiceStatus;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.wildfly.camel.test.common.types.Endpoint;
import org.wildfly.extension.camel.SpringCamelContextFactory;

public class CXFWebServiceConsumerTest {

    static CamelContext camelctx;

    @BeforeClass
    public static void beforeClass() throws Exception {
        ClassLoader classLoader = CXFWebServiceConsumerTest.class.getClassLoader();
        URL resurl = classLoader.getResource("cxf/cxfws-camel-context.xml");
        camelctx = SpringCamelContextFactory.createSingleCamelContext(resurl, classLoader);
        camelctx.start();
    }

    @AfterClass
    public static void afterClass() throws Exception {
        if (camelctx != null) {
            camelctx.stop();
        }
    }

    @Test
    public void testCXFConsumer() throws Exception {

        Assert.assertEquals(ServiceStatus.Started, camelctx.getStatus());

        QName qname = new QName("http://wildfly.camel.test.cxf", "EndpointService");
        Service service = Service.create(new URL("http://localhost:8080/EndpointService/EndpointPort?wsdl"), qname);
        Endpoint endpoint = service.getPort(Endpoint.class);
        Assert.assertNotNull("Endpoint not null", endpoint);

        Assert.assertEquals("Hello Kermit", endpoint.echo("Kermit"));
    }

    @Test
    public void testCXFRoundtrip() throws Exception {

        Assert.assertEquals(ServiceStatus.Started, camelctx.getStatus());

        ProducerTemplate producer = camelctx.createProducerTemplate();
        String result = producer.requestBody("direct:start", "Kermit", String.class);
        Assert.assertEquals("Hello Kermit", result);
    }

}
