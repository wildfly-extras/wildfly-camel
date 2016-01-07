/*
* #%L
* Wildfly Camel :: Testsuite
* %%
* Copyright (C) 2013 - 2015 RedHat
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
package org.wildfly.camel.test.common.utils;

import java.io.IOException;

import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.dmr.ModelNode;

public class JMSUtils {

    public static ModelNode createJmsQueue(String queueName, String jndiName, ModelControllerClient client) throws IOException {
        ModelNode modelNode = createJmsQueueModelNode("add", queueName, jndiName, client);
        return executeModelNode(client, modelNode);
    }

    public static ModelNode removeJmsQueue(String queueName, ModelControllerClient client) throws IOException {
        ModelNode modelNode = createJmsQueueModelNode("remove", queueName, null, client);
        return executeModelNode(client, modelNode);
    }

    private static ModelNode executeModelNode(ModelControllerClient client, ModelNode modelNode) throws IOException {
        ModelNode result = client.execute(modelNode);
        return result;
    }

    private static ModelNode createJmsQueueModelNode(String operationName, String queueName, String jndiName, ModelControllerClient client) {
        ModelNode modelNode = new ModelNode();
        modelNode.get("operation").set(operationName);

        modelNode.get("address").add("subsystem", MessagingSubsystem.ACTIVEMQ_ARTEMIS.getSubsystemName());
        modelNode.get("address").add(MessagingSubsystem.ACTIVEMQ_ARTEMIS.getServerName(), "default");
        modelNode.get("address").add("jms-queue", queueName);

        if (jndiName != null) {
            modelNode.get("entries").add(jndiName);
        }
        return modelNode;
    }

    private enum MessagingSubsystem {
        ACTIVEMQ_ARTEMIS("messaging-activemq", "server");

        private final String subsystemName;
        private final String serverName;

        MessagingSubsystem(String subsystemName, String serverName) {
            this.subsystemName = subsystemName;
            this.serverName = serverName;
        }

        public String getSubsystemName() {
            return subsystemName;
        }

        public String getServerName() {
            return serverName;
        }
    }
}
