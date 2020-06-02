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

package org.wildfly.camel.test.kafka;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.ServiceStatus;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.kafka.KafkaComponent;
import org.apache.camel.component.kafka.KafkaConfiguration;
import org.apache.camel.component.kafka.KafkaConstants;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.camel.test.common.kafka.EmbeddedKafkaBroker;
import org.wildfly.camel.test.common.utils.TestUtils;
import org.wildfly.camel.test.common.zookeeper.EmbeddedZookeeper;
import org.wildfly.camel.test.kafka.subA.SimpleKafkaPartitioner;
import org.wildfly.camel.test.kafka.subA.SimpleKafkaSerializer;
import org.wildfly.extension.camel.CamelAware;

@CamelAware
@RunWith(Arquillian.class)
@Ignore("[ENTESB-13788] Kafka fails with NoSuchMethodError")
public class KafkaIntegrationTest {

    private static final String TOPIC_STRINGS = "test";
    private static final String TOPIC_STRINGS_IN_HEADER = "testHeader";
    private static final int KAFKA_PORT = 9092;

    static EmbeddedZookeeper embeddedZookeeper;
    static EmbeddedKafkaBroker embeddedKafkaBroker;

    @Deployment
    public static JavaArchive deployment() {
        return ShrinkWrap.create(JavaArchive.class, "kafka-producer-tests.jar")
            .addClasses(SimpleKafkaSerializer.class, SimpleKafkaPartitioner.class, TestUtils.class,
                EmbeddedZookeeper.class, EmbeddedKafkaBroker.class);
    }

    @BeforeClass
    public static void before() throws Exception {
        embeddedZookeeper = new EmbeddedZookeeper();
        embeddedKafkaBroker = new EmbeddedKafkaBroker(0, KAFKA_PORT, embeddedZookeeper.getConnection(), new Properties());

        embeddedZookeeper.startup(1, TimeUnit.SECONDS);
        System.out.println("### Embedded Zookeeper connection: " + embeddedZookeeper.getConnection());

        embeddedKafkaBroker.before();
        System.out.println("### Embedded Kafka cluster broker list: " + embeddedKafkaBroker.getBrokerList());
    }

    @AfterClass
    public static void after() throws Exception {
        try {
            embeddedKafkaBroker.after();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        try {
            embeddedZookeeper.shutdown();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @Test
    public void producedStringMessageIsReceivedByKafka() throws Exception {

        String epuri = "kafka:" + TOPIC_STRINGS + "?requestRequiredAcks=-1";

        CamelContext camelctx = createCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:start").to(epuri);
            }
        });

        camelctx.start();
        try {
            ProducerTemplate template = camelctx.createProducerTemplate();

            sendMessagesInRoute(10, template, "IT test message", KafkaConstants.PARTITION_KEY, "1");
            sendMessagesInRoute(5, template, "IT test message in other topic", KafkaConstants.PARTITION_KEY, "1", KafkaConstants.TOPIC, TOPIC_STRINGS_IN_HEADER);

            CountDownLatch latch = new CountDownLatch(15);

            boolean allReceived;
            try (KafkaConsumer<String, String> consumer = createKafkaConsumer()) {
                consumeKafkaMessages(consumer, TOPIC_STRINGS, TOPIC_STRINGS_IN_HEADER, latch);
                allReceived = latch.await(2, TimeUnit.SECONDS);
            }

            Assert.assertTrue("Messages published to the kafka topics were received: " + latch.getCount(), allReceived);
        } finally {
            camelctx.close();
        }
    }

    @Test
    public void testCustomKafkaSerializer() throws Exception {

        String serializer = "&serializerClass=" + SimpleKafkaSerializer.class.getName();
        String epuri = "kafka:" + TOPIC_STRINGS + "?requestRequiredAcks=-1" + serializer;

        CamelContext camelctx = createCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:start").to(epuri);
            }
        });

        camelctx.start();
        try {
            Assert.assertEquals(ServiceStatus.Started, camelctx.getStatus());
        } finally {
            camelctx.close();
        }
    }

    @Test
    public void testCustomKafkaPartitioner() throws Exception {

        String partitioner = "&partitioner=" + SimpleKafkaPartitioner.class.getName();
        String epuri = "kafka:" + TOPIC_STRINGS + "?requestRequiredAcks=-1" + partitioner;

        CamelContext camelctx = createCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:start").to(epuri);
            }
        });

        camelctx.start();
        try {
            Assert.assertEquals(ServiceStatus.Started, camelctx.getStatus());
        } finally {
            camelctx.close();
        }
    }

    @Test
    public void testKafkaMessageConsumedByCamel() throws Exception {

        CamelContext camelctx = createCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("kafka:" + TOPIC_STRINGS + "?groupId=group1&autoOffsetReset=earliest&autoCommitEnable=true")
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
                ProducerRecord<String, String> data = new ProducerRecord<String, String>(TOPIC_STRINGS, "1", msg);
                producer.send(data);
            }
            to.assertIsSatisfied(3000);
        } finally {
            camelctx.stop();
        }
    }

	private CamelContext createCamelContext() {
        CamelContext camelctx = new DefaultCamelContext();
		KafkaConfiguration config = new KafkaConfiguration();
        config.setBrokers("localhost:" + KAFKA_PORT);
        KafkaComponent kafka = new KafkaComponent();
        kafka.setConfiguration(config);
        camelctx.addComponent("kafka", kafka);
        return camelctx;
	}

    private KafkaProducer<String, String> createKafkaProducer() {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:" + KAFKA_PORT);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, KafkaConstants.KAFKA_DEFAULT_SERIALIZER);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, KafkaConstants.KAFKA_DEFAULT_SERIALIZER);
        props.put(ProducerConfig.PARTITIONER_CLASS_CONFIG, KafkaConstants.KAFKA_DEFAULT_PARTITIONER);
        props.put(ProducerConfig.ACKS_CONFIG, "1");
        ClassLoader tccl = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(org.apache.kafka.clients.producer.KafkaProducer.class.getClassLoader());
            return new KafkaProducer<>(props);
        } finally {
            Thread.currentThread().setContextClassLoader(tccl);
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
        ClassLoader tccl = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
            return new KafkaConsumer<String, String>(stringsProps);
        } finally {
            Thread.currentThread().setContextClassLoader(tccl);
        }
    }

    @SuppressWarnings("unused")
    private void consumeKafkaMessages(KafkaConsumer<String, String> consumerConn, String topic, String topicInHeader, CountDownLatch messagesLatch) {
        consumerConn.subscribe(Arrays.asList(topic, topicInHeader));
        boolean run = true;

        while (run) {
            ConsumerRecords<String, String> records = consumerConn.poll(100);
            for (ConsumerRecord<String, String> record : records) {
                messagesLatch.countDown();
                if (messagesLatch.getCount() == 0) {
                    run = false;
                }
            }
        }
    }

    private void sendMessagesInRoute(int messages, ProducerTemplate template, Object bodyOther, String... headersWithValue) {
        Map<String, Object> headerMap = new HashMap<>();
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
