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

import java.net.HttpURLConnection;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.camel.CamelContext;
import org.apache.camel.Consumer;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.impl.DefaultCamelContext;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.camel.test.common.http.HttpRequest;
import org.wildfly.camel.test.common.http.HttpRequest.HttpResponse;
import org.wildfly.camel.test.http4.subA.MyServlet;

@RunWith(Arquillian.class)
public class UndertowConsumerIntegrationTest {

    @ArquillianResource
    ManagementClient managementClient;

    @Deployment
    public static WebArchive createDeployment() {
        final WebArchive archive = ShrinkWrap.create(WebArchive.class, "undertow-consumer.war");
        archive.addClasses(MyServlet.class, HttpRequest.class);
        return archive;
    }

    @Test
    public void testHttpConsumer() throws Exception {

        CamelContext camelctx = new DefaultCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("undertow:http://localhost/myapp/serviceA").process(new Processor() {
                    @Override
                    public void process(Exchange exchange) throws Exception {
                        Message in = exchange.getIn();
                        in.setBody("Hello " + in.getHeader("name"));
                    }
                }).to("seda:endA");
                from("undertow:http://localhost/myapp/serviceB").process(new Processor() {
                    @Override
                    public void process(Exchange exchange) throws Exception {
                        Message in = exchange.getIn();
                        in.setBody("Hello " + in.getHeader("name"));
                    }
                }).to("seda:endB");
            }
        });

        camelctx.start();
        try {

            CountDownProcessor procA = new CountDownProcessor(1);
            Consumer consumerA = camelctx.getEndpoint("seda:endA").createConsumer(procA);

            CountDownProcessor procB = new CountDownProcessor(1);
            Consumer consumerB = camelctx.getEndpoint("seda:endB").createConsumer(procB);

            HttpResponse response = HttpRequest.get("http://localhost:8080/myapp/serviceA?name=Kermit").getResponse();
            Assert.assertEquals(HttpURLConnection.HTTP_OK, response.getStatusCode());

            response = HttpRequest.get("http://localhost:8080/myapp/serviceB?name=Piggy").getResponse();
            Assert.assertEquals(HttpURLConnection.HTTP_OK, response.getStatusCode());

            consumerA.start();
            try {
                Assert.assertTrue("Message not processed by consumer", procA.await(3, TimeUnit.SECONDS));
                Assert.assertEquals("Hello Kermit", procA.getExchange().getIn().getBody(String.class));
            } finally {
                consumerA.stop();
            }

            consumerB.start();
            try {
                Assert.assertTrue("Message not processed by consumer", procB.await(3, TimeUnit.SECONDS));
                Assert.assertEquals("Hello Piggy", procB.getExchange().getIn().getBody(String.class));
            } finally {
                consumerB.stop();
            }
        } finally {
            camelctx.stop();
        }
    }

    class CountDownProcessor implements Processor {

        private final CountDownLatch latch;
        private Exchange exchange;

        CountDownProcessor(int count) {
            latch = new CountDownLatch(count);
        }

        @Override
        public synchronized void process(Exchange exchange) throws Exception {
            this.exchange = exchange;
            latch.countDown();
        }

        synchronized Exchange getExchange() {
            return exchange;
        }

        boolean await(long timeout, TimeUnit unit) throws InterruptedException {
            return latch.await(timeout, unit);
        }
    }
}
