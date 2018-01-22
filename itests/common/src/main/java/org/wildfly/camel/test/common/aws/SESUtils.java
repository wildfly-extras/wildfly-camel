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

import com.amazonaws.services.simpleemail.AmazonSimpleEmailServiceClient;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailServiceClientBuilder;

public class SESUtils {

    public static final String SUBJECT = "[wfc-aws-ses] Test Subject";
    public static final String FROM = "tdiesler@redhat.com";
    public static final String TO = "tdiesler@redhat.com";

    // Attach Policy: AmazonSESFullAccess
    public static AmazonSimpleEmailServiceClient createEmailClient() {
        BasicCredentialsProvider credentials = BasicCredentialsProvider.standard();
        AmazonSimpleEmailServiceClient client = !credentials.isValid() ? null : (AmazonSimpleEmailServiceClient)
                AmazonSimpleEmailServiceClientBuilder.standard()
                .withCredentials(credentials)
                .withRegion("eu-west-1")
                .build();
        return client;
    }

}
