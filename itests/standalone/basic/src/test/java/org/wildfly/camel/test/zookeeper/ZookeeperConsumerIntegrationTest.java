/*
 * #%L
 * Wildfly Camel :: Testsuite
 * %%
 * Copyright (C) 2013 - 2014 RedHat
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

package org.wildfly.camel.test.zookeeper;

import java.util.concurrent.TimeUnit;

import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.impl.DefaultCamelContext;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.camel.test.common.utils.AvailablePortFinder;
import org.wildfly.camel.test.common.zookeeper.EmbeddedZookeeper;
import org.wildfly.extension.camel.CamelAware;

@CamelAware
@RunWith(Arquillian.class)
@ServerSetup({ ZookeeperConsumerIntegrationTest.EmbeddedZookeeperSetupTask.class })
public class ZookeeperConsumerIntegrationTest {


    static class EmbeddedZookeeperSetupTask implements ServerSetupTask {

        EmbeddedZookeeper server;

        @Override
        public void setup(final ManagementClient managementClient, String containerId) throws Exception {
            server = new EmbeddedZookeeper().startup(10, TimeUnit.SECONDS);
            AvailablePortFinder.storeServerData("zkcon", server.getConnection());
        }

        @Override
        public void tearDown(final ManagementClient managementClient, String containerId) throws Exception {
            if (server != null) {
                server.shutdown();
            }
        }
    }

    @Deployment
    public static JavaArchive deployment() {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "zookeeper-integration-tests");
        archive.addClasses(AvailablePortFinder.class);
        return archive;
    }

    @Test
    public void testZookeeperConsumer() throws Exception {

        String zkcon = AvailablePortFinder.readServerData("zkcon");

        CamelContext camelctx = new DefaultCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:start")
                .to("zookeeper://" + zkcon + "/somenode")
                .to("mock:end");
            }
        });

        MockEndpoint mockEndpoint = camelctx.getEndpoint("mock:end", MockEndpoint.class);
        mockEndpoint.expectedBodiesReceived("Kermit");
        mockEndpoint.expectedMessageCount(1);

        camelctx.start();
        try {
            // Write payload to znode
            ProducerTemplate producer = camelctx.createProducerTemplate();
            producer.sendBody("direct:start", "Kermit");

            // Read back from the znode
            mockEndpoint.assertIsSatisfied(5000);
        } finally {
            camelctx.close();
        }
    }
}
