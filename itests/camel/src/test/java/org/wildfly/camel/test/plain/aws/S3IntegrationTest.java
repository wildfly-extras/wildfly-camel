package org.wildfly.camel.test.plain.aws;

import java.util.HashMap;
import java.util.Map;

import org.apache.camel.CamelContext;
import org.apache.camel.ConsumerTemplate;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.aws.s3.S3Constants;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.impl.SimpleRegistry;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;
import org.wildfly.camel.test.common.aws.S3Utils;

import com.amazonaws.services.s3.AmazonS3Client;

public class S3IntegrationTest {

    public static final String OBJECT_KEY = "content.txt";

    private static AmazonS3Client s3Client;
    
    @BeforeClass
    public static void beforeClass() throws Exception {
        s3Client = S3Utils.createS3Client();
        if (s3Client != null) {
            S3Utils.createBucket(s3Client);
        }
    }
    
    @AfterClass
    public static void afterClass() throws Exception {
        if (s3Client != null) {
            S3Utils.deleteBucket(s3Client);
        }
    }
    
    
    @Test
    public void testBucketOperations() throws Exception {

        Assume.assumeNotNull("AWS client not null", s3Client);
        
        SimpleRegistry registry = new SimpleRegistry();
        registry.put("s3Client", s3Client);
        
        CamelContext camelctx = new DefaultCamelContext(registry);
        S3Utils.addRoutes(camelctx);
        
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
            s3Client.deleteObject(S3Utils.BUCKET_NAME, OBJECT_KEY);
        }
    }
}
