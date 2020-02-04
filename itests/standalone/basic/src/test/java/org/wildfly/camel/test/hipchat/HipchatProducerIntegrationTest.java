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
package org.wildfly.camel.test.hipchat;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.ExchangePattern;
import org.apache.camel.Message;
import org.apache.camel.Processor;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.hipchat.HipchatConstants;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.http.StatusLine;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.extension.camel.CamelAware;

/**
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 */
@CamelAware
@RunWith(Arquillian.class)
public class HipchatProducerIntegrationTest {

    private HipchatEndpointSupport.PostCallback callback = new HipchatEndpointSupport.PostCallback();

    @Deployment
    public static JavaArchive createDeployment() {
        JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "camel-hipchat-tests.jar");
        archive.addClasses(HipchatComponentSupport.class, HipchatEndpointSupport.class);
        return archive;
    }

    private CamelContext createCamelContext() throws Exception {
        final CamelContext context = new DefaultCamelContext();
        context.addComponent("hipchat", new HipchatComponentSupport(context, callback, null));
        context.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:start").to("hipchat://?authToken=anything").to("mock:result");
            }
        });
        return context;
    }

    @Test
    public void sendInOnly() throws Exception {

        CamelContext camelctx = createCamelContext();

        MockEndpoint result = camelctx.getEndpoint("mock:result", MockEndpoint.class);
        result.expectedMessageCount(1);

        camelctx.start();
        try {
            ProducerTemplate template = camelctx.createProducerTemplate();
            Exchange exchange = template.send("direct:start", ExchangePattern.InOnly, new Processor() {
                public void process(Exchange exchange) throws Exception {
                    exchange.getIn().setHeader(HipchatConstants.TO_ROOM, "CamelUnitTest");
                    exchange.getIn().setHeader(HipchatConstants.TO_USER, "CamelUnitTestUser");
                    exchange.getIn().setBody("This is my unit test message.");
                }
            });

            result.assertIsSatisfied();

            assertCommonResultExchange(result.getExchanges().get(0));
            assertNullExchangeHeader(result.getExchanges().get(0));

            assertResponseMessage(exchange.getIn());
        } finally {
            camelctx.close();
        }

    }

    @Test
    public void sendInOut() throws Exception {
        CamelContext camelctx = createCamelContext();

        MockEndpoint result = camelctx.getEndpoint("mock:result", MockEndpoint.class);
        result.expectedMessageCount(1);

        camelctx.start();
        try {
            ProducerTemplate template = camelctx.createProducerTemplate();
            Exchange exchange = template.send("direct:start", ExchangePattern.InOut, new Processor() {
                public void process(Exchange exchange) throws Exception {
                    exchange.getIn().setHeader(HipchatConstants.TO_ROOM, "CamelUnitTest");
                    exchange.getIn().setHeader(HipchatConstants.TO_USER, "CamelUnitTestUser");
                    exchange.getIn().setHeader(HipchatConstants.MESSAGE_BACKGROUND_COLOR, "CamelUnitTestBkColor");
                    exchange.getIn().setHeader(HipchatConstants.MESSAGE_FORMAT, "CamelUnitTestFormat");
                    exchange.getIn().setHeader(HipchatConstants.TRIGGER_NOTIFY, "CamelUnitTestNotify");
                    exchange.getIn().setBody("This is my unit test message.");
                }
            });

            result.assertIsSatisfied();

            assertCommonResultExchange(result.getExchanges().get(0));
            assertRemainingResultExchange(result.getExchanges().get(0));

            assertResponseMessage(exchange.getIn());
        } finally {
            camelctx.close();
        }

    }

    @Test
    public void sendInOutRoomOnly() throws Exception {
        CamelContext camelctx = createCamelContext();

        MockEndpoint result = camelctx.getEndpoint("mock:result", MockEndpoint.class);
        result.expectedMessageCount(1);

        camelctx.start();
        try {
            ProducerTemplate template = camelctx.createProducerTemplate();
            Exchange exchange = template.send("direct:start", ExchangePattern.InOut, new Processor() {
                public void process(Exchange exchange) throws Exception {
                    exchange.getIn().setHeader(HipchatConstants.TO_ROOM, "CamelUnitTest");
                    exchange.getIn().setHeader(HipchatConstants.MESSAGE_BACKGROUND_COLOR, "CamelUnitTestBkColor");
                    exchange.getIn().setHeader(HipchatConstants.MESSAGE_FORMAT, "CamelUnitTestFormat");
                    exchange.getIn().setHeader(HipchatConstants.TRIGGER_NOTIFY, "CamelUnitTestNotify");
                    exchange.getIn().setBody("This is my unit test message.");
                }
            });

            result.assertIsSatisfied();

            Exchange resultExchange = result.getExchanges().get(0);
            HipchatEndpointSupport.assertIsInstanceOf(String.class, resultExchange.getIn().getBody());
            Assert.assertEquals("This is my unit test message.", resultExchange.getIn().getBody(String.class));
            Assert.assertEquals("CamelUnitTest", resultExchange.getIn().getHeader(HipchatConstants.TO_ROOM));
            Assert.assertNull(resultExchange.getIn().getHeader(HipchatConstants.TO_USER));
            Assert.assertNull(resultExchange.getIn().getHeader(HipchatConstants.TO_USER_RESPONSE_STATUS));
            Assert.assertNotNull(resultExchange.getIn().getHeader(HipchatConstants.TO_ROOM_RESPONSE_STATUS));

            assertRemainingResultExchange(result.getExchanges().get(0));

            Assert.assertEquals(204, exchange.getIn()
                    .getHeader(HipchatConstants.TO_ROOM_RESPONSE_STATUS, StatusLine.class).getStatusCode());
            Assert.assertNotNull(callback);
            Assert.assertNotNull(callback.called);
            Assert.assertEquals("This is my unit test message.", callback.called.get("message"));
            Assert.assertEquals("CamelUnitTestBkColor", callback.called.get("color"));
            Assert.assertEquals("CamelUnitTestFormat", callback.called.get("message_format"));
            Assert.assertEquals("CamelUnitTestNotify", callback.called.get("notify"));
        } finally {
            camelctx.close();
        }
    }

    @Test
    public void sendInOutUserOnly() throws Exception {
        CamelContext camelctx = createCamelContext();

        MockEndpoint result = camelctx.getEndpoint("mock:result", MockEndpoint.class);
        result.expectedMessageCount(1);

        camelctx.start();
        try {
            ProducerTemplate template = camelctx.createProducerTemplate();

            Exchange exchange = template.send("direct:start", ExchangePattern.InOut, new Processor() {
                public void process(Exchange exchange) throws Exception {
                    exchange.getIn().setHeader(HipchatConstants.TO_USER, "CamelUnitTest");
                    exchange.getIn().setHeader(HipchatConstants.MESSAGE_BACKGROUND_COLOR, "CamelUnitTestBkColor");
                    exchange.getIn().setHeader(HipchatConstants.MESSAGE_FORMAT, "CamelUnitTestFormat");
                    exchange.getIn().setHeader(HipchatConstants.TRIGGER_NOTIFY, "CamelUnitTestNotify");
                    exchange.getIn().setBody("This is my unit test message.");
                }
            });

            result.assertIsSatisfied();

            Exchange resultExchange = result.getExchanges().get(0);
            HipchatEndpointSupport.assertIsInstanceOf(String.class, resultExchange.getIn().getBody());
            Assert.assertEquals("This is my unit test message.", resultExchange.getIn().getBody(String.class));
            Assert.assertEquals("CamelUnitTest", resultExchange.getIn().getHeader(HipchatConstants.TO_USER));
            Assert.assertNull(resultExchange.getIn().getHeader(HipchatConstants.TO_ROOM));
            Assert.assertNull(resultExchange.getIn().getHeader(HipchatConstants.TO_ROOM_RESPONSE_STATUS));
            Assert.assertNotNull(resultExchange.getIn().getHeader(HipchatConstants.TO_USER_RESPONSE_STATUS));

            assertRemainingResultExchange(result.getExchanges().get(0));

            Assert.assertEquals(204, exchange.getIn()
                    .getHeader(HipchatConstants.TO_USER_RESPONSE_STATUS, StatusLine.class).getStatusCode());
            Assert.assertNotNull(callback);
            Assert.assertNotNull(callback.called);
            Assert.assertEquals("This is my unit test message.", callback.called.get("message"));
            Assert.assertNull(callback.called.get("color"));
            Assert.assertEquals("CamelUnitTestFormat", callback.called.get("message_format"));
            Assert.assertEquals("CamelUnitTestNotify", callback.called.get("notify"));
        } finally {
            camelctx.close();
        }
    }

    private static void assertRemainingResultExchange(Exchange resultExchange) {
        Assert.assertEquals("CamelUnitTestBkColor",
                resultExchange.getIn().getHeader(HipchatConstants.MESSAGE_BACKGROUND_COLOR));
        Assert.assertEquals("CamelUnitTestFormat", resultExchange.getIn().getHeader(HipchatConstants.MESSAGE_FORMAT));
        Assert.assertEquals("CamelUnitTestNotify", resultExchange.getIn().getHeader(HipchatConstants.TRIGGER_NOTIFY));
    }

    private void assertCommonResultExchange(Exchange resultExchange) {
        HipchatEndpointSupport.assertIsInstanceOf(String.class, resultExchange.getIn().getBody());
        Assert.assertEquals("This is my unit test message.", resultExchange.getIn().getBody(String.class));
        Assert.assertEquals("CamelUnitTest", resultExchange.getIn().getHeader(HipchatConstants.TO_ROOM));
        Assert.assertEquals("CamelUnitTestUser", resultExchange.getIn().getHeader(HipchatConstants.TO_USER));
        Assert.assertNotNull(resultExchange.getIn().getHeader(HipchatConstants.TO_USER_RESPONSE_STATUS));
        Assert.assertNotNull(resultExchange.getIn().getHeader(HipchatConstants.TO_ROOM_RESPONSE_STATUS));
    }

    private void assertNullExchangeHeader(Exchange resultExchange) {
        Assert.assertNull(resultExchange.getIn().getHeader(HipchatConstants.FROM_USER));
        Assert.assertNull(resultExchange.getIn().getHeader(HipchatConstants.MESSAGE_BACKGROUND_COLOR));
        Assert.assertNull(resultExchange.getIn().getHeader(HipchatConstants.MESSAGE_FORMAT));
        Assert.assertNull(resultExchange.getIn().getHeader(HipchatConstants.TRIGGER_NOTIFY));
    }

    private void assertResponseMessage(Message message) {
        Assert.assertEquals(204,
                message.getHeader(HipchatConstants.TO_ROOM_RESPONSE_STATUS, StatusLine.class).getStatusCode());
        Assert.assertEquals(204,
                message.getHeader(HipchatConstants.TO_USER_RESPONSE_STATUS, StatusLine.class).getStatusCode());
    }
}
