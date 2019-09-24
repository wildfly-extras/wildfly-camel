package org.wildfly.camel.test.aws;

import javax.inject.Inject;

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
import org.wildfly.camel.test.common.aws.AWSUtils;
import org.wildfly.camel.test.common.aws.BasicCredentialsProvider;
import org.wildfly.camel.test.common.aws.DynamoDBUtils;
import org.wildfly.extension.camel.CamelAware;
import org.wildfly.extension.camel.WildFlyCamelContext;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.TableDescription;

@CamelAware
@RunWith(Arquillian.class)
public class DynamoDBIntegrationTest {

    private static final String tableName = AWSUtils.toTimestampedName(DynamoDBIntegrationTest.class);

    @Inject
    private DynamoDBClientProvider provider;

    @Deployment
    public static JavaArchive deployment() {
        JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "aws-ddb-tests.jar");
        archive.addClasses(DynamoDBClientProducer.class, DynamoDBUtils.class, BasicCredentialsProvider.class, AWSUtils.class);
        archive.addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml");
        return archive;
    }

    @Test
    public void testKeyValueOperations() throws Exception {

        AmazonDynamoDBClient client = provider.getClient();
        Assume.assumeNotNull("AWS client not null", client);

        DynamoDBUtils.assertNoStaleTables(client, "before");

        try {
            try {
                TableDescription description = DynamoDBUtils.createTable(client, tableName);
                Assert.assertEquals("ACTIVE", description.getTableStatus());

                WildFlyCamelContext camelctx = new WildFlyCamelContext();
                camelctx.getNamingContext().bind("ddbClientA", client);

                camelctx.addRoutes(new RouteBuilder() {
                    @Override
                    public void configure() throws Exception {
                        from("direct:start").to("aws-ddb://" + tableName + "?amazonDDBClient=#ddbClientA");
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
                    camelctx.close();
                }
            } finally {
                DynamoDBUtils.deleteTable(client, tableName);
            }
        } finally {
            DynamoDBUtils.assertNoStaleTables(client, "after");
        }
    }

}
