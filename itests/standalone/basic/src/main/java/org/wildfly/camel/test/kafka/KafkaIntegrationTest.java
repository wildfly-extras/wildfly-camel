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

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import kafka.consumer.Consumer;
import kafka.consumer.ConsumerConfig;
import kafka.consumer.ConsumerIterator;
import kafka.consumer.KafkaStream;
import kafka.javaapi.consumer.ConsumerConnector;
import kafka.javaapi.producer.Producer;
import kafka.producer.KeyedMessage;
import kafka.producer.ProducerConfig;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.Route;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.kafka.KafkaConstants;
import org.apache.camel.impl.DefaultCamelContext;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wildfly.camel.test.common.zookeeper.EmbeddedZookeeperServer;
import org.wildfly.camel.test.kafka.subA.SimpleKafkaPartitioner;
import org.wildfly.camel.test.kafka.subA.SimpleKafkaSerializer;
import org.wildfly.extension.camel.CamelAware;

@CamelAware
@RunWith(Arquillian.class)
@Ignore("[#1007] Kafka integration fails with authentication issue")
public class KafkaIntegrationTest {

    private static final Logger LOG = LoggerFactory.getLogger(KafkaIntegrationTest.class);
    private static final String TEST_TOPIC_NAME = "test";
    private static final int KAFKA_PORT = 9092;
    private static final int ZOOKEEPER_PORT = 2181;
    private static EmbeddedZookeeperServer zkServer;
    private static EmbeddedKafkaCluster kafkaCluster;

