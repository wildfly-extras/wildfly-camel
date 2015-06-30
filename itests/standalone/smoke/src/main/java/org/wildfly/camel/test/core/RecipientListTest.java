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

package org.wildfly.camel.test.core;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.model.language.SimpleExpression;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.camel.test.core.subA.RecipientListBean;

@RunWith(Arquillian.class)
public class RecipientListTest {

    @Deployment
    public static JavaArchive deployment() {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "recipient-tests");
        archive.addClasses(RecipientListBean.class);
        return archive;
    }

    @Test
    public void testSimpleRecipientList() throws Exception {

        final CountDownLatch latch = new CountDownLatch(2);

        DefaultCamelContext camelctx = new DefaultCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:start").recipientList(new SimpleExpression("direct:one, direct:two"), ",");
                from("direct:one").process(new Processor() {
                    @Override
                    public void process(Exchange exchange) throws Exception {
                        latch.countDown();
                    }
                });
                from("direct:two").process(new Processor() {
                    @Override
                    public void process(Exchange exchange) throws Exception {
                        latch.countDown();
                    }
                });
            }
        });

        camelctx.start();
        try {
            ProducerTemplate producer = camelctx.createProducerTemplate();
            String result = producer.requestBody("direct:start", "John,Doe", String.class);
            Assert.assertTrue("Latch reached zerro", latch.await(100, TimeUnit.MILLISECONDS));
            Assert.assertEquals("John,Doe", result);
        } finally {
            camelctx.stop();
        }
    }

    @Test
    public void testRecipientListAnnotation() throws Exception {

        final CountDownLatch latch = new CountDownLatch(2);

        DefaultCamelContext camelctx = new DefaultCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:start").bean(RecipientListBean.class);
                from("direct:one").process(new Processor() {
                    @Override
                    public void process(Exchange exchange) throws Exception {
                        latch.countDown();
                    }
                });
                from("direct:two").process(new Processor() {
                    @Override
                    public void process(Exchange exchange) throws Exception {
                        latch.countDown();
                    }
                });
            }
        });

        camelctx.start();
        try {
            ProducerTemplate producer = camelctx.createProducerTemplate();
            String result = producer.requestBody("direct:start", "John,Doe", String.class);
            Assert.assertTrue("Latch reached zerro", latch.await(100, TimeUnit.MILLISECONDS));
            Assert.assertEquals("John,Doe", result);
        } finally {
            camelctx.stop();
        }
    }
}
