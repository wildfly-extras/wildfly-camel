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

import javax.enterprise.inject.Disposes;
import javax.enterprise.inject.Produces;
import javax.inject.Singleton;

import org.junit.Assert;
import org.wildfly.camel.test.aws.DynamoDBIntegrationTest;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.model.AttributeDefinition;
import com.amazonaws.services.dynamodbv2.model.CreateTableRequest;
import com.amazonaws.services.dynamodbv2.model.KeySchemaElement;
import com.amazonaws.services.dynamodbv2.model.KeyType;
import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughput;
import com.amazonaws.services.dynamodbv2.model.ScalarAttributeType;
import com.amazonaws.services.dynamodbv2.model.TableDescription;

public class DynamoDBClientProducer {

    public class AWSClientProvider {
        private final AmazonDynamoDBClient client;
        AWSClientProvider(AmazonDynamoDBClient client) {
            this.client = client;
        }
        public AmazonDynamoDBClient getClient() {
            return client;
        }
    }
    
    @Produces
    @Singleton
    public AWSClientProvider getClientProvider() throws Exception {
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
        return new AWSClientProvider(client);
    }

    public void close(@Disposes AWSClientProvider provider) throws Exception {
        if (provider.getClient() != null) {
            deleteTable(provider.getClient());
        }
    }

    private TableDescription createTable(AmazonDynamoDB client) throws InterruptedException {

        CreateTableRequest tableReq = new CreateTableRequest().withTableName(DynamoDBIntegrationTest.TABLE_NAME)
                .withKeySchema(new KeySchemaElement("Id", KeyType.HASH))
                .withAttributeDefinitions(new AttributeDefinition("Id", ScalarAttributeType.N))
                .withProvisionedThroughput(new ProvisionedThroughput(10L, 10L));

        return awaitStatus(client, client.createTable(tableReq).getTableDescription(), "ACTIVE");
    }

    private void deleteTable(AmazonDynamoDB client) throws InterruptedException {
        TableDescription description = client.describeTable(DynamoDBIntegrationTest.TABLE_NAME).getTable();
        awaitStatus(client, description, "ACTIVE");
        client.deleteTable(DynamoDBIntegrationTest.TABLE_NAME);
    }

    private TableDescription awaitStatus(AmazonDynamoDB client, TableDescription description, String status) throws InterruptedException {
        int retries = 20;
        while (--retries > 0 && !description.getTableStatus().equals(status)) {
            Thread.sleep(500);
            description = client.describeTable(DynamoDBIntegrationTest.TABLE_NAME).getTable();
        }
        return description;
    }
}
