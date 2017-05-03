/*
 * #%L
 * Wildfly Camel :: Testsuite
 * %%
 * Copyright (C) 2013 - 2017 RedHat
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package org.wildfly.camel.test.aws.subA;

import java.util.HashMap;
import java.util.Map;

import javax.enterprise.inject.Disposes;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.ExchangeBuilder;
import org.apache.camel.component.aws.ddb.DdbConstants;
import org.apache.camel.component.aws.ddb.DdbOperations;
import org.junit.Assert;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBStreamsClient;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBStreamsClientBuilder;
import com.amazonaws.services.dynamodbv2.model.AttributeDefinition;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.AttributeValueUpdate;
import com.amazonaws.services.dynamodbv2.model.CreateTableRequest;
import com.amazonaws.services.dynamodbv2.model.KeySchemaElement;
import com.amazonaws.services.dynamodbv2.model.KeyType;
import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughput;
import com.amazonaws.services.dynamodbv2.model.ScalarAttributeType;
import com.amazonaws.services.dynamodbv2.model.StreamSpecification;
import com.amazonaws.services.dynamodbv2.model.StreamViewType;
import com.amazonaws.services.dynamodbv2.model.TableDescription;

public class AmazonDynamoDBUtils {

    @Inject
    private String tableName;
    
    public class DynamoDBClientProvider {
        private final AmazonDynamoDBClient client;
        DynamoDBClientProvider(AmazonDynamoDBClient client) {
            this.client = client;
        }
        public AmazonDynamoDBClient getClient() {
            return client;
        }
    }
    
    public class DynamoDBStreamsClientProvider {
        private final AmazonDynamoDBStreamsClient client;
        DynamoDBStreamsClientProvider(AmazonDynamoDBStreamsClient client) {
            this.client = client;
        }
        public AmazonDynamoDBStreamsClient getClient() {
            return client;
        }
    }
    
    @Produces
    @Singleton
    public DynamoDBClientProvider getDynamoDBClientProvider() throws Exception {
        AmazonDynamoDBClient client = null;
        String accessId = System.getenv("AWSAccessId");
        String secretKey = System.getenv("AWSSecretKey");
        if (accessId != null && secretKey != null) {
            client = (AmazonDynamoDBClient) AmazonDynamoDBClientBuilder.standard()
                    .withCredentials(new BasicCredentialsProvider(accessId, secretKey))
                    .withRegion("eu-west-1")
                    .build();

            TableDescription createTable = createTable(client);
            Assert.assertEquals("ACTIVE", createTable.getTableStatus());
        }
        return new DynamoDBClientProvider(client);
    }

    @Produces
    @Singleton
    public DynamoDBStreamsClientProvider getDynamoDBStreamsClientProvider() throws Exception {
        AmazonDynamoDBStreamsClient client = null;
        String accessId = System.getenv("AWSAccessId");
        String secretKey = System.getenv("AWSSecretKey");
        if (accessId != null && secretKey != null) {
            client = (AmazonDynamoDBStreamsClient) AmazonDynamoDBStreamsClientBuilder.standard()
                    .withCredentials(new BasicCredentialsProvider(accessId, secretKey))
                    .withRegion("eu-west-1")
                    .build();
        }
        return new DynamoDBStreamsClientProvider(client);
    }

    public void close(@Disposes DynamoDBClientProvider provider) throws Exception {
        if (provider.getClient() != null) {
            deleteTable(provider.getClient());
        }
    }

    public static void putItem(CamelContext camelctx, String title) {
        HashMap<String, AttributeValue> putItem = new HashMap<>();
        putItem.put("Id", new AttributeValue().withN("103"));
        putItem.put("Title", new AttributeValue().withS(title));

        Exchange exchange = new ExchangeBuilder(camelctx)
            .withHeader(DdbConstants.OPERATION, DdbOperations.PutItem)
            .withHeader(DdbConstants.ITEM, putItem).build();

        ProducerTemplate producer = camelctx.createProducerTemplate();
        producer.send("direct:start", exchange);
    }

    public static void updItem(CamelContext camelctx, String title) {
        
        HashMap<String, AttributeValue> key = new HashMap<>();
        key.put("Id", new AttributeValue().withN("103"));

        HashMap<String, AttributeValueUpdate> updItem = new HashMap<>();
        AttributeValueUpdate updValue = new AttributeValueUpdate();
        updValue.setValue(new AttributeValue().withS(title));
        updItem.put("Title", updValue);
        
        Exchange exchange = new ExchangeBuilder(camelctx)
                .withHeader(DdbConstants.OPERATION, DdbOperations.UpdateItem)
                .withHeader(DdbConstants.KEY, key)
                .withHeader(DdbConstants.UPDATE_VALUES, updItem).build();

        ProducerTemplate producer = camelctx.createProducerTemplate();
        producer.send("direct:start", exchange);
        Assert.assertNull(exchange.getException());
    }

    public static Map<?, ?> getItem(CamelContext camelctx) {
        HashMap<String, AttributeValue> key = new HashMap<>();
        key.put("Id", new AttributeValue().withN("103"));

        Exchange exchange = new ExchangeBuilder(camelctx)
                .withHeader(DdbConstants.OPERATION, DdbOperations.GetItem)
                .withHeader(DdbConstants.KEY, key).build();

        ProducerTemplate producer = camelctx.createProducerTemplate();
        producer.send("direct:start", exchange);
        Assert.assertNull(exchange.getException());
        
        return exchange.getIn().getHeader(DdbConstants.ATTRIBUTES, Map.class);
    }

    private TableDescription createTable(AmazonDynamoDB client) throws InterruptedException {

        CreateTableRequest tableReq = new CreateTableRequest().withTableName(tableName)
                .withKeySchema(new KeySchemaElement("Id", KeyType.HASH))
                .withAttributeDefinitions(new AttributeDefinition("Id", ScalarAttributeType.N))
                .withProvisionedThroughput(new ProvisionedThroughput(10L, 10L))
                .withStreamSpecification(new StreamSpecification().withStreamEnabled(true).withStreamViewType(StreamViewType.NEW_AND_OLD_IMAGES));

        return awaitStatus(client, client.createTable(tableReq).getTableDescription(), "ACTIVE");
    }

    private void deleteTable(AmazonDynamoDB client) throws InterruptedException {
        TableDescription description = client.describeTable(tableName).getTable();
        awaitStatus(client, description, "ACTIVE");
        client.deleteTable(tableName);
    }

    private TableDescription awaitStatus(AmazonDynamoDB client, TableDescription description, String status) throws InterruptedException {
        int retries = 20;
        while (--retries > 0 && !description.getTableStatus().equals(status)) {
            Thread.sleep(500);
            description = client.describeTable(tableName).getTable();
        }
        return description;
    }
}
