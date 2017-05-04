package org.wildfly.camel.test.plain.aws;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import org.apache.camel.CamelContext;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.impl.SimpleRegistry;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.wildfly.camel.test.common.aws.DynamoDBUtils;
import org.wildfly.camel.test.common.types.CatalogItem;
import org.wildfly.camel.test.common.types.TableNameProviderA;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBQueryExpression;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.TableDescription;

public class DynamoDBIntegrationTest {

    public static String tableName = TableNameProviderA.TABLE_NAME;

    private static AmazonDynamoDBClient ddbClient;
    
    @BeforeClass
    public static void beforeClass() throws Exception {
        ddbClient = DynamoDBUtils.createDynamoDBClient();
        if (ddbClient != null) {
            TableDescription description = DynamoDBUtils.createTable(ddbClient, tableName);
            Assert.assertEquals("ACTIVE", description.getTableStatus());
        }
    }
    
    @AfterClass
    public static void afterClass() throws Exception {
        if (ddbClient != null) {
            DynamoDBUtils.deleteTable(ddbClient, tableName);
        }
    }
    
    @Test
    @Ignore("[#1778] Add support for DynamoDB mapper")
    public void testMapperOperations() throws Exception {

        Assume.assumeNotNull("AWS client not null", ddbClient);

        DynamoDBMapper mapper = new DynamoDBMapper(ddbClient);

        CatalogItem item = new CatalogItem();
        item.setId(102);
        item.setTitle("Book 102 Title");
        item.setISBN("222-2222222222");
        item.setBookAuthors(new HashSet<String>(Arrays.asList("Author 1", "Author 2")));
        item.setSomeProp("Test");

        mapper.save(item);

        item = new CatalogItem();
        item.setId(102);

        DynamoDBQueryExpression<CatalogItem> exp = new DynamoDBQueryExpression<CatalogItem>().withHashKeyValues(item);

        List<CatalogItem> result = mapper.query(CatalogItem.class, exp);
        Assert.assertEquals(1, result.size());
        Assert.assertEquals(new Integer(102), result.get(0).getId());
        Assert.assertEquals("Book 102 Title", result.get(0).getTitle());
    }

    @Test
    public void testKeyValueOperations() throws Exception {
        
        Assume.assumeNotNull("AWS client not null", ddbClient);
        
        SimpleRegistry registry = new SimpleRegistry();
        registry.put("ddbClient", ddbClient);
        
        CamelContext camelctx = new DefaultCamelContext(registry);
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:start").to("aws-ddb://" + tableName + "?amazonDDBClient=#ddbClient");
            }
        });

        camelctx.start();
        try {
            DynamoDBUtils.putItem(camelctx, "Book 103 Title");
            
            String result = ((AttributeValue) DynamoDBUtils.getItem(camelctx).get("Title")).getS();
            Assert.assertEquals("Book 103 Title", result);

            DynamoDBUtils.updItem(camelctx, "Book 103 Update");

            result = ((AttributeValue) DynamoDBUtils.getItem(camelctx).get("Title")).getS();
            Assert.assertEquals("Book 103 Update", result);
                
        } finally {
            camelctx.stop();
        }
    }

}
