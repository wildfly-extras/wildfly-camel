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

import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.etcd.EtcdConstants;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.impl.DefaultCamelContext;
import org.arquillian.cube.CubeController;
import org.arquillian.cube.docker.impl.requirement.RequiresDocker;
import org.arquillian.cube.requirement.ArquillianConditionalRunner;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.camel.test.common.utils.TestUtils;
import org.wildfly.extension.camel.CamelAware;

@CamelAware
@RunWith(ArquillianConditionalRunner.class)
@RequiresDocker
@Ignore("[CAMEL-14492] EtcdKeysEndpoint cannot be cast to AbstractEtcdPollingEndpoint")
public class EtcdIntegrationTest {

    private static final String CONTAINER_ETCD = "etcd";

    @ArquillianResource
    private CubeController cubeController;

    @Deployment
    public static JavaArchive createDeployment() {
        return ShrinkWrap.create(JavaArchive.class, "camel-etcd-tests.jar")
            .addClass(TestUtils.class);
    }

    @Before
    public void setUp() throws Exception {
        cubeController.create(CONTAINER_ETCD);
        cubeController.start(CONTAINER_ETCD);
    }

    @After
    public void tearDown() throws Exception {
        cubeController.stop(CONTAINER_ETCD);
        cubeController.destroy(CONTAINER_ETCD);
    }

    @Test
    public void testEtcdComponent() throws Exception {
        DefaultCamelContext camelctx = new DefaultCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                String dockerHost = TestUtils.getDockerHost();

                from("direct:start")
                .toF("etcd:keys?uris=http://%s:23379,http://%s:40001", dockerHost, dockerHost)
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
