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
package org.wildfly.camel.test.kafka;

import java.time.Duration;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.kafka.KafkaConstants;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
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
@ServerSetup({KafkaIntegrationTest.ContainerSetupTask.class})
public class KafkaIntegrationTest {

    private static final String TOPIC_A = "wfc.test.topicA";
    private static final String TOPIC_B = "wfc.header.topicB";
    private static final String TOPIC_C = "wfc.test.topicC";

    private static final int KAFKA_PORT = 9092;
    private static final int ZOOKEEPER_PORT = 2181;

    @Deployment
    public static JavaArchive createDeployment() {
        return ShrinkWrap.create(JavaArchive.class, "camel-kafka-tests.jar")
            .addClasses(SimpleKafkaSerializer.class, TestUtils.class);
    }

    static class ContainerSetupTask implements ServerSetupTask {

    	private DockerManager dockerManager;

        @Override
        public void setup(ManagementClient managementClient, String someId) throws Exception {
        	
            String dockerHost = TestUtils.getDockerHost();
            
            /*
            export KAFKA_HOST=192.168.0.30
            export KAFKA_PORT=9092
            export ZKHOST=$KAFKA_HOST
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

            
            /*
            docker run --detach \
                --name=kafka \
                -p $KAFKA_PORT:$KAFKA_PORT \
                -e KAFKA_ZOOKEEPER_CONNECT=$ZKHOST:$ZKPORT \
                -e KAFKA_ADVERTISED_LISTENERS=PLAINTEXT://$KAFKA_HOST:$KAFKA_PORT \
                -e KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR=1 \
                -e KAFKA_BROKER_ID=1 \
                confluentinc/cp-kafka:5.5.1
            */
            
            dockerManager
                    .createContainer("confluentinc/cp-kafka:5.5.1")
                    .withName("kafka")
                    .withPortBindings(KAFKA_PORT)
                    .withEnv("KAFKA_ZOOKEEPER_CONNECT=" + dockerHost + ":" + ZOOKEEPER_PORT, 
                            "KAFKA_ADVERTISED_LISTENERS=PLAINTEXT://" + dockerHost + ":" + KAFKA_PORT,
                            "KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR=1",
                            "KAFKA_BROKER_ID=1")
                    .startContainer();

            dockerManager
                    .withAwaitLogMessage("started (kafka.server.KafkaServer)")
                    .awaitCompletion(60, TimeUnit.SECONDS);
        }

        @Override
        public void tearDown(ManagementClient managementClient, String someId) throws Exception {
        	if (dockerManager != null) {
                dockerManager.removeContainer("kafka");
                dockerManager.removeContainer("zookeeper");
        	}
        }
    }

    @Test
    public void testProducedStringMessageIsReceivedByKafka() throws Exception {

        try (CamelContext camelctx = new DefaultCamelContext()) {
        	
            camelctx.addRoutes(new RouteBuilder() {
                @Override
                public void configure() throws Exception {
					from("direct:start").toF("kafka:%s?brokers=%s&requestRequiredAcks=-1", TOPIC_A, getKafkaBrokers());
                }
            });

            camelctx.start();

            ProducerTemplate template = camelctx.createProducerTemplate();

            sendMessagesInRoute(5, template, "IT test message");
            sendMessagesInRoute(5, template, "IT test message in other topic", KafkaConstants.TOPIC, TOPIC_B);

            CountDownLatch latch = new CountDownLatch(10);

            boolean allReceived;
            try (KafkaConsumer<String, String> consumer = createKafkaConsumer()) {
                consumeKafkaMessages(consumer, TOPIC_A, TOPIC_B, latch);
                allReceived = latch.await(2, TimeUnit.SECONDS);
            }

            Assert.assertTrue("Messages published to the kafka topics were received: " + latch.getCount(), allReceived);
        }
    }

