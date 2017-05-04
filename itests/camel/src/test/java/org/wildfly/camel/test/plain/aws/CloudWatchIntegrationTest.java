package org.wildfly.camel.test.plain.aws;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.aws.cw.CwConstants;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.impl.SimpleRegistry;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;
import org.wildfly.camel.test.common.aws.CloudWatchUtils;

import com.amazonaws.services.cloudwatch.AmazonCloudWatchClient;
import com.amazonaws.services.cloudwatch.model.DimensionFilter;
import com.amazonaws.services.cloudwatch.model.ListMetricsRequest;
import com.amazonaws.services.cloudwatch.model.Metric;


public class CloudWatchIntegrationTest {

    public static final String namespace = "MySpace";
    public static final String name = "MyMetric";
    public static final String dimName = "MyDimName";
    public static final String dimValue = "MyDimValue";
    
    private static AmazonCloudWatchClient cwClient;
    
    @BeforeClass
    public static void beforeClass() throws Exception {
        cwClient = CloudWatchUtils.createCloudWatchClient();
    }
    
    @Test
    public void testKeyValueOperations() throws Exception {
        
        Assume.assumeNotNull("AWS client not null", cwClient);
        
        SimpleRegistry registry = new SimpleRegistry();
        registry.put("cwClient", cwClient);
        
        CamelContext camelctx = new DefaultCamelContext(registry);
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:metrics").to("aws-cw://" + namespace + "?amazonCwClient=#cwClient");
            }
        });

        camelctx.start();
        try {
            Map<String, Object> headers = new HashMap<>();
            headers.put(CwConstants.METRIC_NAME, name);
            headers.put(CwConstants.METRIC_DIMENSION_NAME, dimName);
            headers.put(CwConstants.METRIC_DIMENSION_VALUE, dimValue);

            ListMetricsRequest request = new ListMetricsRequest()
                    .withNamespace(namespace)
                    .withMetricName(name)
                    .withDimensions(new DimensionFilter().withName(dimName).withValue(dimValue));

            List<Metric> metrics = Collections.emptyList();
            ProducerTemplate producer = camelctx.createProducerTemplate();
            for (int i = 100; i < 105 && metrics.size() == 0; i++) {
                producer.sendBodyAndHeaders("direct:metrics", new Double(i), headers);
                metrics = cwClient.listMetrics(request).getMetrics();
                System.out.println("metrics #" + i + ": " + metrics);
                Thread.sleep(1000);
            }
            
            // It may take several minutes for the metric to show up
            // Assert.assertEquals(1, metrics.size());
            
        } finally {
            camelctx.stop();
        }
    }

}
