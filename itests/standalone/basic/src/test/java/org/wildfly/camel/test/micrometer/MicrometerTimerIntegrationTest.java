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
package org.wildfly.camel.test.micrometer;

import static org.apache.camel.component.micrometer.MicrometerConstants.HEADER_METRIC_NAME;
import static org.apache.camel.component.micrometer.MicrometerConstants.HEADER_TIMER_ACTION;
import static org.apache.camel.component.micrometer.MicrometerConstants.METRICS_REGISTRY_NAME;

import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

import java.util.concurrent.TimeUnit;

import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.micrometer.MicrometerTimerAction;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.support.jndi.JndiBeanRepository;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.extension.camel.CamelAware;

@CamelAware
@RunWith(Arquillian.class)
public class MicrometerTimerIntegrationTest {

    private static final long DELAY = 20L;

    @ArquillianResource
    private InitialContext context;

    private SimpleMeterRegistry metricsRegistry;

    @Deployment
    public static JavaArchive createDeployment() {
        return ShrinkWrap.create(JavaArchive.class, "camel-micrometer-timer-tests.jar");
    }

    @Before
    public void setUp() throws NamingException {
        metricsRegistry = new SimpleMeterRegistry();
        context.bind(METRICS_REGISTRY_NAME, metricsRegistry);
    }

    @After
    public void tearDown() {
        try {
            context.unbind(METRICS_REGISTRY_NAME);
        } catch (NamingException e) {
            // Ignored
        }
    }

    @Test
    public void testOverrideMetricsName() throws Exception {
        CamelContext camelctx = createCamelContext();
        camelctx.start();
        try {
            Object body = new Object();

            MockEndpoint mockEndpoint = camelctx.getEndpoint("mock:out", MockEndpoint.class);
            mockEndpoint.expectedBodiesReceived(body);

            ProducerTemplate template = camelctx.createProducerTemplate();
            template.sendBody("direct:in-1", body);

            Timer timer = metricsRegistry.find("B").timer();
            Assert.assertEquals(1L, timer.count());
            Assert.assertTrue(timer.max(TimeUnit.MILLISECONDS) > 0.0D);
            mockEndpoint.assertIsSatisfied();
        } finally {
            camelctx.stop();
        }
    }


    @Test
    public void testOverrideNoAction() throws Exception {
        CamelContext camelctx = createCamelContext();
        camelctx.start();
        try {
            Object body = new Object();

            MockEndpoint mockEndpoint = camelctx.getEndpoint("mock:out", MockEndpoint.class);
            mockEndpoint.expectedBodiesReceived(body);

            ProducerTemplate template = camelctx.createProducerTemplate();
            template.sendBody("direct:in-2", body);

            Timer timer = metricsRegistry.find("A").timer();
            Assert.assertEquals(1L, timer.count());
            Assert.assertTrue(timer.max(TimeUnit.MILLISECONDS) > 0.0D);
            mockEndpoint.assertIsSatisfied();
        } finally {
            camelctx.stop();
        }
    }

    @Test
    public void testNormal() throws Exception {
        CamelContext camelctx = createCamelContext();
        camelctx.start();
        try {
            int count = 10;
            String body = "hello";

            MockEndpoint mockEndpoint = camelctx.getEndpoint("mock:out", MockEndpoint.class);
            mockEndpoint.expectedMessageCount(count);

            ProducerTemplate template = camelctx.createProducerTemplate();
            for (int i = 0; i < count; i++) {
                template.sendBody("direct:in-3", body);
            }

            Timer timer = metricsRegistry.find("C").timer();
            Assert.assertEquals(count, timer.count());
            Assert.assertTrue(timer.max(TimeUnit.MILLISECONDS) > DELAY);
            Assert.assertTrue(timer.mean(TimeUnit.MILLISECONDS) > DELAY);
            Assert.assertTrue(timer.totalTime(TimeUnit.MILLISECONDS) > DELAY * count);
            Assert.assertEquals(body, timer.getId().getTag("a"));
            mockEndpoint.assertIsSatisfied();
        } finally {
            camelctx.stop();
        }
    }

    private CamelContext createCamelContext() throws Exception {
        CamelContext camelctx = new DefaultCamelContext(new JndiBeanRepository());
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:in-1")
                    .setHeader(HEADER_METRIC_NAME, constant("B"))
                    .to("micrometer:timer:A?action=start")
                    .delay(DELAY)
                    .setHeader(HEADER_METRIC_NAME, constant("B"))
                    .to("micrometer:timer:A?action=stop")
                    .to("mock:out");

                from("direct:in-2")
                    .setHeader(HEADER_TIMER_ACTION, constant(MicrometerTimerAction.start))
                    .to("micrometer:timer:A")
                    .delay(DELAY)
                    .setHeader(HEADER_TIMER_ACTION, constant(MicrometerTimerAction.stop))
                    .to("micrometer:timer:A")
                    .to("mock:out");

                from("direct:in-3")
                    .to("micrometer:timer:C?action=start")
                    .delay(DELAY)
                    .to("micrometer:timer:C?action=stop&tags=a=${body}")
                    .to("mock:out");
            }
        });
        return camelctx;
    }
}
