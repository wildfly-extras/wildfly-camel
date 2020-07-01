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
package org.wildfly.camel.test.etcd;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.etcd.EtcdConstants;
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
@ServerSetup({EtcdIntegrationTest.ContainerSetupTask.class})
public class EtcdIntegrationTest {

    private static final String CONTAINER_NAME = "etcd";

    @Deployment
    public static JavaArchive createDeployment() {
        return ShrinkWrap.create(JavaArchive.class, "camel-etcd-tests.jar")
            .addClass(TestUtils.class);
    }

    static class ContainerSetupTask implements ServerSetupTask {

    	private DockerManager dockerManager;

        @Override
        public void setup(ManagementClient managementClient, String someId) throws Exception {
        	
            String dockerHost = TestUtils.getDockerHost();
            
			/*
			docker run --detach \
				--name etcd \
				-p 2379:2379 \
				-p 2380:2380 \
				-p 4001:4001 \
				quay.io/coreos/etcd:v2.2.5 \
				-listen-client-urls http://0.0.0.0:2379,http://0.0.0.0:4001 \
				-listen-peer-urls http://0.0.0.0:2380 \
				-advertise-client-urls http://localhost:2379,http://localhost:4001
			*/
        	
        	dockerManager = new DockerManager()
        			.createContainer("quay.io/coreos/etcd:v2.2.5", true)
        			.withName(CONTAINER_NAME)
        			.withPortBindings("2379:2379", "2380:2380", "4001:4001")
        			.withCmd("-listen-client-urls http://0.0.0.0:2379,http://0.0.0.0:4001 -listen-peer-urls http://0.0.0.0:2380 -advertise-client-urls http://localhost:2379,http://localhost:4001")
        			.startContainer();

			dockerManager
				.withAwaitHttp("http://" + dockerHost + ":2379/health")
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
    public void testEtcdComponent() throws Exception {
        DefaultCamelContext camelctx = new DefaultCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                String dockerHost = TestUtils.getDockerHost();

                from("direct:start")
                .toF("etcd-keys:camel?uris=http://%s:2379,http://%s:40001", dockerHost, dockerHost)
                .to("mock:result");
            }
        });

        String path = "/camel/" + UUID.randomUUID().toString();
        String value = UUID.randomUUID().toString();

        Map<String, Object> headers = new HashMap<>();
        headers.put(EtcdConstants.ETCD_ACTION, EtcdConstants.ETCD_KEYS_ACTION_SET);
        headers.put(EtcdConstants.ETCD_PATH, path);

        MockEndpoint mockEndpoint = camelctx.getEndpoint("mock:result", MockEndpoint.class);
        mockEndpoint.expectedMinimumMessageCount(1);
        mockEndpoint.expectedHeaderReceived(EtcdConstants.ETCD_PATH, path);

        camelctx.start();
        try {
            ProducerTemplate template = camelctx.createProducerTemplate();
            template.sendBodyAndHeaders("direct:start", value, headers);
            mockEndpoint.assertIsSatisfied();
        } finally {
            camelctx.close();
        }
    }
}
