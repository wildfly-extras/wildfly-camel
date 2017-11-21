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

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.Assert;

import com.amazonaws.services.kinesis.AmazonKinesisClient;
import com.amazonaws.services.kinesis.AmazonKinesisClientBuilder;
import com.amazonaws.services.kinesis.model.DescribeStreamRequest;
import com.amazonaws.waiters.NoOpWaiterHandler;
import com.amazonaws.waiters.Waiter;
import com.amazonaws.waiters.WaiterParameters;

public class KinesisUtils {

    // Attach Policy: AmazonKinesisFullAccess
    public static AmazonKinesisClient createKinesisClient() {
        BasicCredentialsProvider credentials = BasicCredentialsProvider.standard();
        AmazonKinesisClient client = !credentials.isValid() ? null : (AmazonKinesisClient)
                AmazonKinesisClientBuilder.standard()
                .withCredentials(credentials)
                .withRegion("eu-west-1").build();
        return client;
    }

    public static void createStream(AmazonKinesisClient client, String streamName) throws Exception {

        client.createStream(streamName, 1);

        Waiter<DescribeStreamRequest> waiter = client.waiters().streamExists();
        DescribeStreamRequest request = new DescribeStreamRequest().withStreamName(streamName);
        Assert.assertNotNull("Cannot obtain stream description", request);
        Future<Void> future = waiter.runAsync(new WaiterParameters<DescribeStreamRequest>(request), new NoOpWaiterHandler());
        future.get(1, TimeUnit.MINUTES);
    }

}
