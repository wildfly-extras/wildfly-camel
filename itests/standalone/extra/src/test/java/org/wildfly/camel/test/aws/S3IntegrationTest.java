package org.wildfly.camel.test.aws;

import java.util.HashMap;
import java.util.Map;

import org.apache.camel.ConsumerTemplate;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.aws.s3.S3Constants;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.extension.camel.CamelAware;
import org.wildfly.extension.camel.WildFlyCamelContext;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;

@CamelAware
@RunWith(Arquillian.class)
public class S3IntegrationTest {

    static final String BUCKET_NAME = "wfc-aws-s3-bucket";
    static final String OBJECT_KEY = "content.txt";

    @Deployment
    public static JavaArchive deployment() {
        return ShrinkWrap.create(JavaArchive.class, "aws-s3-tests");
    }

    @Test
    public void testBucketOperations() throws Exception {

        String accessId = System.getenv("AWSAccessId");
        String secretKey = System.getenv("AWSSecretKey");
        Assume.assumeNotNull("AWSAccessId not null", accessId);
        Assume.assumeNotNull("AWSSecretKey not null", secretKey);
        
        WildFlyCamelContext camelctx = new WildFlyCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                String clientref = "amazonS3Client=#s3Client";
                from("direct:upload").to("aws-s3://" + BUCKET_NAME + "?" + clientref);
                from("aws-s3://" + BUCKET_NAME + "?" + clientref).to("seda:read");
            }
        });

        AmazonS3 client = AmazonS3ClientBuilder.standard()
                .withCredentials(new BasicCredentialsProvider(accessId, secretKey))
                .withRegion("eu-west-1")
                .build();

        camelctx.getNamingContext().bind("s3Client", client);
        
        client.createBucket(BUCKET_NAME);
        try {
            camelctx.start();
            try {
                
                // Put object
                Map<String, Object> headers = new HashMap<>();
                headers.put(S3Constants.KEY, OBJECT_KEY);
                
                ProducerTemplate producer = camelctx.createProducerTemplate();
                String content = "My bucket content";
                String result1 = producer.requestBodyAndHeaders("direct:upload", content, headers, String.class);
                Assert.assertEquals(content, result1);
                
                ConsumerTemplate consumer = camelctx.createConsumerTemplate();
                String result2 = consumer.receiveBody("seda:read", String.class);
                Assert.assertEquals(content, result2);
            } finally {
                camelctx.stop();
            }
        } finally {
            client.deleteObject(BUCKET_NAME, OBJECT_KEY);
            client.deleteBucket(BUCKET_NAME);
        }
    }

    static class BasicCredentialsProvider implements AWSCredentialsProvider {

        final String accessId;
        final String secretKey;

        BasicCredentialsProvider(String accessId, String secretKey) {
            this.accessId = accessId;
            this.secretKey = secretKey;
        }

        @Override
        public AWSCredentials getCredentials() {
            return new BasicAWSCredentials(accessId, secretKey);
        }

        @Override
        public void refresh() {
        }
    }
}
