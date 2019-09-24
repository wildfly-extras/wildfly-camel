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
package org.wildfly.camel.test.guava.eventbus;

import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.extension.camel.CamelAware;
import org.wildfly.extension.camel.WildFlyCamelContext;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;

@CamelAware
@RunWith(Arquillian.class)
public class GuavaEventIntegrationTest {

    EventBus eventBus = new EventBus();

    Object receivedEvent;

    @Deployment
    public static JavaArchive deployment() {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "guava-eventbus-tests");
        return archive;
    }

    @Test
    public void shouldForwardMessageToCamel() throws Exception {

        WildFlyCamelContext camelctx = new WildFlyCamelContext();
        camelctx.getNamingContext().bind("eventBusA", eventBus);

        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("guava-eventbus:eventBusA").to("mock:allEvents");
            }
        });

        camelctx.start();
        try {
            String message = "message";
            eventBus.post(message);

            MockEndpoint mockAll = camelctx.getEndpoint("mock:allEvents", MockEndpoint.class);
            mockAll.setExpectedMessageCount(1);
            mockAll.assertIsSatisfied();
            Assert.assertEquals(message, mockAll.getExchanges().get(0).getIn().getBody());
        } finally {
            camelctx.close();
        }
    }

    @Test
    public void shouldReceiveMessageFromCamel() throws Exception {

        WildFlyCamelContext camelctx = new WildFlyCamelContext();
        camelctx.getNamingContext().bind("eventBusB", eventBus);
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:start").to("guava-eventbus:eventBusB");
            }
        });

        camelctx.start();
        try {
            String message = "message";
            eventBus.register(this);

            ProducerTemplate producer = camelctx.createProducerTemplate();
            producer.sendBody("direct:start", message);
            Assert.assertEquals(message, receivedEvent);
        } finally {
            camelctx.close();
        }
    }

    @Subscribe
    public void receiveEvent(Object event) {
        this.receivedEvent = event;
    }
}
