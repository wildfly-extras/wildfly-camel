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

package org.wildfly.camel.test.mqtt;

import java.util.concurrent.TimeUnit;

import org.apache.activemq.broker.BrokerService;
import org.apache.camel.CamelContext;
import org.apache.camel.PollingConsumer;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.impl.DefaultCamelContext;
import org.fusesource.mqtt.client.BlockingConnection;
import org.fusesource.mqtt.client.MQTT;
import org.fusesource.mqtt.client.Message;
import org.fusesource.mqtt.client.QoS;
import org.fusesource.mqtt.client.Topic;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.camel.test.common.utils.AvailablePortFinder;
import org.wildfly.camel.test.common.utils.ManifestBuilder;
import org.wildfly.camel.test.common.utils.TestUtils;
import org.wildfly.extension.camel.CamelAware;

@CamelAware
@RunWith(Arquillian.class)
@ServerSetup({ MQTTIntegrationTest.BrokerSetup.class })
public class MQTTIntegrationTest {

    static class BrokerSetup implements ServerSetupTask {

        static final int PORT = AvailablePortFinder.getNextAvailable();
        static final String MQTT_CONNECTION = "mqtt://127.0.0.1:" + PORT;
        static final String TCP_CONNECTION = "tcp://127.0.0.1:" + PORT;
        static final String TEST_TOPIC = "ComponentTestTopic";

        private BrokerService brokerService;

        @Override
        public void setup(ManagementClient managementClient, String containerId) throws Exception {
            brokerService = new BrokerService();
            brokerService.setPersistent(false);
            brokerService.setAdvisorySupport(false);
            brokerService.addConnector(MQTT_CONNECTION);
            brokerService.start();
        }

        @Override
        public void tearDown(ManagementClient managementClient, String containerId) throws Exception {
            brokerService.stop();
        }
    }

    @Deployment
    public static JavaArchive deployment() {
        JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "mqtt-tests");
        archive.addAsResource(new StringAsset(BrokerSetup.TCP_CONNECTION), "tcp-connection");
        archive.addClasses(TestUtils.class);
        archive.setManifest(() -> {
            ManifestBuilder builder = new ManifestBuilder();
            builder.addManifestHeader("Dependencies", "org.fusesource.mqtt");
            return builder.openStream();
        });
        return archive;
    }

    @Test
    public void testMQTTConsumer() throws Exception {

        String conUrl = TestUtils.getResourceValue(getClass(), "/tcp-connection");

        CamelContext camelctx = new DefaultCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("mqtt:bar?subscribeTopicName=" + BrokerSetup.TEST_TOPIC + "&host=" + conUrl).
                transform(body().prepend("Hello ")).to("seda:end");
            }
        });

        camelctx.start();

        PollingConsumer consumer = camelctx.getEndpoint("seda:end").createPollingConsumer();
        consumer.start();

        try {
            MQTT mqtt = new MQTT();
            mqtt.setHost(conUrl);
            BlockingConnection connection = mqtt.blockingConnection();
            Topic topic = new Topic(BrokerSetup.TEST_TOPIC, QoS.AT_MOST_ONCE);

            connection.connect();
            try {
                connection.publish(topic.name().toString(), "Kermit".getBytes(), QoS.AT_LEAST_ONCE, false);
            } finally {
                connection.disconnect();
            }

            String result = consumer.receive(3000).getIn().getBody(String.class);
            Assert.assertEquals("Hello Kermit", result);
        } finally {
            camelctx.stop();
        }
    }

    @Test
    public void testMQTTProducer() throws Exception {

        String conUrl = TestUtils.getResourceValue(getClass(), "/tcp-connection");

        CamelContext camelctx = new DefaultCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:start").
                transform(body().prepend("Hello ")).
                to("mqtt:foo?publishTopicName=" + BrokerSetup.TEST_TOPIC + "&host=" + conUrl);
            }
        });

        camelctx.start();
        try {
            MQTT mqtt = new MQTT();
            mqtt.setHost(conUrl);
            BlockingConnection connection = mqtt.blockingConnection();

            connection.connect();
            try {
                Topic topic = new Topic(BrokerSetup.TEST_TOPIC, QoS.AT_MOST_ONCE);
                connection.subscribe(new Topic[] { topic });

                ProducerTemplate producer = camelctx.createProducerTemplate();
                producer.asyncSendBody("direct:start", "Kermit");

                Message message = connection.receive(10, TimeUnit.SECONDS);
                message.ack();

                String result = new String(message.getPayload());
                Assert.assertEquals("Hello Kermit", result);
            } finally {
                connection.disconnect();
            }
        } finally {
            camelctx.stop();
        }
    }
}
