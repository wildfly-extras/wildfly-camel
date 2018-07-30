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

package org.wildfly.camel.test.quartz2;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.quartz2.QuartzComponent;
import org.apache.camel.impl.DefaultCamelContext;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.quartz.Scheduler;
import org.wildfly.extension.camel.CamelAware;

@CamelAware
@RunWith(Arquillian.class)
public class QuartzIntegrationTest {

    @Deployment
    public static JavaArchive deployment() {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "quartz-tests");
        return archive;
    }

    @Test
    public void testScheduler() throws Exception {

        final CountDownLatch procLatch = new CountDownLatch(3);

        ClassLoader loader = QuartzIntegrationTest.class.getClassLoader();
        Class<?> sclass = loader.loadClass("org.quartz.Scheduler");
        Assert.assertNotNull("Scheduler can be loaded", sclass);

        CamelContext camelctx = new DefaultCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("quartz2://mytimer?trigger.repeatCount=3&trigger.repeatInterval=100").process(new Processor() {
                    @Override
                    public void process(Exchange exchange) throws Exception {
                        CamelContext context = exchange.getContext();
                        QuartzComponent comp = context.getComponent("quartz2", QuartzComponent.class);
                        Scheduler scheduler = comp.getScheduler();
                        Assert.assertNotNull("Scheduler not null", scheduler);
                        procLatch.countDown();
                    }
                }).to("mock:result");
            }
        });

        camelctx.start();
        try {
            Assert.assertTrue("ProcLatch is not zero", procLatch.await(5, TimeUnit.SECONDS));
        } finally {
            camelctx.stop();
        }
    }

    @Test
    public void testManualComponentConfig() throws Exception {

        final CountDownLatch startLatch = new CountDownLatch(1);
        final CountDownLatch procLatch = new CountDownLatch(3);

        CamelContext camelctx = new DefaultCamelContext();
        camelctx.addComponent("quartz2", new QuartzComponent() {
            @Override
            public void onCamelContextStarted(CamelContext context, boolean alreadyStarted) throws Exception {
                super.onCamelContextStarted(context, alreadyStarted);
                if (!alreadyStarted) {
                    startLatch.countDown();
                }
            }
        });

        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("quartz2://mytimer?trigger.repeatCount=3&trigger.repeatInterval=100").process(new Processor() {
                    @Override
                    public void process(Exchange exchange) throws Exception {
                        procLatch.countDown();
                    }
                }).to("mock:result");
            }
        });

        camelctx.start();
        try {
            Assert.assertEquals("StartLatch is zero", 0, startLatch.getCount());
            Assert.assertTrue("ProcLatch is not zero", procLatch.await(5, TimeUnit.SECONDS));
        } finally {
            camelctx.stop();
        }
    }
}
