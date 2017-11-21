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
package org.wildfly.camel.test.common.aws;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Table;
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

public class DynamoDBUtils {

    // Attach Policy: AmazonDynamoDBFullAccess
    public static AmazonDynamoDBClient createDynamoDBClient() {
        BasicCredentialsProvider credentials = BasicCredentialsProvider.standard();
        AmazonDynamoDBClient client = !credentials.isValid() ? null : (AmazonDynamoDBClient)
                AmazonDynamoDBClientBuilder.standard()
                .withCredentials(credentials)
                .withRegion("eu-west-1")
                .build();
        return client;
    }

    public static AmazonDynamoDBStreamsClient createDynamoDBStreamsClient() {
        BasicCredentialsProvider credentials = BasicCredentialsProvider.standard();
        AmazonDynamoDBStreamsClient client = !credentials.isValid() ? null : (AmazonDynamoDBStreamsClient)
                AmazonDynamoDBStreamsClientBuilder.standard()
                .withCredentials(credentials)
                .withRegion("eu-west-1")
                .build();
        return client;
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

    public static TableDescription createTable(AmazonDynamoDB client, String tableName) throws InterruptedException {
        CreateTableRequest tableReq = new CreateTableRequest().withTableName(tableName)
                .withKeySchema(new KeySchemaElement("Id", KeyType.HASH))
                .withAttributeDefinitions(new AttributeDefinition("Id", ScalarAttributeType.N))
                .withProvisionedThroughput(new ProvisionedThroughput(10L, 10L))
                .withStreamSpecification(new StreamSpecification().withStreamEnabled(true).withStreamViewType(StreamViewType.NEW_AND_OLD_IMAGES));

        DynamoDB dynamoDB = new DynamoDB(client);
        Table table = dynamoDB.createTable(tableReq);
        return table.waitForActive();
    }

    public static void deleteTable(AmazonDynamoDB client, String tableName) throws InterruptedException {
        new DynamoDB(client).getTable(tableName).delete();
    }

    public static void assertNoStaleTables(AmazonDynamoDBClient client, String when) {
        /* Get the list of tables without the ones in the deleting state */
        List<String> tables = client.listTables().getTableNames().stream()
                .filter(t -> System.currentTimeMillis() - AWSUtils.toEpochMillis(t) > AWSUtils.HOUR || !"DELETING".equals(client.describeTable(t).getTable().getTableStatus()))
                .collect(Collectors.toList());
        Assert.assertEquals(String.format("Found stale DynamoDB tables %s running the test: %s", when, tables), 0, tables.size());
    }

}
