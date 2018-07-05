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
package org.wildfly.camel.test.consul;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.apache.camel.CamelContext;
import org.apache.camel.FluentProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.cloud.ServiceDefinition;
import org.apache.camel.cloud.ServiceDiscovery;
import org.apache.camel.component.consul.ConsulComponent;
import org.apache.camel.component.consul.ConsulConfiguration;
import org.apache.camel.component.consul.ConsulConstants;
import org.apache.camel.component.consul.cloud.ConsulServiceDiscovery;
import org.apache.camel.component.consul.endpoint.ConsulCatalogActions;
import org.apache.camel.impl.DefaultCamelContext;
import org.arquillian.cube.CubeController;
import org.arquillian.cube.docker.impl.requirement.RequiresDocker;
import org.arquillian.cube.requirement.ArquillianConditionalRunner;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.camel.test.common.utils.TestUtils;
import org.wildfly.extension.camel.CamelAware;

import com.orbitz.consul.AgentClient;
import com.orbitz.consul.Consul;
import com.orbitz.consul.model.agent.ImmutableRegistration;
import com.orbitz.consul.model.agent.Registration;
import com.orbitz.consul.model.health.Node;

@CamelAware
@RunWith(ArquillianConditionalRunner.class)
@RequiresDocker
public class ConsulIntegrationTest {

    private static final String CONTAINER_NAME = "consul";
    private static final int CONSUL_PORT = 48802;

    private String consulUrl;
    private List<Registration> registrations;

    @ArquillianResource
    private CubeController cubeController;

    @Deployment
    public static JavaArchive createDeployment() {
        return ShrinkWrap.create(JavaArchive.class, "camel-consul-tests.jar")
            .addClass(TestUtils.class);
    }

    @Before
    public void setUp() throws Exception {
        cubeController.create(CONTAINER_NAME);
        cubeController.start(CONTAINER_NAME);
        consulUrl = "http://"+ TestUtils.getDockerHost() +":"+ CONSUL_PORT;
    }

    @After
    public void tearDown() throws Exception {
        cubeController.stop(CONTAINER_NAME);
        cubeController.destroy(CONTAINER_NAME);
    }

    @Test
    public void testListDatacenters() throws Exception {
        List<String> ref = getConsul().catalogClient().getDatacenters();

        CamelContext camelctx = new DefaultCamelContext();
        camelctx.getComponent("consul", ConsulComponent.class).getConfiguration().setUrl(consulUrl);
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:consul")
                .to("consul:catalog");
            }
        });

        camelctx.start();
        try {
            List<?> res = fluentTemplate(camelctx)
                    .withHeader(ConsulConstants.CONSUL_ACTION, ConsulCatalogActions.LIST_DATACENTERS)
                    .to("direct:consul")
                    .request(List.class);

            Assert.assertFalse(ref.isEmpty());
            Assert.assertFalse(res.isEmpty());
            Assert.assertEquals(ref, res);
        } finally {
            camelctx.stop();
        }
    }

    @Test
    public void testListNodes() throws Exception {
        List<Node> ref = getConsul().catalogClient().getNodes().getResponse();

        CamelContext camelctx = new DefaultCamelContext();
        camelctx.getComponent("consul", ConsulComponent.class).getConfiguration().setUrl(consulUrl);
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:consul")
                .to("consul:catalog");
            }
        });

        camelctx.start();
        try {
            List<?> res = fluentTemplate(camelctx)
                    .withHeader(ConsulConstants.CONSUL_ACTION, ConsulCatalogActions.LIST_NODES)
                    .to("direct:consul")
                    .request(List.class);

            Assert.assertFalse(ref.isEmpty());
            Assert.assertFalse(res.isEmpty());
            Assert.assertEquals(ref, res);
        } finally {
            camelctx.stop();
        }
    }

    @Test
    public void testServiceDiscovery() throws Exception {
        final AgentClient client = getConsul().agentClient();
        try {
            registrations = new ArrayList<>(3);

            for (int i = 0; i < 3; i++) {
                Registration r = ImmutableRegistration.builder()
                    .id("service-" + i)
                    .name("my-service")
                    .address("127.0.0.1")
                    .addTags("a-tag")
                    .addTags("key1=value1")
                    .addTags("key2=value2")
                    .port(9000 + i)
                    .build();

                client.register(r);
                registrations.add(r);
            }

            ConsulConfiguration configuration = new ConsulConfiguration();
            configuration.setUrl(consulUrl);
            ServiceDiscovery discovery = new ConsulServiceDiscovery(configuration);

            List<ServiceDefinition> services = discovery.getServices("my-service");
            assertNotNull(services);
            assertEquals(3, services.size());

            for (ServiceDefinition service : services) {
                assertFalse(service.getMetadata().isEmpty());
                assertTrue(service.getMetadata().containsKey("service.name"));
                assertTrue(service.getMetadata().containsKey("service.id"));
                assertTrue(service.getMetadata().containsKey("service.node"));
                assertTrue(service.getMetadata().containsKey("a-tag"));
                assertTrue(service.getMetadata().containsKey("key1"));
                assertTrue(service.getMetadata().containsKey("key2"));
            }
        } finally {
            if (registrations != null && client != null) {
                registrations.forEach(r -> client.deregister(r.getId()));
            }
        }
    }

    private FluentProducerTemplate fluentTemplate(CamelContext camelctx) throws Exception {
        FluentProducerTemplate fluentTemplate = camelctx.createFluentProducerTemplate();
        fluentTemplate.start();
        return fluentTemplate;
    }

    private Consul getConsul() {
        return Consul.builder().withUrl(consulUrl).build();
    }
}
