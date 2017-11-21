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
import org.wildfly.camel.test.aws.subA.DynamoDBClientProducer;
import org.wildfly.camel.test.aws.subA.DynamoDBClientProducer.DynamoDBClientProvider;
import org.wildfly.camel.test.aws.subA.DynamoDBClientProducer.DynamoDBStreamsClientProvider;
import org.wildfly.camel.test.common.aws.AWSUtils;
import org.wildfly.camel.test.common.aws.BasicCredentialsProvider;
import org.wildfly.camel.test.common.aws.DynamoDBUtils;
import org.wildfly.extension.camel.CamelAware;
import org.wildfly.extension.camel.WildFlyCamelContext;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.Record;
import com.amazonaws.services.dynamodbv2.model.StreamRecord;
import com.amazonaws.services.dynamodbv2.model.TableDescription;

@CamelAware
@RunWith(Arquillian.class)
public class DynamoDBStreamsIntegrationTest {

    private static final String tableName = AWSUtils.toTimestampedName(DynamoDBStreamsIntegrationTest.class);

    @Inject
    private DynamoDBClientProvider ddbProvider;

    @Inject
    private DynamoDBStreamsClientProvider dbsProvider;

    @Deployment
    public static JavaArchive deployment() {
        JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "aws-ddb-streams-tests.jar");
        archive.addClasses(DynamoDBClientProducer.class, DynamoDBUtils.class, BasicCredentialsProvider.class, AWSUtils.class);
        archive.addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml");
        return archive;
    }

    @Test
    public void testKeyValueOperations() throws Exception {

        AmazonDynamoDBClient ddbClient = ddbProvider.getClient();
        Assume.assumeNotNull("AWS client not null", ddbClient);

        DynamoDBUtils.assertNoStaleTables(ddbClient, "before");

        try {
            try {
                TableDescription description = DynamoDBUtils.createTable(ddbClient, tableName);
                Assert.assertEquals("ACTIVE", description.getTableStatus());

                WildFlyCamelContext camelctx = new WildFlyCamelContext();
                camelctx.getNamingContext().bind("ddbClientB", ddbClient);
                camelctx.getNamingContext().bind("dbsClientB", dbsProvider.getClient());

                camelctx.addRoutes(new RouteBuilder() {
                    @Override
                    public void configure() throws Exception {
                        from("direct:start").to("aws-ddb://" + tableName + "?amazonDDBClient=#ddbClientB");
                        from("aws-ddbstream://" + tableName + "?amazonDynamoDbStreamsClient=#dbsClientB")
                                .to("seda:end");
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
            } finally {
                DynamoDBUtils.deleteTable(ddbClient, tableName);
            }
        } finally {
            DynamoDBUtils.assertNoStaleTables(ddbClient, "after");
        }
    }

}
