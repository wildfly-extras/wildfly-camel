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
package org.wildfly.camel.test.kubernetes.subA;

import java.io.IOException;
import java.nio.ByteBuffer;

import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

import org.wildfly.camel.test.common.utils.TestUtils;

/**
 * Simulates a Kubernetes API WebSocket endpoint to return a pod watch event
 */

@ServerEndpoint("/fake-kubernetes/api/v1/namespaces/{namespace}/pods")
public class FakeKubernetesWebSocketA {

    @OnOpen
    public void onConnectionOpen(Session session) {
        try {
            String resource = TestUtils.getResourceValue(FakeKubernetesWebSocketA.class, "/event.json");
            session.getBasicRemote().sendBinary(ByteBuffer.wrap(resource.getBytes()));
        } catch (IOException e) {
            throw new IllegalStateException("Unable to read resource: event.json", e);
        }
    }
}
