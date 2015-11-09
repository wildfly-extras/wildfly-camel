package org.wildfly.camel.test.metrics;

import java.util.SortedMap;

import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
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

import com.codahale.metrics.Counter;
import com.codahale.metrics.MetricRegistry;

@CamelAware
@RunWith(Arquillian.class)
public class MetricsIntegrationTest {

    @Deployment
    public static JavaArchive createDeployment() {
        return ShrinkWrap.create(JavaArchive.class, "came-metrics-tests.jar");
    }

    @Test
    public void testRouteMetrics() throws Exception {
        SimpleRegistry registry = new SimpleRegistry();
        MetricRegistry metricRegistry = new MetricRegistry();
        registry.put("metricRegistry", metricRegistry);

        CamelContext camelctx = new DefaultCamelContext(registry);
        try {
            camelctx.addRoutes(new RouteBuilder() {
                @Override
                public void configure() throws Exception {
                    from("direct:start")
                    .to("metrics:counter:simple.counter?increment=5");
                }
            });

            camelctx.start();

            ProducerTemplate template = camelctx.createProducerTemplate();
            template.requestBody("direct:start", "fake body");

            SortedMap<String, Counter> counters = metricRegistry.getCounters();
            Counter counter = null;
            for(String counterName : counters.keySet()) {
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
}
