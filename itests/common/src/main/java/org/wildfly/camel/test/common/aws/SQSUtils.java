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

import com.amazonaws.services.sqs.AmazonSQSClient;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;

public class SQSUtils {

    private static final String SUFFIX = "-id" + SQSUtils.class.getClassLoader().hashCode();
    
    public static final String QUEUE_NAME = "MyNewCamelQueue" + SUFFIX;

    // Attach Policy: AmazonSQSFullAccess
    public static AmazonSQSClient createSQSClient() {
        BasicCredentialsProvider credentials = BasicCredentialsProvider.standard();
        AmazonSQSClient client = !credentials.isValid() ? null : (AmazonSQSClient) 
                AmazonSQSClientBuilder.standard()
                .withCredentials(credentials)
                .withRegion("eu-west-1").build();
        return client;
    }

    public static void createQueue(AmazonSQSClient client) {
        client.createQueue(QUEUE_NAME);
    }

    public static void deleteQueue(AmazonSQSClient client) {
        client.deleteQueue(QUEUE_NAME);
    }
}
