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
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.test.AvailablePortFinder;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.camel.test.common.HttpRequest;
import org.wildfly.camel.test.common.HttpRequest.HttpResponse;
import org.wildfly.camel.test.http4.subA.MyServlet;
import org.wildfly.extension.camel.CamelAware;

@CamelAware
@RunWith(Arquillian.class)
public class UndertowIntegrationTest {

    @Deployment
    public static WebArchive createDeployment() {
        final WebArchive archive = ShrinkWrap.create(WebArchive.class, "camel-undertow.war");
        archive.addClasses(MyServlet.class, AvailablePortFinder.class, HttpRequest.class);
        return archive;
    }

    @Test
    public void testHttpProducer() throws Exception {

        CamelContext camelctx = new DefaultCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:start").to("undertow:http://localhost:8080/camel-undertow/myservlet");
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

    @Test
    public void testHttpConsumer() throws Exception {

        final int port = AvailablePortFinder.getNextAvailable(9080);
        final String httpurl = "http://localhost:" + port + "/myapp/myservice";

        CamelContext camelctx = new DefaultCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("undertow:" + httpurl).process(new Processor() {
                    @Override
                    public void process(Exchange exchange) throws Exception {
                        System.out.println("Processing: " + exchange);
                        Message in = exchange.getIn();
                        in.setBody("Hello " + in.getHeader("name"));
                    }
                }).to("seda:end");
            }
        });

        camelctx.start();
        try {

            final CountDownLatch latch = new CountDownLatch(1);
            Consumer consumer = camelctx.getEndpoint("seda:end").createConsumer(new Processor() {
                @Override
                public void process(Exchange exchange) throws Exception {
                    System.out.println("Processing: " + exchange);
                    Assert.assertEquals("Hello Kermit", exchange.getIn().getBody());
                    latch.countDown();
                }});

            HttpResponse response = HttpRequest.get(httpurl + "?name=Kermit").getResponse();
            Assert.assertEquals(HttpURLConnection.HTTP_OK, response.getStatusCode());

            consumer.start();
            try {
                Assert.assertTrue("Message not processed by consumer", latch.await(3, TimeUnit.SECONDS));
            } finally {
                consumer.stop();
            }
        } finally {
            camelctx.stop();
        }
    }
}
