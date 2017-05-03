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

import org.wildfly.camel.test.aws.S3IntegrationTest;

import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;

public class AmazonS3Utils {

    public class AWSClientProvider {
        private final AmazonS3Client client;
        AWSClientProvider(AmazonS3Client client) {
            this.client = client;
        }
        public AmazonS3Client getClient() {
            return client;
        }
    }
    
    @Produces
    @Singleton
    public AWSClientProvider getClientProvider() throws Exception {
        AmazonS3Client client = null;
        String accessId = System.getenv("AWSAccessId");
        String secretKey = System.getenv("AWSSecretKey");
        if (accessId != null && secretKey != null) {
            client = (AmazonS3Client) AmazonS3ClientBuilder.standard()
                    .withCredentials(new BasicCredentialsProvider(accessId, secretKey))
                    .withRegion("eu-west-1")
                    .build();

            client.createBucket(S3IntegrationTest.BUCKET_NAME);
        }
        return new AWSClientProvider(client);
    }

    public void close(@Disposes AWSClientProvider provider) throws Exception {
        if (provider.getClient() != null) {
            provider.getClient().deleteBucket(S3IntegrationTest.BUCKET_NAME);
        }
    }
}
