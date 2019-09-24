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
package org.wildfly.camel.test.google;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import com.google.api.services.pubsub.Pubsub;
import com.google.api.services.pubsub.model.Subscription;
import com.google.api.services.pubsub.model.Topic;

import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.google.pubsub.GooglePubsubComponent;
import org.apache.camel.component.google.pubsub.GooglePubsubConnectionFactory;
import org.apache.camel.component.google.pubsub.GooglePubsubConstants;
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
public class PubSubIntegrationTest {

    private static final String CONTAINER_NAME = "google_pubsub";

    @ArquillianResource
    private CubeController cubeController;

    @Deployment
    public static JavaArchive createDeployment() {
        return ShrinkWrap.create(JavaArchive.class, "camel-google-pubsub-tests.jar")
            .addClass(TestUtils.class)
            .addAsResource("google/pubsub.properties", "pubsub.properties");
    }

    @Before
    public void setUp() throws Exception {
        cubeController.create(CONTAINER_NAME);
        cubeController.start(CONTAINER_NAME);
    }

    @After
    public void tearDown() throws Exception {
        cubeController.stop(CONTAINER_NAME);
        cubeController.destroy(CONTAINER_NAME);
    }

    @Test
    public void testGooglePubSubComponent() throws Exception {
        Properties properties = new Properties();
        properties.load(PubSubIntegrationTest.class.getResourceAsStream("/pubsub.properties"));

        String serviceURL = properties.getProperty("test.serviceURL");
        if (System.getenv("DOCKER_HOST") != null) {
            serviceURL = String.format("http://%s:8590", TestUtils.getDockerHost());
        }

        GooglePubsubConnectionFactory connectionFactory = new GooglePubsubConnectionFactory()
            .setServiceAccount(properties.getProperty("service.account"))
            .setServiceAccountKey(properties.getProperty("service.key"))
            .setServiceURL(serviceURL);

        String topicFullName = String.format("projects/%s/topics/%s",
            properties.getProperty("project.id"),
            properties.getProperty("topic.name"));

        String subscriptionFullName = String.format("projects/%s/subscriptions/%s",
            properties.getProperty("project.id"),
            properties.getProperty("subscription.name"));

        Pubsub pubsub = connectionFactory.getDefaultClient();
        pubsub.projects().topics().create(topicFullName, new Topic()).execute();

        Subscription subscription = new Subscription().setTopic(topicFullName).setAckDeadlineSeconds(10);
        pubsub.projects().subscriptions().create(subscriptionFullName, subscription).execute();

        CamelContext camelctx = new DefaultCamelContext();

        GooglePubsubComponent component = camelctx.getComponent("google-pubsub", GooglePubsubComponent.class);
        component.setConnectionFactory(connectionFactory);

        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:send")
                .toF("google-pubsub:%s:%s", properties.getProperty("project.id"), properties.getProperty("topic.name"));

                fromF("google-pubsub:%s:%s", properties.getProperty("project.id"), properties.getProperty("subscription.name"))
                .to("direct:receive");

                from("direct:receive")
                .to("mock:result");
            }
        });

        MockEndpoint mockEndpoint = camelctx.getEndpoint("mock:result", MockEndpoint.class);
        mockEndpoint.expectedBodiesReceivedInAnyOrder("Hello Kermit");

        camelctx.start();
        try {
            Map<String, String> attributes = new HashMap<>();
            attributes.put("ATTRIBUTE-TEST-KEY", "ATTRIBUTE-TEST-VALUE");

            ProducerTemplate template = camelctx.createProducerTemplate();
            template.sendBodyAndHeader("direct:send", "Hello Kermit", GooglePubsubConstants.ATTRIBUTES, attributes);

            mockEndpoint.assertIsSatisfied();
        } finally {
            camelctx.close();
            pubsub.projects().topics().delete(topicFullName).execute();
            pubsub.projects().subscriptions().delete(subscriptionFullName).execute();
        }
    }
}