    @Deployment
    public static JavaArchive deployment() {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "kafka-integration-tests.jar");
        archive.addClasses(EmbeddedZookeeperServer.class, EmbeddedKafkaCluster.class, KafkaConstants.class);
        return archive;
    }

    @BeforeClass
    public static void beforeClass() throws Exception {
        List<Integer> kafkaPorts = new ArrayList<>();
        kafkaPorts.add(KAFKA_PORT);

        zkServer = new EmbeddedZookeeperServer(ZOOKEEPER_PORT, Paths.get("target", "zookeper"));
        kafkaCluster = new EmbeddedKafkaCluster(zkServer.getConnection(), new Properties(), kafkaPorts);

        zkServer.startup(1, TimeUnit.SECONDS);
        kafkaCluster.startup();
    }

    @AfterClass
    public static void afterClass() throws Exception {
        if (kafkaCluster != null) {
            kafkaCluster.shutdown();
        }
        if (zkServer != null) {
            zkServer.shutdown();
        }
    }

    @Test
    public void testKafkaProducer() throws Exception {
        String zkhost = "&zookeeperHost=localhost";
        String zkport = "&zookeeperPort=" + ZOOKEEPER_PORT;
        String serializer = "&serializerClass=kafka.serializer.StringEncoder";
        final String epuri = "kafka:localhost:" + KAFKA_PORT + "?topic=" + TEST_TOPIC_NAME + "&groupId=group1" + zkhost + zkport + serializer;

        CamelContext camelctx = new DefaultCamelContext();
        ConsumerConnector kafkaMessageConsumer = null;

        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:start").to(epuri);
            }
        });

        camelctx.start();
        try {
            CountDownLatch latch = new CountDownLatch(5);
            kafkaMessageConsumer = createKafkaMessageConsumer(latch);

            Map<String, Object> headerMap = new HashMap<>();
            headerMap.put(KafkaConstants.PARTITION_KEY, "1");

            ProducerTemplate producerTemplate = camelctx.createProducerTemplate();
            for (int i = 0; i < 5; i++) {
                producerTemplate.sendBodyAndHeaders("direct:start", "Camel Kafka test message " + i, headerMap);
            }

            boolean result = latch.await(5, TimeUnit.SECONDS);
            Assert.assertTrue("Expected " + 5 + " messages. Not received: " + latch.getCount(), result);
        } finally {
            camelctx.stop();
            if (kafkaMessageConsumer != null) {
                kafkaMessageConsumer.shutdown();
            }
        }
    }

    @Test
    public void testKafkaConsumer() throws Exception {
        String zkhost = "&zookeeperHost=localhost";
        String zkport = "&zookeeperPort=" + ZOOKEEPER_PORT;
        final String epuri = "kafka:localhost:" + KAFKA_PORT + "?topic=" + TEST_TOPIC_NAME + "&groupId=group1&autoOffsetReset=smallest" + zkhost + zkport;

        final CountDownLatch latch = new CountDownLatch(5);

        Producer<String, String> producer = createKafkaMessageProducer();
        CamelContext camelctx = new DefaultCamelContext();

        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from(epuri).process(new Processor() {
                    @Override
                    public void process(Exchange exchange) throws Exception {
                        LOG.info("Consumed message {}", exchange.getIn().getBody());
                        latch.countDown();
                    }
                });
            }
        });

        camelctx.start();
        try {
            for (int i = 0; i < 5; i++) {
                KeyedMessage<String, String> data = new KeyedMessage<>(TEST_TOPIC_NAME, "1", "Camel Kafka test message " + i);
                producer.send(data);
            }

            boolean result = latch.await(10, TimeUnit.SECONDS);
            Assert.assertTrue("Expected to consume 5 messages but consumed " + latch.getCount(), result);
        } finally {
            producer.close();
            camelctx.stop();
        }
    }

    @Test
    @Ignore("[KAFKA-2295] Dynamically loaded classes (encoders, etc.) may not be found by Kafka Producer")
    public void testCustomKafkaPartitionerLoads() throws Exception {
        String zkhost = "&zookeeperHost=localhost";
        String zkport = "&zookeeperPort=" + ZOOKEEPER_PORT;
        String partitioner = "&partitionerClass=" + SimpleKafkaPartitioner.class.getName();
        final String epuri = "kafka:localhost:" + KAFKA_PORT + "?topic=" + TEST_TOPIC_NAME + "&groupId=group1" + zkhost + zkport + partitioner;

        CamelContext camelctx = new DefaultCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:start").routeId("testCustomPartitioner").to(epuri);
            }
        });

        camelctx.start();
        try {
            Route route = camelctx.getRoute("testCustomPartitioner");
            Assert.assertTrue(route.getRouteContext().isRouteAdded());
        } finally {
            camelctx.stop();
        }
    }

    @Test
    @Ignore("[KAFKA-2295] Dynamically loaded classes (encoders, etc.) may not be found by Kafka Producer")
    public void testCustomKafkaSerializerLoads() throws Exception {
        String zkhost = "&zookeeperHost=localhost";
        String zkport = "&zookeeperPort=" + ZOOKEEPER_PORT;
        String serializer = "&serializerClass=" + SimpleKafkaSerializer.class.getName();
        final String epuri = "kafka:localhost:" + KAFKA_PORT + "?topic=" + TEST_TOPIC_NAME + "&groupId=group1" + zkhost + zkport + serializer;

        CamelContext camelctx = new DefaultCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:start").routeId("testCustomSerializer").to(epuri);
            }
        });

        camelctx.start();
        try {
            Route route = camelctx.getRoute("testCustomSerializer");
            Assert.assertTrue(route.getRouteContext().isRouteAdded());
        } finally {
            camelctx.stop();
        }
    }

    private ConsumerConnector createKafkaMessageConsumer(CountDownLatch messagesLatch) {
        Properties consumerProps = new Properties();
        consumerProps.put("zookeeper.connect", "localhost:" + ZOOKEEPER_PORT);
        consumerProps.put("group.id", "group1");
        consumerProps.put("zookeeper.session.timeout.ms", "6000");
        consumerProps.put("zookeeper.connectiontimeout.ms", "12000");
        consumerProps.put("zookeeper.sync.time.ms", "200");
        consumerProps.put("auto.commit.interval.ms", "1000");
        consumerProps.put("auto.offset.reset", "smallest");

        ConsumerConnector kafkaConsumer = Consumer.createJavaConsumerConnector(new ConsumerConfig(consumerProps));

        Map<String, Integer> topicCountMap = new HashMap<>();
        topicCountMap.put(TEST_TOPIC_NAME, 5);

        Map<String, List<KafkaStream<byte[], byte[]>>> consumerMap = kafkaConsumer.createMessageStreams(topicCountMap);
        ExecutorService executor = Executors.newFixedThreadPool(5);
        for (KafkaStream<byte[], byte[]> stream : consumerMap.get(TEST_TOPIC_NAME)) {
            executor.submit(new KakfaTopicConsumer(stream, messagesLatch));
        }

        return kafkaConsumer;
    }

    private Producer<String, String> createKafkaMessageProducer() {
        Properties properties = new Properties();
        properties.put("metadata.broker.list", "localhost:" + KAFKA_PORT);
        properties.put("serializer.class", "kafka.serializer.StringEncoder");
        properties.put("request.required.acks", "1");

        ProducerConfig config = new ProducerConfig(properties);
        return new Producer<>(config);
    }

    private static class KakfaTopicConsumer implements Runnable {
        private KafkaStream<byte[], byte[]> stream;
        private CountDownLatch latch;

        public KakfaTopicConsumer(KafkaStream<byte[], byte[]> stream, CountDownLatch latch) {
            this.stream = stream;
            this.latch = latch;
        }

        @Override
        public void run() {
            ConsumerIterator<byte[], byte[]> it = stream.iterator();
            while (it.hasNext()) {
                String msg = new String(it.next().message());
                LOG.info("Received message: {}", msg);
                latch.countDown();
            }
        }
    }
}
