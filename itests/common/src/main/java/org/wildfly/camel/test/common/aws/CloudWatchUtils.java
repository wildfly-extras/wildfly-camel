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

import com.amazonaws.services.cloudwatch.AmazonCloudWatchClient;
import com.amazonaws.services.cloudwatch.AmazonCloudWatchClientBuilder;

public class CloudWatchUtils {

    private static final String SUFFIX = "-id" + CloudWatchUtils.class.getClassLoader().hashCode();
    
    public static final String NAMESPACE = "MySpace" + SUFFIX;
    public static final String NAME = "MyMetric" + CloudWatchUtils.SUFFIX;
    public static final String DIM_NAME = "MyDimName" + CloudWatchUtils.SUFFIX;
    public static final String DIM_VALUE = "MyDimValue" + CloudWatchUtils.SUFFIX;

    public static AmazonCloudWatchClient createCloudWatchClient() {
        BasicCredentialsProvider credentials = BasicCredentialsProvider.standard();
        AmazonCloudWatchClient client = !credentials.isValid() ? null : (AmazonCloudWatchClient) 
                AmazonCloudWatchClientBuilder.standard()
                .withCredentials(credentials)
                .withRegion("eu-west-1")
                .build();
        return client;
    }
}
