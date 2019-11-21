/*
 * #%L
 * Wildfly Camel :: Testsuite
 * %%
 * Copyright (C) 2013 - 2019 RedHat
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
package org.wildfly.camel.test.microprofile.metrics;

import static org.apache.camel.component.microprofile.metrics.MicroProfileMetricsConstants.HEADER_GAUGE_VALUE;
import static org.apache.camel.component.microprofile.metrics.MicroProfileMetricsConstants.HEADER_HISTOGRAM_VALUE;
import static org.apache.camel.component.microprofile.metrics.MicroProfileMetricsConstants.HEADER_METER_MARK;

import io.smallrye.metrics.MetricRegistries;

import java.io.StringReader;

import javax.json.Json;
import javax.json.JsonNumber;
import javax.json.JsonObject;
import javax.json.stream.JsonParser;
import javax.ws.rs.core.MediaType;

import org.apache.camel.CamelContext;
import org.apache.camel.ServiceStatus;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.microprofile.metrics.event.notifier.context.MicroProfileMetricsCamelContextEventNotifier;
import org.apache.camel.component.microprofile.metrics.event.notifier.route.MicroProfileMetricsRouteEventNotifier;
import org.apache.camel.impl.DefaultCamelContext;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.camel.test.common.http.HttpRequest;
import org.wildfly.extension.camel.CamelAware;

@CamelAware
@RunWith(Arquillian.class)
public class MicroProfileMetricsIntegrationTest {

    @Deployment
    public static JavaArchive createDeployment() {
        return ShrinkWrap.create(JavaArchive.class, "camel-microprofile-metrics-tests.jar")
            .addClass(HttpRequest.class);
    }

    @Test
    public void testCounter() throws Exception {
        CamelContext camelctx = new DefaultCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:counter")
                    .to("microprofile-metrics:counter:wfc-counter");
            }
        });
        bindMetricRegistry(camelctx);

        camelctx.start();
        try {
            camelctx.createProducerTemplate().sendBody("direct:counter", null);
            assertSimpleMetricValue(camelctx, "wfc-counter", 1);
        } finally {
            camelctx.stop();
        }
    }

    @Test
    public void testConcurrentGauge() throws Exception {
        CamelContext camelctx = new DefaultCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:concurrentGaugeIncrement")
                    .to("microprofile-metrics:concurrent gauge:wfc-concurrent-gauge?gaugeIncrement=true");

                from("direct:concurrentGaugeDecrement")
                    .to("microprofile-metrics:concurrent gauge:wfc-concurrent-gauge?gaugeDecrement=true");
            }
        });
        bindMetricRegistry(camelctx);

        camelctx.start();
        try {
            camelctx.createProducerTemplate().sendBody("direct:concurrentGaugeIncrement", null);
            assertComplexMetricValue(camelctx, "wfc-concurrent-gauge.current", 1);

            camelctx.createProducerTemplate().sendBody("direct:concurrentGaugeDecrement", null);
            assertComplexMetricValue(camelctx, "wfc-concurrent-gauge.current", 0);
        } finally {
            camelctx.stop();
        }
    }

    @Test
    public void testGauge() throws Exception {
        CamelContext camelctx = new DefaultCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:gauge")
                    .setHeader(HEADER_GAUGE_VALUE, simple("${body}"))
                    .to("microprofile-metrics:gauge:wfc-gauge");
            }
        });
        bindMetricRegistry(camelctx);

        camelctx.start();
        try {
            camelctx.createProducerTemplate().sendBody("direct:gauge", 10);
            assertSimpleMetricValue(camelctx, "wfc-gauge", 10);
        } finally {
            camelctx.stop();
        }
    }

    @Test
    public void testHistogram() throws Exception {
        CamelContext camelctx = new DefaultCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:histogram")
                    .setHeader(HEADER_HISTOGRAM_VALUE, simple("${body}"))
                    .to("microprofile-metrics:histogram:wfc-histogram");
            }
        });
        bindMetricRegistry(camelctx);

        camelctx.start();
        try {
            camelctx.createProducerTemplate().sendBody("direct:histogram", 10);
            assertComplexMetricValue(camelctx, "wfc-histogram.max", 10);
        } finally {
            camelctx.stop();
        }
    }

    @Test
    public void testMeter() throws Exception {
        CamelContext camelctx = new DefaultCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:meter")
                    .setHeader(HEADER_METER_MARK, simple("${body}"))
                    .to("microprofile-metrics:meter:wfc-meter");
            }
        });
        bindMetricRegistry(camelctx);

        camelctx.start();
        try {
            camelctx.createProducerTemplate().sendBody("direct:meter", 10);
            assertComplexMetricValue(camelctx, "wfc-meter.count", 10);
        } finally {
            camelctx.stop();
        }
    }

    @Test
    public void testTimer() throws Exception {
        CamelContext camelctx = new DefaultCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:timer")
                    .to("microprofile-metrics:timer:wfc-timer?action=start")
                    .delay(100)
                    .to("microprofile-metrics:timer:wfc-timer?action=stop");
            }
        });
        bindMetricRegistry(camelctx);

        camelctx.start();
        try {
            camelctx.createProducerTemplate().sendBody("direct:timer", null);
            assertComplexMetricValue(camelctx, "wfc-timer.count", 1);
        } finally {
            camelctx.stop();
        }
    }

    @Test
    public void testMicroProfileMetricsEventNotifiers() throws Exception {
        CamelContext camelctx = new DefaultCamelContext();
        camelctx.getManagementStrategy().addEventNotifier(new MicroProfileMetricsCamelContextEventNotifier());
        camelctx.getManagementStrategy().addEventNotifier(new MicroProfileMetricsRouteEventNotifier());
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:start")
                    .to("log:end");
            }
        });
        bindMetricRegistry(camelctx);

        camelctx.start();
        try {
            assertSimpleMetricValue(camelctx, "camel.context.status", ServiceStatus.Started.ordinal());
            assertSimpleMetricValue(camelctx, "camel.route.running.count", 1);
        } finally {
            camelctx.stop();
        }
    }

    private void bindMetricRegistry(CamelContext camelctx) {
        camelctx.getRegistry().bind("metricRegistry", new MetricRegistries().getApplicationRegistry());
    }

    private void assertSimpleMetricValue(CamelContext camelctx, String metricName, Number metricValue) throws Exception {
        String metrics = getMetrics();

        JsonParser parser = Json.createParser(new StringReader(metrics));
        Assert.assertTrue("Metrics endpoint content is empty", parser.hasNext());
        parser.next();

        JsonNumber jsonNumber = parser.getObject().getJsonNumber(metricName + ";camelContext=" + camelctx.getName());

        Assert.assertEquals("Expected to find metric" + metricName + " with value " + metricValue, metricValue, jsonNumber.intValue());
    }

    private void assertComplexMetricValue(CamelContext camelctx, String metricName, Number metricValue) throws Exception {
        String metrics = getMetrics();

        JsonParser parser = Json.createParser(new StringReader(metrics));
        Assert.assertTrue("Metrics endpoint content is empty", parser.hasNext());
        parser.next();

        JsonObject jsonObject = parser.getObject();
        JsonNumber result = null;
        String[] nameParts = metricName.split("\\.");
        for (int i = 0; i < nameParts.length; i++) {
            String jsonKey = i + 1 == nameParts.length ? nameParts[i] + ";camelContext=" + camelctx.getName() : nameParts[i];

            if (i + 1 == nameParts.length) {
                result = jsonObject.getJsonNumber(jsonKey);
                break;
            } else {
                jsonObject = jsonObject.getJsonObject(jsonKey);
            }
        }

        Assert.assertNotNull("Expected to find metric " + metricName, result);
        Assert.assertEquals("Expected metric " + metricName + " to have value " + metricValue, metricValue, result.intValue());
    }

    private String getMetrics() throws Exception {
        return HttpRequest.get("http://localhost:9990/metrics/application")
            .header("Accept", MediaType.APPLICATION_JSON)
            .getResponse()
            .getBody();
    }
}
