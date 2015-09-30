package org.wildfly.camel.test.kafka.subA;

import kafka.producer.Partitioner;
import kafka.utils.VerifiableProperties;

public class SimpleKafkaPartitioner implements Partitioner {
    public SimpleKafkaPartitioner(VerifiableProperties props) {
    }

    @Override
    public int partition(Object key, int numPartitions) {
        return key.hashCode() % numPartitions;
    }
}
