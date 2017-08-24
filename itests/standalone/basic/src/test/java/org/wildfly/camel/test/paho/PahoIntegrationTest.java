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

package org.wildfly.camel.test.paho;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.activemq.broker.BrokerService;
import org.apache.camel.CamelContext;
import org.apache.camel.PollingConsumer;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.impl.DefaultCamelContext;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
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
import org.wildfly.camel.test.common.utils.TestUtils;
import org.wildfly.extension.camel.CamelAware;

@CamelAware
@RunWith(Arquillian.class)
@ServerSetup({ PahoIntegrationTest.BrokerSetup.class })
public class PahoIntegrationTest {

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
        archive.addClasses(TestUtils.class);
        archive.addAsResource(new StringAsset(BrokerSetup.TCP_CONNECTION), "tcp-connection");
        return archive;
    }

    @Test
    public void testPahoConsumer() throws Exception {
        
        String conUrl = TestUtils.getResourceValue(getClass(), "/tcp-connection");
        
        CamelContext camelctx = new DefaultCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("paho:" + BrokerSetup.TEST_TOPIC + "?brokerUrl=" + conUrl).
                transform(body().prepend("Hello ")).to("seda:end");
            }
        });

        camelctx.start();
        try {
            PollingConsumer consumer = camelctx.getEndpoint("seda:end").createPollingConsumer();
            consumer.start();
            
            MqttClient client = null;
            try {
                client = new MqttClient(conUrl, "MqttClient", new MemoryPersistence());
                MqttConnectOptions opts = new MqttConnectOptions();
                opts.setCleanSession(true);
                client.connect(opts);
                MqttMessage message = new MqttMessage("Kermit".getBytes());
                message.setQos(2);
                client.publish(BrokerSetup.TEST_TOPIC, message);
            } finally {
                client.disconnect();
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
                to("paho:" + BrokerSetup.TEST_TOPIC + "?brokerUrl=" + conUrl);
            }
        });

        camelctx.start();
        try {
            MqttClient client = new MqttClient(conUrl, "MqttClient", new MemoryPersistence());
            MqttConnectOptions opts = new MqttConnectOptions();
            opts.setCleanSession(true);
            client.connect(opts);
            client.subscribe(BrokerSetup.TEST_TOPIC, 2);

            final List<String> result = new ArrayList<>();
            final CountDownLatch latch = new CountDownLatch(1);

            client.setCallback(new MqttCallback() {

                @Override
                public void connectionLost(Throwable cause) {
                }

                @Override
                public void messageArrived(String topic, MqttMessage message) throws Exception {
                    result.add(new String (message.getPayload()));
                    latch.countDown();
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {
                }});

            ProducerTemplate producer = camelctx.createProducerTemplate();
            producer.asyncSendBody("direct:start", "Kermit");

            Assert.assertTrue(latch.await(10, TimeUnit.SECONDS));
            Assert.assertEquals("One message", 1, result.size());
            Assert.assertEquals("Hello Kermit", result.get(0));
        } finally {
            camelctx.stop();
        }
    }
}
