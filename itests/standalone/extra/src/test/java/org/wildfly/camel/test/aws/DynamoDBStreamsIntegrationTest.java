package org.wildfly.camel.test.aws;

import java.util.Map;

import javax.inject.Inject;

import org.apache.camel.Exchange;
import org.apache.camel.PollingConsumer;
import org.apache.camel.builder.RouteBuilder;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.camel.test.aws.subA.AmazonDynamoDBUtils;
import org.wildfly.camel.test.aws.subA.AmazonDynamoDBUtils.DynamoDBClientProvider;
import org.wildfly.camel.test.aws.subA.AmazonDynamoDBUtils.DynamoDBStreamsClientProvider;
import org.wildfly.camel.test.aws.subA.BasicCredentialsProvider;
import org.wildfly.camel.test.aws.subA.TableNameStreamsProvider;
import org.wildfly.extension.camel.CamelAware;
import org.wildfly.extension.camel.WildFlyCamelContext;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.Record;
import com.amazonaws.services.dynamodbv2.model.StreamRecord;

@CamelAware
@RunWith(Arquillian.class)
public class DynamoDBStreamsIntegrationTest {

    @Inject
    private String tableName;

    @Inject
    private DynamoDBClientProvider ddbProvider;
    
    @Inject
    private DynamoDBStreamsClientProvider dbsProvider;
    
    @Deployment
    public static JavaArchive deployment() {
        JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "aws-ddb-streams-tests.jar");
        archive.addClasses(AmazonDynamoDBUtils.class, BasicCredentialsProvider.class, TableNameStreamsProvider.class);
        archive.addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml");
        return archive;
    }
    
    @Test
    public void testKeyValueOperations() throws Exception {
        
        AmazonDynamoDBClient ddbClient = ddbProvider.getClient();
        Assume.assumeNotNull("AWS client not null", ddbClient);
        
        WildFlyCamelContext camelctx = new WildFlyCamelContext();
        camelctx.getNamingContext().bind("ddbClientB", ddbClient);
        camelctx.getNamingContext().bind("dbsClientB", dbsProvider.getClient());
        
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:start").to("aws-ddb://" + tableName + "?amazonDDBClient=#ddbClientB");
                from("aws-ddbstream://" + tableName + "?amazonDynamoDbStreamsClient=#dbsClientB").to("seda:end");
            }
        });

        PollingConsumer pollingConsumer = camelctx.getEndpoint("seda:end").createPollingConsumer();
        pollingConsumer.start();
            
        camelctx.start();
        try {
            AmazonDynamoDBUtils.putItem(camelctx, "Book 103 Title");
            
            String result = ((AttributeValue) AmazonDynamoDBUtils.getItem(camelctx).get("Title")).getS();
            Assert.assertEquals("Book 103 Title", result);

            Exchange exchange = pollingConsumer.receive(3000);
            Assert.assertNull(exchange);
            
            AmazonDynamoDBUtils.updItem(camelctx, "Book 103 Update");

            result = ((AttributeValue) AmazonDynamoDBUtils.getItem(camelctx).get("Title")).getS();
            Assert.assertEquals("Book 103 Update", result);
                
            exchange = pollingConsumer.receive(3000);
            StreamRecord record = exchange.getIn().getBody(Record.class).getDynamodb();
            Map<String, AttributeValue> oldImage = record.getOldImage();
            Map<String, AttributeValue> newImage = record.getNewImage();
            Assert.assertEquals("Book 103 Title", oldImage.get("Title").getS());
            Assert.assertEquals("Book 103 Update", newImage.get("Title").getS());
            
        } finally {
            camelctx.stop();
        }
    }

}
