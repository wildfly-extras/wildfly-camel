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

package org.wildfly.camel.test.ahc;

import java.util.concurrent.Future;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.impl.DefaultCamelContext;
import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.DefaultAsyncHttpClient;
import org.asynchttpclient.Response;
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
public class AhcIntegrationTest {

    private static final String HTTP_URL = "http://localhost:8080";

    @Deployment
    public static JavaArchive createdeployment() {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "camel-ahc-test.jar");
        return archive;
    }

    @Test
    public void testAsyncHttpClient() throws Exception {

        try (AsyncHttpClient client = new DefaultAsyncHttpClient()) {
            Future<Response> f = client.prepareGet(HTTP_URL).execute();
            Response res = f.get();
            Assert.assertEquals(200, res.getStatusCode());
            final String body = res.getResponseBody();
            Assert.assertTrue("Got body " + body, body.contains("Welcome to WildFly"));
        }
    }

    @Test
    public void testAsyncHttpRoute() throws Exception {

        CamelContext camelctx = new DefaultCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:start")
                .to("ahc:" + HTTP_URL)
                .to("mock:results");
            }
        });

        MockEndpoint mockep = camelctx.getEndpoint("mock:results", MockEndpoint.class);
        mockep.setExpectedCount(1);

        camelctx.start();
        try {
            ProducerTemplate producer = camelctx.createProducerTemplate();
            producer.sendBody("direct:start", null);

            mockep.assertIsSatisfied();
            Message message = mockep.getExchanges().get(0).getIn();
            Assert.assertEquals(200, (int) message.getHeader(Exchange.HTTP_RESPONSE_CODE, Integer.class));
            final String body = message.getBody(String.class);
            Assert.assertTrue("Got body " + body, body.contains("Welcome to WildFly"));

        } finally {
            camelctx.stop();
        }
    }
}
