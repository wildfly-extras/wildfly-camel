/*
 * #%L
 * Wildfly Camel :: Testsuite
 * %%
 * Copyright (C) 2013 - 2016 RedHat
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
package org.wildfly.camel.test.metrics;

import java.util.SortedMap;

import com.codahale.metrics.Counter;
import com.codahale.metrics.MetricRegistry;

import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.metrics.messagehistory.MetricsMessageHistoryFactory;
import org.apache.camel.component.metrics.messagehistory.MetricsMessageHistoryService;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.impl.SimpleRegistry;
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
public class MetricsIntegrationTest {

    @Deployment
    public static JavaArchive createDeployment() {
        return ShrinkWrap.create(JavaArchive.class, "camel-metrics-tests.jar");
    }

    @Test
    public void testRouteMetrics() throws Exception {
        SimpleRegistry registry = new SimpleRegistry();
        MetricRegistry metricRegistry = new MetricRegistry();
        registry.put("metricRegistry", metricRegistry);

        CamelContext camelctx = new DefaultCamelContext(registry);
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:start")
                .to("metrics:counter:simple.counter?increment=5");
            }
        });

        try {
            camelctx.start();

            ProducerTemplate template = camelctx.createProducerTemplate();
            template.requestBody("direct:start", "fake body");

            SortedMap<String, Counter> counters = metricRegistry.getCounters();
            Counter counter = null;
            for (String counterName : counters.keySet()) {
                if (counterName.equals("simple.counter")) {
                    counter = counters.get(counterName);
                    break;
                }
            }

            Assert.assertNotNull("Counter simple.counter was null", counter);
            Assert.assertEquals(5, counter.getCount());
        } finally {
            camelctx.stop();
        }
    }

    @Test
    public void testMetricsJsonSerialization() throws Exception {
        SimpleRegistry registry = new SimpleRegistry();
        MetricRegistry metricRegistry = new MetricRegistry();
        registry.put("metricRegistry", metricRegistry);

        MetricsMessageHistoryFactory messageHistoryFactory = new MetricsMessageHistoryFactory();
        messageHistoryFactory.setMetricsRegistry(metricRegistry);

        CamelContext camelctx = new DefaultCamelContext(registry);
        camelctx.setMessageHistoryFactory(messageHistoryFactory);
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:start")
                .to("metrics:counter:simple.counter?increment=5");
            }
        });

        camelctx.start();
        try {
            MetricsMessageHistoryService service = camelctx.hasService(MetricsMessageHistoryService.class);
            String json = service.dumpStatisticsAsJson();
            Assert.assertTrue(json.contains("\"gauges\":{},\"counters\":{},\"histograms\":{},\"meters\":{},\"timers\":{}"));
        } finally {
            camelctx.stop();
        }
    }
}
