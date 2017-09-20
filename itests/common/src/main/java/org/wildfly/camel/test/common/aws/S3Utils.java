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

import org.apache.camel.CamelContext;
import org.apache.camel.builder.RouteBuilder;

import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.HeadBucketRequest;
import com.amazonaws.waiters.NoOpWaiterHandler;
import com.amazonaws.waiters.Waiter;
import com.amazonaws.waiters.WaiterParameters;

public class S3Utils {

    private static final String SUFFIX = "-id" + S3Utils.class.getClassLoader().hashCode();
    
    public static final String BUCKET_NAME = "wfc-bucket" + SUFFIX;
    
    // Attach Policy: AmazonS3FullAccess
    public static AmazonS3Client createS3Client() {
        BasicCredentialsProvider credentials = BasicCredentialsProvider.standard();
        AmazonS3Client client = !credentials.isValid() ? null : (AmazonS3Client) 
                AmazonS3ClientBuilder.standard()
                .withCredentials(credentials)
                .withRegion("eu-west-1")
                .build();
        return client;
    }

    public static void addRoutes(CamelContext camelctx) throws Exception {
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                String clientref = "amazonS3Client=#s3Client";
                from("direct:upload").to("aws-s3://" + BUCKET_NAME + "?" + clientref);
                from("aws-s3://" + BUCKET_NAME + "?" + clientref).to("seda:read");
            }
        });
    }

    @SuppressWarnings("unchecked")
    public static void createBucket(AmazonS3Client client) throws Exception {

        client.createBucket(BUCKET_NAME);
        
        HeadBucketRequest request = new HeadBucketRequest(BUCKET_NAME);
        Waiter<HeadBucketRequest> waiter = client.waiters().bucketExists();
        Future<Void> future = waiter.runAsync(new WaiterParameters<HeadBucketRequest>(request), new NoOpWaiterHandler());
        future.get(1, TimeUnit.MINUTES);
    }

    public static void deleteBucket(AmazonS3Client client) throws Exception {
        client.deleteBucket(BUCKET_NAME);
    }
}
