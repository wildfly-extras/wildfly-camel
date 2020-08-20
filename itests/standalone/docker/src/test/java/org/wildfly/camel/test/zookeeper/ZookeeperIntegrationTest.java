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
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.camel.test.common.utils.TestUtils;
import org.wildfly.camel.test.dockerjava.DockerManager;
import org.wildfly.extension.camel.CamelAware;

@CamelAware
@RunWith(Arquillian.class)
@ServerSetup({ ZookeeperIntegrationTest.ContainerSetupTask.class })
public class ZookeeperIntegrationTest {

    private static final int ZOOKEEPER_PORT = 2181;
    
    @Deployment
    public static JavaArchive deployment() {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "zookeeper-integration-tests");
        archive.addClasses(TestUtils.class);
        return archive;
    }

    static class ContainerSetupTask implements ServerSetupTask {

        private DockerManager dockerManager;

        @Override
        public void setup(ManagementClient managementClient, String someId) throws Exception {
            
            /*
            export ZKHOST=192.168.0.30
            export ZKPORT=2181

            docker run --detach \
                --name=zookeeper \
                -p $ZKPORT:$ZKPORT \
                -e ZOOKEEPER_CLIENT_PORT=$ZKPORT \
                confluentinc/cp-zookeeper:5.5.1
            */
            
            dockerManager = new DockerManager()
                    .createContainer("confluentinc/cp-zookeeper:5.5.1")
                    .withName("zookeeper")
                    .withPortBindings(ZOOKEEPER_PORT)
                    .withEnv("ZOOKEEPER_CLIENT_PORT=" + ZOOKEEPER_PORT)
                    .startContainer();

            dockerManager
                    .withAwaitLogMessage("binding to port 0.0.0.0/0.0.0.0:" + ZOOKEEPER_PORT)
                    .awaitCompletion(60, TimeUnit.SECONDS);
        }

        @Override
        public void tearDown(ManagementClient managementClient, String someId) throws Exception {
            if (dockerManager != null) {
                dockerManager.removeContainer();
            }
        }
    }

    @Test
    public void testZookeeperConsumer() throws Exception {

        String zkcon = getZookeeperConnection();

        try (CamelContext camelctx = new DefaultCamelContext()) {
        	
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
            
            // Write payload to znode
            ProducerTemplate producer = camelctx.createProducerTemplate();
            producer.sendBody("direct:start", "Kermit");

            // Read back from the znode
            mockEndpoint.assertIsSatisfied(5000);
        }
    }

    @Test
    public void testZookeeperProducer() throws Exception {

        String zkcon = getZookeeperConnection();

        try (CamelContext camelctx = new DefaultCamelContext()) {
        	
            camelctx.addRoutes(new RouteBuilder() {
                @Override
                public void configure() throws Exception {
                    from("direct:start").to("zookeeper://" + zkcon + "/somenode?create=true");
                }
            });

            camelctx.start();

            ProducerTemplate producer = camelctx.createProducerTemplate();
            String result = producer.requestBody("direct:start", "Kermit", String.class);
            Assert.assertEquals("/somenode", result);
        }
    }

    private String getZookeeperConnection() {
        try {
            String dockerHost = TestUtils.getDockerHost();
            return dockerHost + ":" + ZOOKEEPER_PORT;
        } catch (RuntimeException rte) {
            throw rte;
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }
}
