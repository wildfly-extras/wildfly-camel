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

package org.wildfly.camel.test.smpp;

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
import org.wildfly.camel.test.common.utils.TestUtils;
import org.wildfly.camel.test.dockerjava.DockerManager;
import org.wildfly.extension.camel.CamelAware;

@CamelAware
@RunWith(Arquillian.class)
@ServerSetup({SMPPIntegrationTest.ContainerSetupTask.class})
public class SMPPIntegrationTest {

    private static final String CONTAINER_NAME = "smpp_simulator";

    @Deployment
    public static JavaArchive createDeployment() {
        return ShrinkWrap.create(JavaArchive.class, "camel-smpp-tests")
            .addClass(TestUtils.class);
    }

    static class ContainerSetupTask implements ServerSetupTask {

    	private DockerManager dockerManager;

        @Override
        public void setup(ManagementClient managementClient, String someId) throws Exception {
        	
            String dockerHost = TestUtils.getDockerHost();
            
			/*
			docker run --detach \
				--name smpp_simulator \
				-p 2775:2775 \
				-p 8888:88 \
				wildflyext/smppsimulator:2.6.11
			*/
        	
        	dockerManager = new DockerManager()
        			.createContainer("wildflyext/smppsimulator:2.6.11", true)
        			.withName(CONTAINER_NAME)
        			.withPortBindings("2775:2775", "8888:88")
        			.startContainer();

			dockerManager
				.withAwaitHttp("http://" + dockerHost + ":8888")
				.withResponseCode(200)
				.withSleepPolling(500)
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
    public void testSMPPComponent() throws Exception {
        
        try (CamelContext camelctx = new DefaultCamelContext()) {
            
            camelctx.addRoutes(new RouteBuilder() {
                @Override
                public void configure() throws Exception {
                    from("direct:start")
                    .to("smpp://smppuser@" + TestUtils.getDockerHost() + ":2775?password=password&systemType=producer");

                    from("smpp://smppuser@" + TestUtils.getDockerHost() + ":2775?password=password&systemType=consumer")
                    .setBody(simple("Hello ${body}"))
                    .to("mock:result");
                }
            });

            MockEndpoint mockEndpoint = camelctx.getEndpoint("mock:result", MockEndpoint.class);
            mockEndpoint.expectedBodiesReceived("Hello Kermit");

            camelctx.start();
            
            ProducerTemplate template = camelctx.createProducerTemplate();
            template.sendBody("direct:start", "Kermit");
            mockEndpoint.assertIsSatisfied();
        }
    }
}