    @Test
    public void testCustomKafkaSerializer() throws Exception {

        try (CamelContext camelctx = new DefaultCamelContext()) {
        	
            camelctx.addRoutes(new RouteBuilder() {
                @Override
                public void configure() throws Exception {
                    from("direct:start").toF("kafka:%s?brokers=%s&requestRequiredAcks=-1&serializerClass=%s", TOPIC_A, 
                    		getKafkaBrokers(), SimpleKafkaSerializer.class.getName());
                }
            });

            camelctx.start();

            ProducerTemplate template = camelctx.createProducerTemplate();

            sendMessagesInRoute(5, template, "IT test message");
            sendMessagesInRoute(5, template, "IT test message in other topic", KafkaConstants.TOPIC, TOPIC_B);

            CountDownLatch latch = new CountDownLatch(10);

            boolean allReceived;
            try (KafkaConsumer<String, String> consumer = createKafkaConsumer()) {
                consumeKafkaMessages(consumer, TOPIC_A, TOPIC_B, latch);
                allReceived = latch.await(2, TimeUnit.SECONDS);
            }

            Assert.assertTrue("Messages published to the kafka topics were received: " + latch.getCount(), allReceived);
        }
    }

    @Test
    public void testKafkaMessageConsumedByCamel() throws Exception {

        try (CamelContext camelctx = new DefaultCamelContext()) {
        	
            camelctx.addRoutes(new RouteBuilder() {
                @Override
                public void configure() throws Exception {
                    fromF("kafka:%s?brokers=%s&groupId=group1&autoOffsetReset=earliest&autoCommitEnable=true", TOPIC_C, getKafkaBrokers())
                    .to("mock:result");
                }
            });
            
            MockEndpoint to = camelctx.getEndpoint("mock:result", MockEndpoint.class);
            to.expectedBodiesReceivedInAnyOrder("message-0", "message-1", "message-2", "message-3", "message-4");
            to.expectedMessageCount(5);

            camelctx.start();
            
            try (KafkaProducer<String, String> producer = createKafkaProducer()) {
                for (int k = 0; k < 5; k++) {
                    String msg = "message-" + k;
                    ProducerRecord<String, String> data = new ProducerRecord<String, String>(TOPIC_C, null, msg);
                    producer.send(data);
                }
                to.assertIsSatisfied(3000);
            }
        }
    }
    
    private KafkaConsumer<String, String> createKafkaConsumer() {
        Properties stringsProps = new Properties();
        stringsProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, getKafkaBrokers());
        stringsProps.put(ConsumerConfig.GROUP_ID_CONFIG, "DemoConsumer");
        stringsProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        stringsProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        stringsProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        return new KafkaConsumer<String, String>(stringsProps);
    }
    
    private KafkaProducer<String, String> createKafkaProducer() {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, getKafkaBrokers());
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, KafkaConstants.KAFKA_DEFAULT_SERIALIZER);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, KafkaConstants.KAFKA_DEFAULT_SERIALIZER);
        return new KafkaProducer<>(props);
    }

    private void consumeKafkaMessages(KafkaConsumer<String, String> consumer, String topic, String topicInHeader, CountDownLatch messagesLatch) {
        consumer.subscribe(Arrays.asList(topic, topicInHeader));
        boolean done = false;
        while (!done) {
            ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(100));
            for (@SuppressWarnings("unused") ConsumerRecord<String, String> record : records) {
                messagesLatch.countDown();
                if (messagesLatch.getCount() == 0) {
                    done = true;
                    break;
                }
            }
        }
    }

    private void sendMessagesInRoute(int messages, ProducerTemplate template, Object bodyOther, String... headersWithValue) {
        Map<String, Object> headerMap = new HashMap<>();
        for (int i = 0; i < headersWithValue.length; i = i + 2) {
            headerMap.put(headersWithValue[i], headersWithValue[i + 1]);
        }
        for (int k = 0; k < messages; k++) {
            template.sendBodyAndHeaders("direct:start", bodyOther, headerMap);
        }
    }
    
    private String getKafkaBrokers() {
        try {
            String dockerHost = TestUtils.getDockerHost();
            return dockerHost + ":" + KAFKA_PORT;
        } catch (RuntimeException rte) {
            throw rte;
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }
}
