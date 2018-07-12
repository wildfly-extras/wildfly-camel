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

import java.net.URL;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.xml.namespace.QName;
import javax.xml.ws.Service;

import org.apache.camel.CamelContext;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.cxf.CxfComponent;
import org.apache.camel.component.cxf.CxfEndpoint;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.cxf.interceptor.AttachmentInInterceptor;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.interceptor.Interceptor;
import org.apache.cxf.interceptor.LoggingInInterceptor;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.ws.policy.PolicyInInterceptor;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.camel.test.common.types.Endpoint;
import org.wildfly.extension.camel.CamelAware;

@CamelAware
@RunWith(Arquillian.class)
public class CXFWSInterceptorTest {

    @Deployment
    public static WebArchive createDeployment() {
        return ShrinkWrap.create(WebArchive.class, "camel-cxfws-interceptor-tests.war")
            .addClass(Endpoint.class);
    }

    @Test
    public void testCXFInterceptor() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);

        CamelContext camelctx = new DefaultCamelContext();

        CxfComponent component = new CxfComponent(camelctx);
        CxfEndpoint cxfEndpoint = new CxfEndpoint("http://localhost:8080/EndpointService/EndpointPort", component);
        cxfEndpoint.setServiceClass(Endpoint.class);

        List<Interceptor<? extends Message>> inInterceptors = cxfEndpoint.getInInterceptors();
        inInterceptors.add(new CountDownInterceptor(latch));

        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from(cxfEndpoint)
                .setBody(constant("Hello ${body}"));
            }
        });

        camelctx.start();
        try {
            QName qname = new QName("http://wildfly.camel.test.cxf", "EndpointService");
            Service service = Service.create(new URL("http://localhost:8080/EndpointService/EndpointPort?wsdl"), qname);
            Endpoint endpoint = service.getPort(Endpoint.class);
            endpoint.echo("Kermit");
            Assert.assertTrue("Gave up waiting for CXF interceptor handleMessage", latch.await(5, TimeUnit.SECONDS));
        } finally {
            camelctx.stop();
        }
    }

    private class CountDownInterceptor extends AbstractPhaseInterceptor {
        private final CountDownLatch latch;

        public CountDownInterceptor(CountDownLatch latch) {
            super(Phase.RECEIVE);

            // Verify common interceptors can be added
            getBefore().add(PolicyInInterceptor.class.getName());
            getBefore().add(LoggingInInterceptor.class.getName());
            getBefore().add(AttachmentInInterceptor.class.getName());

            this.latch = latch;
        }

        @Override
        public void handleMessage(Message message) throws Fault {
            this.latch.countDown();
        }
    }
}
