/*
 * #%L
 * Wildfly Camel :: Testsuite
 * %%
 * Copyright (C) 2013 - 2019 RedHat
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
package org.wildfly.camel.test.nsq;

import java.util.concurrent.TimeUnit;

import org.apache.camel.CamelContext;
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
import org.junit.Assume;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.camel.test.common.utils.EnvironmentUtils;
import org.wildfly.camel.test.common.utils.TestUtils;
import org.wildfly.camel.test.dockerjava.DockerManager;
import org.wildfly.extension.camel.CamelAware;

import com.github.brainlag.nsq.NSQProducer;

@CamelAware
@RunWith(Arquillian.class)
@ServerSetup({NsqIntegrationTest.ContainerSetupTask.class})
public class NsqIntegrationTest {

    private static final String TOPIC_NAME = "wfc-topic";

    @Deployment
    public static JavaArchive createDeployment() {
        return ShrinkWrap.create(JavaArchive.class, "camel-nsq-tests.jar")
            .addClasses(TestUtils.class, EnvironmentUtils.class);
    }

    static class ContainerSetupTask implements ServerSetupTask {

    	private DockerManager dockerManager;

        @Override
        public void setup(ManagementClient managementClient, String someId) throws Exception {
        	
			/*
			docker run --detach \
				--name nsqlookupd \
				--network=host \
				nsqio/nsq:v1.1.0 /nsqlookupd
			*/
        	
        	dockerManager = new DockerManager()
        			.createContainer("nsqio/nsq:v1.1.0", true)
        			.withName("nsqlookupd")
        			.withNetworkMode("host")
        			.withCmd("/nsqlookupd")
        			.startContainer();

			dockerManager
					.withAwaitLogMessage("TCP: listening")
					.awaitCompletion(60, TimeUnit.SECONDS);
        	
			/*
			docker run --detach \
				--name nsq \
				--network=host \
				nsqio/nsq:v1.1.0 /nsqd --broadcast-address 127.0.0.1 --lookupd-tcp-address 127.0.0.1:4160
			*/
        	
        	dockerManager
        			.createContainer("nsqio/nsq:v1.1.0", true)
        			.withName("nsq")
        			.withNetworkMode("host")
        			.withCmd("/nsqd --broadcast-address 127.0.0.1 --lookupd-tcp-address 127.0.0.1:4160")
        			.startContainer();

			dockerManager
					.withAwaitLogMessage("TCP: listening")
					.awaitCompletion(60, TimeUnit.SECONDS);
        }

        @Override
        public void tearDown(ManagementClient managementClient, String someId) throws Exception {
        	if (dockerManager != null) {
            	dockerManager.removeContainer("nsqlookupd");
            	dockerManager.removeContainer("nsq");
        	}
        }
    }

    @Test
    public void testNsqComponent() throws Exception {

        String dockerHost = TestUtils.getDockerHost();
        System.out.println("DockerHost: " + dockerHost);
        
        // Cannot run on Desktop Docker for Mac
        Assume.assumeFalse(EnvironmentUtils.isMac());

        CamelContext camelctx = new DefaultCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
				fromF("nsq:%s?servers=%s&port=4160&lookupInterval=2s&autoFinish=false&requeueInterval=1s", TOPIC_NAME, dockerHost)
                .to("mock:result");
            }
        });

        camelctx.start();
        try {
            MockEndpoint mockEndpoint = camelctx.getEndpoint("mock:result", MockEndpoint.class);
            mockEndpoint.expectedBodiesReceived("Hello Kermit");

            NSQProducer producer = new NSQProducer();
            producer.addAddress(dockerHost, 4160);
            producer.start();
            producer.produce(TOPIC_NAME, "Hello Kermit".getBytes());

            mockEndpoint.assertIsSatisfied(5000);
        } finally {
            camelctx.close();
        }
    }
}
