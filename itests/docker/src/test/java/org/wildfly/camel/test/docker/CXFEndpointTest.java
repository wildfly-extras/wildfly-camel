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

package org.wildfly.camel.test.docker;

import java.net.URL;

import javax.xml.namespace.QName;
import javax.xml.ws.Service;

import org.arquillian.cube.HostIp;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.camel.test.common.types.Endpoint;

@RunAsClient
@RunWith(Arquillian.class)
public class CXFEndpointTest {

    @HostIp
    private String wildflyIp;

    @Deployment(testable = false)
    public static WebArchive deployment() {
        final WebArchive archive = ShrinkWrap.create(WebArchive.class, "cxf-ws-consumer.war");
        archive.addClasses(Endpoint.class);
        archive.addAsResource("cxf/cxfws-consumer-camel-context.xml");
        return archive;
    }

    @Test
    public void testEndpoint() throws Exception {

        QName qname = new QName("http://wildfly.camel.test.cxf", "EndpointService");
        Service service = Service.create(new URL("http://" + wildflyIp + ":8080/EndpointService/EndpointPort?wsdl"), qname);
        Endpoint endpoint = service.getPort(Endpoint.class);
        Assert.assertNotNull("Endpoint not null", endpoint);

        Assert.assertEquals("Hello Kermit", endpoint.echo("Kermit"));
    }
}
