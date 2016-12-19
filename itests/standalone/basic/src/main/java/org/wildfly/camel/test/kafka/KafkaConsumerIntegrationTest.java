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

import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import org.apache.camel.CamelContext;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.kafka.KafkaConstants;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.gravia.resource.ManifestBuilder;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.camel.test.common.kafka.EmbeddedKafkaCluster;
import org.wildfly.camel.test.common.utils.TestUtils;
import org.wildfly.camel.test.common.zookeeper.EmbeddedZookeeper;
import org.wildfly.extension.camel.CamelAware;

@CamelAware
@RunWith(Arquillian.class)
@Ignore("[#1536] Disable camel-kafka component")
public class KafkaConsumerIntegrationTest {

    private static final int KAFKA_PORT = 9092;
    public static final String TOPIC = "test";

    static EmbeddedZookeeper embeddedZookeeper;
    static EmbeddedKafkaCluster embeddedKafkaCluster;

    @Deployment
    public static JavaArchive deployment() {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "kafka-consumer-tests.jar");
        archive.addPackage(EmbeddedKafkaCluster.class.getPackage());
        archive.addPackage(EmbeddedZookeeper.class.getPackage());
        archive.addClass(TestUtils.class);
        archive.setManifest(new Asset() {
            @Override
            public InputStream openStream() {
                ManifestBuilder builder = new ManifestBuilder();
                builder.addManifestHeader("Dependencies", "org.apache.kafka");
                return builder.openStream();
            }
        });
        return archive;
    }

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
    public void kaftMessageIsConsumedByCamel() throws Exception {

        CamelContext camelctx = new DefaultCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("kafka:localhost:" + KAFKA_PORT + "?topic=" + TOPIC
                        + "&groupId=group1&autoOffsetReset=earliest&keyDeserializer=org.apache.kafka.common.serialization.StringDeserializer&"
                        + "valueDeserializer=org.apache.kafka.common.serialization.StringDeserializer"
                        + "&autoCommitIntervalMs=1000&sessionTimeoutMs=30000&autoCommitEnable=true")
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
                ProducerRecord<String, String> data = new ProducerRecord<String, String>(TOPIC, "1", msg);
                producer.send(data);
            }
            to.assertIsSatisfied(3000);
        } finally {
            camelctx.stop();
        }
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
            Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
            return new KafkaProducer<String, String>(props);
        } finally {
            Thread.currentThread().setContextClassLoader(tccl);
        }
    }
}
