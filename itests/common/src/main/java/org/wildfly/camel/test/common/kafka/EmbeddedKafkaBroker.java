/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.wildfly.camel.test.common.kafka;

import java.io.File;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.kafka.common.utils.SystemTime;
import org.junit.rules.ExternalResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wildfly.camel.test.common.utils.AvailablePortFinder;

import kafka.metrics.KafkaMetricsReporter;
import kafka.server.KafkaConfig;
import kafka.server.KafkaServer;
import scala.Option;
import scala.collection.mutable.Buffer;


public class EmbeddedKafkaBroker extends ExternalResource {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private final Integer brokerId;
    private final Integer port;
    private final String zkConnection;
    private final Properties baseProperties;

    private final String brokerList;

    private KafkaServer kafkaServer;
    private File logDir;

    public EmbeddedKafkaBroker(int brokerId, String zkConnection) {
        this(brokerId, AvailablePortFinder.getNextAvailable(), zkConnection, new Properties());
    }

    public EmbeddedKafkaBroker(int brokerId, int port, String zkConnection, Properties baseProperties) {
        this.brokerId = brokerId;
        this.port = port;
        this.zkConnection = zkConnection;
        this.baseProperties = baseProperties;

        log.info("Starting broker[{}] on port {}", brokerId, port);
        this.brokerList = "localhost:" + this.port;
    }

    @Override
    public void before() {
        logDir = Paths.get("target/kafka-log").toFile();
        logDir.mkdirs();

        Properties properties = new Properties();
        properties.putAll(baseProperties);
        properties.setProperty("zookeeper.connect", zkConnection);
        properties.setProperty("broker.id", brokerId.toString());
        properties.setProperty("host.name", "localhost");
        properties.setProperty("port", Integer.toString(port));
        properties.setProperty("log.dir", logDir.getAbsolutePath());
        properties.setProperty("num.partitions", String.valueOf(1));
        properties.setProperty("auto.create.topics.enable", String.valueOf(Boolean.TRUE));
        log.info("log directory: " + logDir.getAbsolutePath());
        properties.setProperty("log.flush.interval.messages", String.valueOf(1));
        properties.setProperty("offsets.topic.replication.factor", String.valueOf(1));

        kafkaServer = startBroker(properties);
    }

    public void after() {
        kafkaServer.shutdown();
    }

    private KafkaServer startBroker(Properties props) {
        List<KafkaMetricsReporter> kmrList = new ArrayList<>();
        Buffer<KafkaMetricsReporter> metricsList = scala.collection.JavaConversions.asScalaBuffer(kmrList);
        KafkaServer server = new KafkaServer(new KafkaConfig(props), new SystemTime(), Option.<String> empty(), metricsList);
        server.startup();
        return server;
    }

    public String getBrokerList() {
        return brokerList;
    }

    public Integer getPort() {
        return port;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("EmbeddedKafkaBroker{");
        sb.append("brokerList='").append(brokerList).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
