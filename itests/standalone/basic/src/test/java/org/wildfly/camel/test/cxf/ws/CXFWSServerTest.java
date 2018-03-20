/*
 * #%L
 * Wildfly Camel :: Testsuite
 * %%
 * Copyright (C) 2013 - 2018 RedHat
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
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.camel.test.common.types.Endpoint;
import org.wildfly.camel.test.cxf.ws.subA.EndpointImpl;
import org.wildfly.extension.camel.CamelAware;
import org.wildfly.extension.camel.CamelContextRegistry;

@CamelAware
@RunWith(Arquillian.class)
public class CXFWSServerTest {

    @ArquillianResource
    private CamelContextRegistry contextRegistry;

    @Deployment
    public static JavaArchive createDeployment() {
        return ShrinkWrap.create(JavaArchive.class, "cxf-ws-server-tests.jar")
            .addClasses(Endpoint.class, EndpointImpl.class)
            .addAsResource("cxf/spring/cxfws-server-camel-context.xml", "cxfws-server-camel-context.xml");
    }

    @Test
    public void testCXFWsServer() throws Exception {
        CamelContext camelctx = contextRegistry.getCamelContext("cxf-server-context");
        Assert.assertNotNull("Expected cxf-server-context to not be null", camelctx);
        Assert.assertEquals(ServiceStatus.Started, camelctx.getStatus());

        ProducerTemplate template = camelctx.createProducerTemplate();
        String result = template.requestBody("direct:start", "Kermit", String.class);
        Assert.assertEquals("Hello Kermit", result);
    }
}
