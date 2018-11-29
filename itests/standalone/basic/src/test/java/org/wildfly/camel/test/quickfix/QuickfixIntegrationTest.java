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
package org.wildfly.camel.test.quickfix;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.camel.CamelContext;
import org.apache.camel.Endpoint;
import org.apache.camel.Exchange;
import org.apache.camel.ExchangePattern;
import org.apache.camel.Producer;
import org.apache.camel.builder.PredicateBuilder;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.quickfixj.QuickfixjEndpoint;
import org.apache.camel.component.quickfixj.QuickfixjEventCategory;
import org.apache.camel.impl.DefaultCamelContext;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.camel.test.quickfix.subA.CountDownLatchDecrementer;
import org.wildfly.camel.test.quickfix.subA.QuickfixjMessageJsonPrinter;
import org.wildfly.extension.camel.CamelAware;

import quickfix.field.EmailThreadID;
import quickfix.field.EmailType;
import quickfix.field.MsgType;
import quickfix.field.Subject;
import quickfix.field.Text;
import quickfix.fix42.Email;

@CamelAware
@RunWith(Arquillian.class)
public class QuickfixIntegrationTest {

    @Deployment
    public static JavaArchive createdeployment() {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "camel-quickfix-tests");
        archive.addPackage(QuickfixjMessageJsonPrinter.class.getPackage());
        archive.addAsResource("quickfix/inprocess.cfg");
        return archive;
    }

    @Test
    public void sendMessage() throws Exception {

        final CountDownLatch logonLatch = new CountDownLatch(2);
        final CountDownLatch receivedMessageLatch = new CountDownLatch(1);

        CamelContext camelctx = new DefaultCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                // Release latch when session logon events are received
                // We expect two events, one for the trader session and one for the market session
                from("quickfix:quickfix/inprocess.cfg").
                    filter(header(QuickfixjEndpoint.EVENT_CATEGORY_KEY).isEqualTo(QuickfixjEventCategory.SessionLogon)).
                    bean(new CountDownLatchDecrementer("logon", logonLatch));

                // For all received messages, print the JSON-formatted message to stdout
                from("quickfix:quickfix/inprocess.cfg").
                    filter(PredicateBuilder.or(
                            header(QuickfixjEndpoint.EVENT_CATEGORY_KEY).isEqualTo(QuickfixjEventCategory.AdminMessageReceived),
                            header(QuickfixjEndpoint.EVENT_CATEGORY_KEY).isEqualTo(QuickfixjEventCategory.AppMessageReceived))).
                    bean(new QuickfixjMessageJsonPrinter());

                // If the market session receives an email then release the latch
                from("quickfix:quickfix/inprocess.cfg?sessionID=FIX.4.2:MARKET->TRADER").
                    filter(header(QuickfixjEndpoint.MESSAGE_TYPE_KEY).isEqualTo(MsgType.EMAIL)).
                    bean(new CountDownLatchDecrementer("message", receivedMessageLatch));
            }
        });

        camelctx.start();
        try {
            Assert.assertTrue("Logon succeed", logonLatch.await(5L, TimeUnit.SECONDS));

            String marketUri = "quickfix:quickfix/inprocess.cfg?sessionID=FIX.4.2:TRADER->MARKET";
            Endpoint endpoint = camelctx.getEndpoint(marketUri);
			Producer producer = endpoint.createProducer();

            Email email = createEmailMessage("Example");
            Exchange exchange = endpoint.createExchange(ExchangePattern.InOnly);
            exchange.getIn().setBody(email);
            producer.process(exchange);

            Assert.assertTrue("Message reached market", receivedMessageLatch.await(5L, TimeUnit.SECONDS));
        } finally {
            camelctx.stop();
        }
    }

    private Email createEmailMessage(String subject) {
        Email email = new Email(new EmailThreadID("ID"), new EmailType(EmailType.NEW), new Subject(subject));
        Email.LinesOfText text = new Email.LinesOfText();
        text.set(new Text("Content"));
        email.addGroup(text);
        return email;
    }

}
