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
import javax.inject.Inject;
import javax.inject.Singleton;

import org.junit.Assert;
import org.wildfly.camel.test.common.aws.DynamoDBUtils;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBStreamsClient;
import com.amazonaws.services.dynamodbv2.model.TableDescription;

public class DynamoDBClientProducer {

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
        AmazonDynamoDBClient client = DynamoDBUtils.createDynamoDBClient();
        if (client != null) {
            TableDescription description = DynamoDBUtils.createTable(client, tableName);
            Assert.assertEquals("ACTIVE", description.getTableStatus());
        }
        return new DynamoDBClientProvider(client);
    }

    @Produces
    @Singleton
    public DynamoDBStreamsClientProvider getDynamoDBStreamsClientProvider() throws Exception {
        AmazonDynamoDBStreamsClient client = DynamoDBUtils.createDynamoDBStreamsClient();
        return new DynamoDBStreamsClientProvider(client);
    }

    public void close(@Disposes DynamoDBClientProvider provider) throws Exception {
        if (provider.getClient() != null) {
            DynamoDBUtils.deleteTable(provider.getClient(), tableName);
        }
    }
}
