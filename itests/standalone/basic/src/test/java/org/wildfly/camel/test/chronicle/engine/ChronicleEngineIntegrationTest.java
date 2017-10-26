/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.wildfly.camel.test.chronicle.engine;

import net.openhft.chronicle.core.io.Closeable;
import net.openhft.chronicle.engine.server.ServerEndpoint;
import net.openhft.chronicle.engine.tree.VanillaAssetTree;
import net.openhft.chronicle.network.TCPRegistry;
import net.openhft.chronicle.network.connection.TcpChannelHub;
import net.openhft.chronicle.wire.WireType;

import java.util.Map;
import java.util.UUID;

import org.apache.camel.CamelContext;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.chronicle.engine.ChronicleEngineConstants;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.impl.DefaultCamelContext;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assume;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.camel.test.common.utils.AvailablePortFinder;
import org.wildfly.camel.test.common.utils.EnvironmentUtils;
import org.wildfly.extension.camel.CamelAware;

@CamelAware
@RunWith(Arquillian.class)
@ServerSetup({ChronicleEngineIntegrationTest.ServerSetup.class})
public class ChronicleEngineIntegrationTest {

    static class ServerSetup implements ServerSetupTask {

        private VanillaAssetTree serverAssetTree;
        private ServerEndpoint serverEndpoint;

        @Override
        public void setup(ManagementClient managementClient, String containerId) throws Exception {
            int port = AvailablePortFinder.getNextAvailable();
            AvailablePortFinder.storeServerData("chronicle-engine-port.txt", port);

            String address = "localhost:" + port;
            serverAssetTree = new VanillaAssetTree().forTesting();
            serverEndpoint = new ServerEndpoint(address, serverAssetTree);
        }

        @Override
        public void tearDown(ManagementClient managementClient, String containerId) throws Exception {
            Closeable.closeQuietly(serverAssetTree);
            Closeable.closeQuietly(serverEndpoint);
            TcpChannelHub.closeAllHubs();
            TCPRegistry.reset();
        }
    }

    @Deployment
    public static JavaArchive createDeployment() {
        return ShrinkWrap.create(JavaArchive.class, "camel-chronicle-engine-tests.jar")
            .addClasses(AvailablePortFinder.class, EnvironmentUtils.class);
    }

    @Test
    public void testMapEvents() throws Exception {
        Assume.assumeFalse("[#2240] ChronicleEngineIntegrationTest fails on IBM JDK", EnvironmentUtils.isIbmJDK());

        CamelContext camelctx = new DefaultCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                fromF("chronicle-engine://%s/my/path?persistent=false", getAddress())
                .to("mock:map-events");
            }
        });

        VanillaAssetTree client = new VanillaAssetTree().forRemoteAccess(getAddress(), WireType.TEXT);

        camelctx.start();
        try {
            String key = UUID.randomUUID().toString();

            MockEndpoint mockEndpoint = camelctx.getEndpoint("mock:map-events", MockEndpoint.class);
            mockEndpoint.expectedMessageCount(1);
            mockEndpoint.expectedBodiesReceived("val");
            mockEndpoint.expectedHeaderReceived(ChronicleEngineConstants.PATH, "/my/path");
            mockEndpoint.expectedHeaderReceived(ChronicleEngineConstants.KEY, key);

            Map<String, String> map = client.acquireMap("/my/path", String.class, String.class);
            map.put(key, "val");

            mockEndpoint.assertIsSatisfied();
        } finally {
            client.close();
            camelctx.stop();
        }
    }

    private String getAddress() {
        return String.format("localhost:%s", AvailablePortFinder.readServerData("chronicle-engine-port.txt"));
    }
}
