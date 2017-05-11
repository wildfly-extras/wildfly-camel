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

import org.junit.Assert;

import com.amazonaws.services.kinesis.AmazonKinesisClient;
import com.amazonaws.services.kinesis.AmazonKinesisClientBuilder;
import com.amazonaws.services.kinesis.model.StreamDescription;

public class KinesisUtils {

    public static final String STREAM_NAME = "wfcStream";

    // Attach Policy: AmazonKinesisFullAccess
    public static AmazonKinesisClient createKinesisClient() {
        BasicCredentialsProvider credentials = BasicCredentialsProvider.standard();
        AmazonKinesisClient client = !credentials.isValid() ? null : (AmazonKinesisClient) 
                AmazonKinesisClientBuilder.standard()
                .withCredentials(credentials)
                .withRegion("eu-west-1").build();
        return client;
    }

    public static void createStream(AmazonKinesisClient kinClient) throws Exception {
        kinClient.createStream(STREAM_NAME, 1);
        
        int retries = 40;
        StreamDescription desc = kinClient.describeStream(STREAM_NAME).getStreamDescription();
        while(!"ACTIVE".equals(desc.getStreamStatus()) && 0 < retries--) {
            Thread.sleep(500);
            desc = kinClient.describeStream(STREAM_NAME).getStreamDescription();
            System.out.println(retries + ": " + desc.getStreamARN() + "," + desc.getStreamStatus());
        }
        Assert.assertEquals("ACTIVE", desc.getStreamStatus());
    }

    public static void deleteStream(AmazonKinesisClient kinClient) {
        kinClient.deleteStream(STREAM_NAME);
    }

}
