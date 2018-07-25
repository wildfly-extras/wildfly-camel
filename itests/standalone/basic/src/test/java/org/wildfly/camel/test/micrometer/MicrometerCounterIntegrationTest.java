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

import static org.apache.camel.component.micrometer.MicrometerConstants.HEADER_COUNTER_DECREMENT;
import static org.apache.camel.component.micrometer.MicrometerConstants.HEADER_COUNTER_INCREMENT;
import static org.apache.camel.component.micrometer.MicrometerConstants.HEADER_METRIC_NAME;
import static org.apache.camel.component.micrometer.MicrometerConstants.METRICS_REGISTRY_NAME;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.impl.DefaultCamelContext;
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
public class MicrometerCounterIntegrationTest {

    @ArquillianResource
    private InitialContext context;

    private SimpleMeterRegistry metricsRegistry;

    @Deployment
    public static JavaArchive createDeployment() {
        return ShrinkWrap.create(JavaArchive.class, "camel-micrometer-counter-tests.jar");
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
            MockEndpoint mockEndpoint = camelctx.getEndpoint("mock:out", MockEndpoint.class);
            mockEndpoint.expectedMessageCount(1);

            ProducerTemplate template = camelctx.createProducerTemplate();
            template.sendBodyAndHeader("direct:in-1", null, HEADER_METRIC_NAME, "A1");

            Assert.assertEquals(5.0D, metricsRegistry.find("A1").counter().count(), 0.01D);
            mockEndpoint.assertIsSatisfied();
        } finally {
            camelctx.stop();
        }
    }

    @Test
    public void testOverrideIncrement() throws Exception {
        CamelContext camelctx = createCamelContext();
        camelctx.start();
        try {
            MockEndpoint mockEndpoint = camelctx.getEndpoint("mock:out", MockEndpoint.class);
            mockEndpoint.expectedMessageCount(1);

            ProducerTemplate template = camelctx.createProducerTemplate();
            template.sendBodyAndHeader("direct:in-1", null, HEADER_COUNTER_INCREMENT, 14.0D);

            Assert.assertEquals(14.0D, metricsRegistry.find("A").counter().count(), 0.01D);
            mockEndpoint.assertIsSatisfied();
        } finally {
            camelctx.stop();
        }
    }

    @Test
    public void testOverrideDecrement() throws Exception {
        CamelContext camelctx = createCamelContext();
        camelctx.start();
        try {
            MockEndpoint mockEndpoint = camelctx.getEndpoint("mock:out", MockEndpoint.class);
            mockEndpoint.expectedMessageCount(1);

            ProducerTemplate template = camelctx.createProducerTemplate();
            template.sendBodyAndHeader("direct:in-2", null, HEADER_COUNTER_DECREMENT, 7.0D);

            Assert.assertEquals(-7.0D, metricsRegistry.find("B").counter().count(), 0.01D);
            mockEndpoint.assertIsSatisfied();
        } finally {
            camelctx.stop();
        }
    }

    @Test
    public void testOverrideUsingConstantValue() throws Exception {
        CamelContext camelctx = createCamelContext();
        camelctx.start();
        try {
            MockEndpoint mockEndpoint = camelctx.getEndpoint("mock:out", MockEndpoint.class);
            mockEndpoint.expectedMessageCount(1);

            ProducerTemplate template = camelctx.createProducerTemplate();
            template.sendBodyAndHeader("direct:in-3", null, HEADER_COUNTER_DECREMENT, 7.0D);

            Assert.assertEquals(417.0D, metricsRegistry.find("C").counter().count(), 0.01D);
            mockEndpoint.assertIsSatisfied();
        } finally {
            camelctx.stop();
        }
    }

    @Test
    public void testUsingScriptEvaluation() throws Exception {
        CamelContext camelctx = createCamelContext();
        camelctx.start();
        try {
            MockEndpoint mockEndpoint = camelctx.getEndpoint("mock:out", MockEndpoint.class);
            mockEndpoint.expectedMessageCount(1);

            String message = "Hello from Camel Metrics!";
            ProducerTemplate template = camelctx.createProducerTemplate();
            template.sendBodyAndHeader("direct:in-4", message, HEADER_COUNTER_DECREMENT, 7.0D);

            Counter counter = metricsRegistry.find("D").counter();
            Assert.assertEquals(message.length(), counter.count(), 0.01D);
            Assert.assertEquals(Integer.toString(message.length()), counter.getId().getTag("a"));
            mockEndpoint.assertIsSatisfied();
        } finally {
            camelctx.stop();
        }
    }

    private CamelContext createCamelContext() throws Exception {
        CamelContext camelctx = new DefaultCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:in-1")
                    .to("micrometer:counter:A?increment=5")
                    .to("mock:out");

                from("direct:in-2")
                    .to("micrometer:counter:B?decrement=9")
                    .to("mock:out");

                from("direct:in-3")
                    .setHeader(HEADER_COUNTER_INCREMENT, constant(417L))
                    .to("micrometer:counter:C")
                    .to("mock:out");

                from("direct:in-4")
                    .to("micrometer:counter:D?increment=${body.length}&tags=a=${body.length}")
                    .to("mock:out");
            }
        });
        return camelctx;
    }
}
