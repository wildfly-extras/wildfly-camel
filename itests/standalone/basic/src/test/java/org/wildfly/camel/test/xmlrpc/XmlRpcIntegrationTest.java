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

package org.wildfly.camel.test.xmlrpc;

import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.component.xmlrpc.XmlRpcRequestImpl;
import org.apache.camel.dataformat.xmlrpc.XmlRpcDataFormat;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.xmlrpc.XmlRpcRequest;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.extension.camel.CamelAware;

@CamelAware
@RunWith(Arquillian.class)
public class XmlRpcIntegrationTest {

    @Deployment
    public static JavaArchive createDeployment() {
        return ShrinkWrap.create(JavaArchive.class, "camel-xmlrpc-tests");
    }

    @Test
    public void testRequestMessage() throws Exception {
        
        CamelContext camelctx = new DefaultCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                
                XmlRpcDataFormat request = new XmlRpcDataFormat();
                request.setRequest(true);
                
                from("direct:request")
                    .marshal(request)
                    .to("log:marshalRequestMessage")
                    .unmarshal(request)
                    .to("log:unmarshalRequestMessage")
                    .to("mock:request");
            }
        });

        camelctx.start();
        try {
            ProducerTemplate template = camelctx.createProducerTemplate();
            MockEndpoint mock = camelctx.getEndpoint("mock:request", MockEndpoint.class);
            mock.expectedMessageCount(1);
            XmlRpcRequest result = template.requestBody("direct:request", new XmlRpcRequestImpl("greet", new Object[]{"you", 2}), XmlRpcRequest.class);
            Assert.assertNotNull(result);
            Assert.assertEquals("Get a wrong request operation name", "greet", result.getMethodName());
            Assert.assertEquals("Get a wrong request parameter size", 2, result.getParameterCount());
            Assert.assertEquals("Get a wrong request parameter", 2, result.getParameter(1));
            mock.assertIsSatisfied();
        } finally {
            camelctx.stop();
        }
    }

    @Test
    public void testResponseMessage() throws Exception {
        
        CamelContext camelctx = new DefaultCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                
                XmlRpcDataFormat response = new XmlRpcDataFormat();
                response.setRequest(false);
                
                from("direct:response")
                    .marshal(response)
                    .to("log:marshalResponseMessage")
                    .unmarshal(response)
                    .to("log:unmarshalResonseMessage")
                    .to("mock:response");
                    
            }
        });

        camelctx.start();
        try {
            ProducerTemplate template = camelctx.createProducerTemplate();
            MockEndpoint mock = camelctx.getEndpoint("mock:response", MockEndpoint.class);
            mock.expectedBodiesReceived("GreetMe from XmlRPC");
            template.sendBody("direct:response", "GreetMe from XmlRPC");
            mock.assertIsSatisfied();
        } finally {
            camelctx.stop();
        }
    }
}
