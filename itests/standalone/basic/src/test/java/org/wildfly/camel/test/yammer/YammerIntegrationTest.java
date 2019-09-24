/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.wildfly.camel.test.yammer;


import java.io.InputStream;
import java.util.Collection;

import org.apache.camel.CamelContext;
import org.apache.camel.Endpoint;
import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.component.yammer.ApiRequestor;
import org.apache.camel.component.yammer.YammerEndpoint;
import org.apache.camel.component.yammer.model.Messages;
import org.apache.camel.impl.DefaultCamelContext;
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
public class YammerIntegrationTest {

    @Deployment
    public static JavaArchive createDeployment() {
        JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "camel-yammer-tests");
        archive.addAsResource("yammer/messages.json");
        return archive;
    }

    @Test
    public void testConsumeAllMessages() throws Exception {

        CamelContext camelctx = new DefaultCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                // using dummy keys here since we are mocking out calls to yammer.com with static json; in a real app, please use your own keys!
                from("yammer:messages?consumerKey=aConsumerKey&consumerSecret=aConsumerSecretKey&accessToken=aAccessToken").to("mock:result");
            }
        });

        camelctx.start();
        try {
            InputStream is = getClass().getResourceAsStream("/yammer/messages.json");
            String messages = camelctx.getTypeConverter().convertTo(String.class, is);

            Collection<Endpoint> endpoints = camelctx.getEndpoints();
            for (Endpoint endpoint : endpoints) {
                if (endpoint instanceof YammerEndpoint) {
                    ((YammerEndpoint)endpoint).getConfig().setRequestor(new TestApiRequestor(messages));
                }
            }

            MockEndpoint mock = camelctx.getEndpoint("mock:result", MockEndpoint.class);
            mock.expectedMinimumMessageCount(1);
            mock.assertIsSatisfied();

            Exchange exchange = mock.getExchanges().get(0);
            Messages response = exchange.getIn().getBody(Messages.class);

            Assert.assertEquals(2, response.getMessages().size());
            Assert.assertEquals("Testing yammer API...", response.getMessages().get(0).getBody().getPlain());
            Assert.assertEquals("(Principal Software Engineer) has #joined the redhat.com network. Take a moment to welcome Jonathan.", response.getMessages().get(1).getBody().getPlain());
        } finally {
            camelctx.close();
        }

    }

    static class TestApiRequestor implements ApiRequestor {

        String body;

        public TestApiRequestor(String body) {
            this.body = body;
        }

        private String send() {
            return body;
        }

        @Override
        public String get() throws Exception {
            return send();
        }

        @Override
        public String post(String params) throws Exception {
            return send();
        }
    }
}
