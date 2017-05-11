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

import org.wildfly.camel.test.common.aws.SQSUtils;

import com.amazonaws.services.sqs.AmazonSQSClient;

public class SQSClientProducer {

    public class SQSClientProvider {
        private final AmazonSQSClient client;
        SQSClientProvider(AmazonSQSClient client) {
            this.client = client;
        }
        public AmazonSQSClient getClient() {
            return client;
        }
    }
    
    @Produces
    @Singleton
    public SQSClientProvider getClientProvider() throws Exception {
        AmazonSQSClient client = SQSUtils.createSQSClient();
        if (client != null) {
            SQSUtils.createQueue(client);
        }
        return new SQSClientProvider(client);
    }

    public void close(@Disposes SQSClientProvider provider) throws Exception {
        AmazonSQSClient client = provider.getClient();
        if (client != null) {
            SQSUtils.deleteQueue(client);
        }
    }
}
