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
package org.wildfly.camel.test.jms;

import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.junit.Assert;
import org.junit.Test;
import org.wildfly.camel.test.common.FileConsumingTestSupport;
import org.wildfly.camel.test.common.http.HttpRequest;
import org.wildfly.camel.test.common.utils.JMSUtils;

public abstract class AbstractJMSExampleTest extends FileConsumingTestSupport {

    private static String ORDERS_QUEUE = "OrdersQueue";
    private static String ORDERS_QUEUE_JNDI = "java:/jms/queue/OrdersQueue";

    static class JmsQueueSetup implements ServerSetupTask {

        @Override
        public void setup(ManagementClient managementClient, String containerId) throws Exception {
            JMSUtils.createJmsQueue(ORDERS_QUEUE, ORDERS_QUEUE_JNDI, managementClient.getControllerClient());
        }

        @Override
        public void tearDown(ManagementClient managementClient, String containerId) throws Exception {
            JMSUtils.removeJmsQueue(ORDERS_QUEUE, managementClient.getControllerClient());
        }
    }

    @Test
    public void testFileToJmsRoute() throws Exception {
        HttpRequest.HttpResponse result = HttpRequest.get("http://localhost:8080/" + getContextPath() + "/orders").getResponse();
        String body = result.getBody();
        Assert.assertTrue("Unexpected response body: " + body, body.contains(getExpectedResponseText()));
    }

    abstract String getContextPath();
    abstract String getExpectedResponseText();
}
