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

package org.wildfly.camel.test.camel.kafka;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.kafka.KafkaConstants;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.wildfly.camel.test.common.kafka.EmbeddedKafkaCluster;
import org.wildfly.camel.test.common.zookeeper.EmbeddedZookeeper;

public class KafkaProducerTest {

    private static final String TOPIC_STRINGS = "test";
    private static final String TOPIC_STRINGS_IN_HEADER = "testHeader";
    private static final int KAFKA_PORT = 9092;

    static EmbeddedZookeeper embeddedZookeeper;
    static EmbeddedKafkaCluster embeddedKafkaCluster;

    @BeforeClass
    public static void before() throws Exception {
        embeddedZookeeper = new EmbeddedZookeeper();
        List<Integer> kafkaPorts = Collections.singletonList(KAFKA_PORT);
        embeddedKafkaCluster = new EmbeddedKafkaCluster(embeddedZookeeper.getConnection(), new Properties(), kafkaPorts);

        embeddedZookeeper.startup(1, TimeUnit.SECONDS);
        System.out.println("### Embedded Zookeeper connection: " + embeddedZookeeper.getConnection());

        embeddedKafkaCluster.startup();
        System.out.println("### Embedded Kafka cluster broker list: " + embeddedKafkaCluster.getBrokerList());
    }

    @AfterClass
    public static void after() throws Exception {
        embeddedKafkaCluster.shutdown();
        embeddedZookeeper.shutdown();
    }

    @Test
    public void producedStringMessageIsReceivedByKafka() throws Exception {
        int messageInTopic = 10;
        int messageInOtherTopic = 5;

        CountDownLatch latch = new CountDownLatch(messageInTopic + messageInOtherTopic);

        CamelContext camelctx = new DefaultCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:start").to("kafka:localhost:" + KAFKA_PORT + "?topic=" + TOPIC_STRINGS + "&requestRequiredAcks=-1");
            }
        });

        camelctx.start();
        try {
            ProducerTemplate template = camelctx.createProducerTemplate();

            sendMessagesInRoute(messageInTopic, template, "IT test message", KafkaConstants.PARTITION_KEY, "1");
            sendMessagesInRoute(messageInOtherTopic, template, "IT test message in other topic", KafkaConstants.PARTITION_KEY, "1", KafkaConstants.TOPIC, TOPIC_STRINGS_IN_HEADER);

            boolean allReceived;
            try (KafkaConsumer<String, String> consumer = createKafkaConsumer()) {
                consumeKafkaMessages(consumer, TOPIC_STRINGS, TOPIC_STRINGS_IN_HEADER, latch);
                allReceived = latch.await(2, TimeUnit.SECONDS);
            }

            Assert.assertTrue("Messages published to the kafka topics were received: " + latch.getCount(), allReceived);
        } finally {
            camelctx.stop();
        }
    }

    private KafkaConsumer<String, String> createKafkaConsumer() {
        Properties stringsProps = new Properties();
        stringsProps.put(org.apache.kafka.clients.consumer.ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:" + KAFKA_PORT);
        stringsProps.put(org.apache.kafka.clients.consumer.ConsumerConfig.GROUP_ID_CONFIG, "DemoConsumer");
        stringsProps.put(org.apache.kafka.clients.consumer.ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true");
        stringsProps.put(org.apache.kafka.clients.consumer.ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, "1000");
        stringsProps.put(org.apache.kafka.clients.consumer.ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, "30000");
        stringsProps.put(org.apache.kafka.clients.consumer.ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer");
        stringsProps.put(org.apache.kafka.clients.consumer.ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer");
        stringsProps.put(org.apache.kafka.clients.consumer.ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        return new KafkaConsumer<String, String>(stringsProps);
    }

    private void consumeKafkaMessages(KafkaConsumer<String, String> consumerConn, String topic, String topicInHeader, CountDownLatch messagesLatch) {
        consumerConn.subscribe(Arrays.asList(topic, topicInHeader));
        boolean run = true;

        while (run) {
            ConsumerRecords<String, String> records = consumerConn.poll(100);
            for (@SuppressWarnings("unused") ConsumerRecord<String, String> record : records) {
                messagesLatch.countDown();
                if (messagesLatch.getCount() == 0) {
                    run = false;
                }
            }
        }
    }

    private void sendMessagesInRoute(int messages, ProducerTemplate template, Object bodyOther, String... headersWithValue) {
        Map<String, Object> headerMap = new HashMap<String, Object>();
        for (int i = 0; i < headersWithValue.length; i = i + 2) {
            headerMap.put(headersWithValue[i], headersWithValue[i + 1]);
        }
        sendMessagesInRoute(messages, template, bodyOther, headerMap);
    }

    private void sendMessagesInRoute(int messages, ProducerTemplate template, Object bodyOther, Map<String, Object> headerMap) {
        for (int k = 0; k < messages; k++) {
            template.sendBodyAndHeaders("direct:start", bodyOther, headerMap);
        }
    }
}
