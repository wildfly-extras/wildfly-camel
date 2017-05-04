package org.wildfly.camel.test.plain.aws;

import java.util.Map;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.PollingConsumer;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.impl.SimpleRegistry;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;
import org.wildfly.camel.test.common.aws.DynamoDBUtils;
import org.wildfly.camel.test.common.types.TableNameProviderB;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBStreamsClient;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.Record;
import com.amazonaws.services.dynamodbv2.model.StreamRecord;
import com.amazonaws.services.dynamodbv2.model.TableDescription;

public class DynamoDBStreamsIntegrationTest {

    public static String tableName = TableNameProviderB.TABLE_NAME;

    private static AmazonDynamoDBClient ddbClient;
    private static AmazonDynamoDBStreamsClient dbsClient;
    
    @BeforeClass
    public static void beforeClass() throws Exception {
        ddbClient = DynamoDBUtils.createDynamoDBClient();
        if (ddbClient != null) {
            TableDescription description = DynamoDBUtils.createTable(ddbClient, tableName);
            Assert.assertEquals("ACTIVE", description.getTableStatus());
        }
        dbsClient = DynamoDBUtils.createDynamoDBStreamsClient();
    }
    
    @AfterClass
    public static void afterClass() throws Exception {
        if (ddbClient != null) {
            DynamoDBUtils.deleteTable(ddbClient, tableName);
        }
    }
    
    @Test
    public void testKeyValueOperations() throws Exception {
        
        Assume.assumeNotNull("AWS client not null", ddbClient);
        
        SimpleRegistry registry = new SimpleRegistry();
        registry.put("ddbClient", ddbClient);
        registry.put("dbsClient", dbsClient);
        
        CamelContext camelctx = new DefaultCamelContext(registry);
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:start").to("aws-ddb://" + tableName + "?amazonDDBClient=#ddbClient");
                from("aws-ddbstream://" + tableName + "?amazonDynamoDbStreamsClient=#dbsClient").to("seda:end");
            }
        });

        PollingConsumer pollingConsumer = camelctx.getEndpoint("seda:end").createPollingConsumer();
        pollingConsumer.start();
            
        camelctx.start();
        try {
            DynamoDBUtils.putItem(camelctx, "Book 103 Title");
            
            String result = ((AttributeValue) DynamoDBUtils.getItem(camelctx).get("Title")).getS();
            Assert.assertEquals("Book 103 Title", result);

            Exchange exchange = pollingConsumer.receive(3000);
            Assert.assertNull(exchange);
            
            DynamoDBUtils.updItem(camelctx, "Book 103 Update");

            result = ((AttributeValue) DynamoDBUtils.getItem(camelctx).get("Title")).getS();
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
