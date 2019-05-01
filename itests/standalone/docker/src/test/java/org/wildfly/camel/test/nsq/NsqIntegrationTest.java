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

import com.github.brainlag.nsq.NSQProducer;

import org.apache.camel.CamelContext;
import org.apache.camel.builder.RouteBuilder;
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
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.camel.test.common.utils.TestUtils;
import org.wildfly.extension.camel.CamelAware;

@CamelAware
@RunWith(ArquillianConditionalRunner.class)
@RequiresDocker
public class NsqIntegrationTest {

    private static final String[] CONTAINER_NAMES = {"nsqlookupd", "nsqd"};
    private static final String TOPIC_NAME = "wfc-topic";

    @ArquillianResource
    private CubeController cubeController;

    @Deployment
    public static JavaArchive createDeployment() {
        return ShrinkWrap.create(JavaArchive.class, "camel-nsq-tests.jar")
            .addClass(TestUtils.class);
    }

    @Before
    public void setUp() throws Exception {
        for (String container : CONTAINER_NAMES) {
            cubeController.create(container);
            cubeController.start(container);
        }
    }

    @After
    public void tearDown() throws Exception {
        for (String container : CONTAINER_NAMES) {
            cubeController.stop(container);
            cubeController.destroy(container);
        }
    }

    @Test
    public void testNsqComponent() throws Exception {
        CamelContext camelctx = new DefaultCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                fromF("nsq://%s:4161?topic=%s&lookupInterval=2s&autoFinish=false&requeueInterval=1s", TestUtils.getDockerHost(), TOPIC_NAME)
                .to("mock:result");
            }
        });

        camelctx.start();
        try {
            MockEndpoint mockEndpoint = camelctx.getEndpoint("mock:result", MockEndpoint.class);
            mockEndpoint.expectedBodiesReceived("Hello Kermit");

            NSQProducer producer = new NSQProducer();
            producer.addAddress(TestUtils.getDockerHost(), 4150);
            producer.start();
            producer.produce(TOPIC_NAME, "Hello Kermit".getBytes());

            mockEndpoint.assertIsSatisfied(5000);
        } finally {
            camelctx.stop();
        }
    }
}
