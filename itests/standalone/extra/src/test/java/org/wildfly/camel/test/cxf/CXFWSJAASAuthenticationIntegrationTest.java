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
package org.wildfly.camel.test.cxf;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.camel.CamelContext;
import org.apache.camel.CamelExecutionException;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.cxf.CxfComponent;
import org.apache.camel.component.cxf.CxfEndpoint;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.cxf.interceptor.Interceptor;
import org.apache.cxf.interceptor.security.JAASLoginInterceptor;
import org.apache.cxf.message.Message;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.camel.test.common.types.Endpoint;
import org.wildfly.extension.camel.CamelAware;

@CamelAware
@RunWith(Arquillian.class)
public class CXFWSJAASAuthenticationIntegrationTest {

    @Deployment
    public static WebArchive createDeployment() {
        return ShrinkWrap.create(WebArchive.class, "camel-cxf-jaas-test.war")
            .addClass(Endpoint.class);
    }

    @Test
    public void testCXFEndpointJAASAuthenticationSuccess() throws Exception {
        CamelContext camelctx = configureCamelContext("appl-pa$$wrd1");
        camelctx.start();
        try {
            ProducerTemplate template = camelctx.createProducerTemplate();
            String result = template.requestBody("direct:start", "Kermit", String.class);
            Assert.assertEquals("Hello Kermit", result);
        } finally {
            camelctx.close();
        }
    }

    @Test
    public void testCXFEndpointJAASAuthenticationFailure() throws Exception {
        CamelContext camelctx = configureCamelContext("invalid-password");
        camelctx.start();
        try {
            ProducerTemplate template = camelctx.createProducerTemplate();
            template.requestBody("direct:start", "Kermit", String.class);
            Assert.fail("Expected CamelExecutionException");
        } catch (CamelExecutionException e) {
            Assert.assertTrue(e.getCause().getMessage().startsWith("Authentication failed"));
        }
        finally {
            camelctx.close();
        }
    }

    private CamelContext configureCamelContext(String password) throws Exception {
        CamelContext camelctx = new DefaultCamelContext();

        CxfComponent component = new CxfComponent(camelctx);
        CxfEndpoint consumerEndpoint = new CxfEndpoint("http://localhost:8080/webservices/greeting", component);
        consumerEndpoint.setServiceClass(Endpoint.class);

        List<Interceptor<? extends Message>> inInterceptors = consumerEndpoint.getInInterceptors();
        JAASLoginInterceptor interceptor =  new JAASLoginInterceptor();
        interceptor.setContextName("other");
        inInterceptors.add(interceptor);

        CxfEndpoint producerEndpoint = new CxfEndpoint("http://localhost:8080/webservices/greeting", component);
        producerEndpoint.setServiceClass(Endpoint.class);
        producerEndpoint.setUsername("user1");
        producerEndpoint.setPassword(password);

        Map<String, Object> properties = producerEndpoint.getProperties();
        if (properties == null) {
            producerEndpoint.setProperties(new HashMap<>());
        }

        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:start")
                .to(producerEndpoint);

                from(consumerEndpoint)
                .process(exchange -> {
                    Object[] args = exchange.getIn().getBody(Object[].class);
                    exchange.getOut().setBody("Hello " + args[0]);
                });
            }
        });
        return camelctx;
    }
}
