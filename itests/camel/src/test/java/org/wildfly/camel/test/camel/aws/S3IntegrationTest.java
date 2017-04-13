package org.wildfly.camel.test.camel.aws;

import java.util.HashMap;
import java.util.Map;

import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.aws.s3.S3Constants;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.impl.SimpleRegistry;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;

/*
    Create the test bucket
    $ aws s3 mb s3://wfc-test

    Assign this bucket policy
    {
    "Version": "2012-10-17",
    "Statement": [
        {
            "Effect": "Allow",
            "Principal": "*",
            "Action": [
                "s3:DeleteObject",
                "s3:GetObject",
                "s3:PutObject"
            ],
            "Resource": "arn:aws:s3:::wfc-test/*"
        }
    ]
    }
*/
public class S3IntegrationTest {

    static final String OBJECT_KEY = "content.txt";

    @Test
    public void testBucketOperations() throws Exception {

        String accessId = System.getenv("AWSAccessId");
        String secretKey = System.getenv("AWSSecretKey");
        Assume.assumeNotNull("AWSAccessId not null", accessId);
        Assume.assumeNotNull("AWSSecretKey not null", secretKey);
        
        AmazonS3 client = AmazonS3ClientBuilder.standard()
                .withCredentials(new BasicCredentialsProvider(accessId, secretKey))
                .build();

        SimpleRegistry registry = new SimpleRegistry();
        registry.put("s3Client", client);
        
        CamelContext camelctx = new DefaultCamelContext(registry);
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                String clientref = "amazonS3Client=#s3Client";
                from("direct:upload").to("aws-s3://wfc-test?" + clientref);
            }
        });

        camelctx.start();
        try {
            
            // Put object
            Map<String, Object> headers = new HashMap<>();
            headers.put(S3Constants.KEY, OBJECT_KEY);
            
            ProducerTemplate template = camelctx.createProducerTemplate();
            String content = "My bucket content";
            String result = template.requestBodyAndHeaders("direct:upload", content, headers, String.class);
            Assert.assertEquals(content, result);
            
        } finally {
            camelctx.stop();
        }
        
        // Delete Object
        client.deleteObject("wfc-test", OBJECT_KEY);
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
