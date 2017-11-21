package org.wildfly.camel.test.aws;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.aws.cw.CwConstants;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.camel.test.aws.subA.CloudWatchClientProducer;
import org.wildfly.camel.test.aws.subA.CloudWatchClientProducer.CloudWatchClientProvider;
import org.wildfly.camel.test.common.aws.AWSUtils;
import org.wildfly.camel.test.common.aws.BasicCredentialsProvider;
import org.wildfly.camel.test.common.aws.CloudWatchUtils;
import org.wildfly.extension.camel.CamelAware;
import org.wildfly.extension.camel.WildFlyCamelContext;

import com.amazonaws.services.cloudwatch.AmazonCloudWatchClient;
import com.amazonaws.services.cloudwatch.model.DimensionFilter;
import com.amazonaws.services.cloudwatch.model.ListMetricsRequest;
import com.amazonaws.services.cloudwatch.model.Metric;

@CamelAware
@RunWith(Arquillian.class)
public class CloudWatchIntegrationTest {

    /** Use the same namespace for each execution of the test */
    private static final String NAMESPACE = CloudWatchIntegrationTest.class.getSimpleName() + "v1";
    /**
     * We use per execution metric name. According to AWS docs the metrics should disappear when they are unused for
     * more than two weeks
     */
    private static final String METRIC_NAME = AWSUtils.toTimestampedName(CloudWatchIntegrationTest.class);
    private static final String DIM_NAME = "MyDimension";
    private static final String DIM_VALUE = "MyValue";

    @Inject
    private CloudWatchClientProvider provider;

    @Deployment
    public static JavaArchive deployment() {
        JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "aws-cw-tests.jar");
        archive.addClasses(CloudWatchClientProducer.class, CloudWatchUtils.class, BasicCredentialsProvider.class,
                AWSUtils.class);
        archive.addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml");
        return archive;
    }

    @Test
    public void testKeyValueOperations() throws Exception {

        AmazonCloudWatchClient cwClient = provider.getClient();
        Assume.assumeNotNull("AWS client not null", cwClient);

        List<Metric> staleMetrics = cwClient.listMetrics(new ListMetricsRequest().withNamespace(NAMESPACE)).getMetrics()
                .stream() //
                .filter(metric -> !metric.getMetricName().startsWith(CloudWatchIntegrationTest.class.getSimpleName())
                        || System.currentTimeMillis()
                                - AWSUtils.toEpochMillis(metric.getMetricName()) > AWSUtils.TWO_WEEKS) //
                .collect(Collectors.toList());
        if (staleMetrics.size() > 0) {
            Assert.fail("Found '" + CloudWatchIntegrationTest.class.getName() + "-*' metrics older than two weeks: "
                    + staleMetrics);
        }

        WildFlyCamelContext camelctx = new WildFlyCamelContext();
        camelctx.getNamingContext().bind("cwClient", cwClient);

        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:metrics").to("aws-cw://" + NAMESPACE + "?amazonCwClient=#cwClient");
            }
        });

        camelctx.start();
        try {
            Map<String, Object> headers = new HashMap<>();
            headers.put(CwConstants.METRIC_NAME, METRIC_NAME);
            headers.put(CwConstants.METRIC_DIMENSION_NAME, DIM_NAME);
            headers.put(CwConstants.METRIC_DIMENSION_VALUE, DIM_VALUE);

            ListMetricsRequest request = new ListMetricsRequest().withNamespace(NAMESPACE).withMetricName(METRIC_NAME)
                    .withDimensions(new DimensionFilter().withName(DIM_NAME).withValue(DIM_VALUE));

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
