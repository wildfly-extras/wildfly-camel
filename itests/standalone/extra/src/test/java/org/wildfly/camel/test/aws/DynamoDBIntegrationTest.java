package org.wildfly.camel.test.aws;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.apache.camel.Exchange;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.ExchangeBuilder;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.aws.ddb.DdbConstants;
import org.apache.camel.component.aws.ddb.DdbOperations;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.After;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.camel.test.aws.subA.BasicCredentialsProvider;
import org.wildfly.camel.test.aws.subA.CatalogItem;
import org.wildfly.extension.camel.CamelAware;
import org.wildfly.extension.camel.WildFlyCamelContext;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBQueryExpression;
import com.amazonaws.services.dynamodbv2.model.AttributeDefinition;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.CreateTableRequest;
import com.amazonaws.services.dynamodbv2.model.KeySchemaElement;
import com.amazonaws.services.dynamodbv2.model.KeyType;
import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughput;
import com.amazonaws.services.dynamodbv2.model.ScalarAttributeType;
import com.amazonaws.services.dynamodbv2.model.TableDescription;

@CamelAware
@RunWith(Arquillian.class)
public class DynamoDBIntegrationTest {

    static final String TABLE_NAME = "ProductCatalog";

    private AmazonDynamoDB client;
    
    @Deployment
    public static JavaArchive deployment() {
        JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "aws-ddb-tests");
        archive.addClasses(BasicCredentialsProvider.class, CatalogItem.class);
        return archive;
    }

    @Before
    public void before() throws Exception {
        
        String accessId = System.getenv("AWSAccessId");
        String secretKey = System.getenv("AWSSecretKey");
        Assume.assumeNotNull("AWSAccessId not null", accessId);
        Assume.assumeNotNull("AWSSecretKey not null", secretKey);

        client = AmazonDynamoDBClientBuilder.standard()
                .withCredentials(new BasicCredentialsProvider(accessId, secretKey))
                .withRegion("eu-west-1")
                .build();

        TableDescription createTable = createTable(client);
        Assert.assertEquals("ACTIVE", createTable.getTableStatus());
    }
    
    @After
    public void after() throws Exception {
        deleteTable(client);
    }
    
    @Test
    @Ignore("[#1778] Add support for DynamoDB mapper")
    public void testMapperOperations() throws Exception {

        Assume.assumeNotNull("AWS client not null", client);
        DynamoDBMapper mapper = new DynamoDBMapper(client);

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
        
        Assume.assumeNotNull("AWS client not null", client);
        
        WildFlyCamelContext camelctx = new WildFlyCamelContext();
        camelctx.getNamingContext().bind("ddbClient", client);
        
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:start").to("aws-ddb://" + TABLE_NAME + "?amazonDDBClient=#ddbClient");
            }
        });

        camelctx.start();
        try {
            HashMap<String, AttributeValue> item = new HashMap<>();
            item.put("Id", new AttributeValue().withN("103"));
            item.put("Title", new AttributeValue().withS("Book 103 Title"));

            ProducerTemplate producer = camelctx.createProducerTemplate();

            Exchange exchange = new ExchangeBuilder(camelctx)
                .withHeader(DdbConstants.OPERATION, DdbOperations.PutItem)
                .withHeader(DdbConstants.ITEM, item).build();

            producer.send("direct:start", exchange);
            
            HashMap<String, AttributeValue> key = new HashMap<>();
            key.put("Id", new AttributeValue().withN("103"));

            exchange = new ExchangeBuilder(camelctx)
                    .withHeader(DdbConstants.OPERATION, DdbOperations.GetItem)
                    .withHeader(DdbConstants.KEY, key).build();

            producer.send("direct:start", exchange);
            Map<?, ?> result = exchange.getIn().getHeader(DdbConstants.ATTRIBUTES, Map.class);
            Assert.assertEquals("Book 103 Title", ((AttributeValue) result.get("Title")).getS());
                
        } finally {
            camelctx.stop();
        }
    }

    private static TableDescription createTable(AmazonDynamoDB client) throws InterruptedException {

        CreateTableRequest tableReq = new CreateTableRequest().withTableName(TABLE_NAME)
                .withKeySchema(new KeySchemaElement("Id", KeyType.HASH))
                .withAttributeDefinitions(new AttributeDefinition("Id", ScalarAttributeType.N))
                .withProvisionedThroughput(new ProvisionedThroughput(10L, 10L));

        return awaitStatus(client, client.createTable(tableReq).getTableDescription(), "ACTIVE");
    }

    private static void deleteTable(AmazonDynamoDB client) throws InterruptedException {
        TableDescription description = client.describeTable(TABLE_NAME).getTable();
        awaitStatus(client, description, "ACTIVE");
        client.deleteTable(TABLE_NAME);
    }

    private static TableDescription awaitStatus(AmazonDynamoDB client, TableDescription description, String status) throws InterruptedException {
        int retries = 20;
        while (--retries > 0 && !description.getTableStatus().equals(status)) {
            Thread.sleep(500);
            description = client.describeTable(TABLE_NAME).getTable();
        }
        return description;
    }
}
