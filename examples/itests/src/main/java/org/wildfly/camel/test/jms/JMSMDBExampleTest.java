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

import java.io.File;
import java.io.IOException;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.camel.test.common.ServerLogReader;
import org.wildfly.camel.test.common.utils.JMSUtils;

@RunAsClient
@RunWith(Arquillian.class)
@ServerSetup({ JMSMDBExampleTest.JmsQueueSetup.class })
public class JMSMDBExampleTest {

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

    @Deployment(testable = false)
    public static WebArchive createDeployment() {
        return ShrinkWrap.createFromZipFile(WebArchive.class, new File("target/examples/example-camel-jms-mdb.war"));
    }

    @Test
    public void testJMSMDBRoute() throws IOException {
        boolean logMessagePresent = ServerLogReader.awaitLogMessage("Received message", 10000);
        Assert.assertTrue("Message 'Received message' not found in server log", logMessagePresent);
    }
}
