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
import org.wildfly.camel.test.common.aws.BasicCredentialsProvider;
import org.wildfly.camel.test.common.aws.DynamoDBUtils;
import org.wildfly.camel.test.common.types.TableNameProviderA;
import org.wildfly.extension.camel.CamelAware;
import org.wildfly.extension.camel.WildFlyCamelContext;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;

@CamelAware
@RunWith(Arquillian.class)
public class DynamoDBIntegrationTest {

    @Inject
    private String tableName;

    @Inject
    private DynamoDBClientProvider provider;
    
    @Deployment
    public static JavaArchive deployment() {
        JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "aws-ddb-tests.jar");
        archive.addClasses(DynamoDBClientProducer.class, DynamoDBUtils.class, BasicCredentialsProvider.class, TableNameProviderA.class);
        archive.addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml");
        return archive;
    }
    
    @Test
    public void testKeyValueOperations() throws Exception {
        
        AmazonDynamoDBClient client = provider.getClient();
        Assume.assumeNotNull("AWS client not null", client);
        
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
            camelctx.stop();
        }
    }

}
