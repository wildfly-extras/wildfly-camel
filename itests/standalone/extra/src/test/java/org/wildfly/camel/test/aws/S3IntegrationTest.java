package org.wildfly.camel.test.aws;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.apache.camel.ConsumerTemplate;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.aws.s3.S3Constants;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.camel.test.aws.subA.BasicCredentialsProvider;
import org.wildfly.camel.test.aws.subA.AmazonS3Utils;
import org.wildfly.camel.test.aws.subA.AmazonS3Utils.AWSClientProvider;
import org.wildfly.extension.camel.CamelAware;
import org.wildfly.extension.camel.WildFlyCamelContext;

import com.amazonaws.services.s3.AmazonS3Client;

@CamelAware
@RunWith(Arquillian.class)
public class S3IntegrationTest {

    public static final String BUCKET_NAME = "wfc-aws-s3-bucket";
    public static final String OBJECT_KEY = "content.txt";

    @Inject
    private AWSClientProvider provider;
    
    @Deployment
    public static JavaArchive deployment() {
        JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "aws-s3-tests.jar");
        archive.addClasses(AmazonS3Utils.class, BasicCredentialsProvider.class);
        archive.addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml");
        return archive;
    }
    
    @Test
    public void testBucketOperations() throws Exception {

        AmazonS3Client client = provider.getClient();
        Assume.assumeNotNull("AWS client not null", client);
        
        WildFlyCamelContext camelctx = new WildFlyCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                String clientref = "amazonS3Client=#s3Client";
                from("direct:upload").to("aws-s3://" + BUCKET_NAME + "?" + clientref);
                from("aws-s3://" + BUCKET_NAME + "?" + clientref).to("seda:read");
            }
        });
        camelctx.getNamingContext().bind("s3Client", client);
        
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
        }
    }
}
