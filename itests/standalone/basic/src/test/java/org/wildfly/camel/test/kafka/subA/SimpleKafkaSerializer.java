package org.wildfly.camel.test.kafka.subA;

import java.util.Map;

import org.apache.kafka.common.serialization.Serializer;

public class SimpleKafkaSerializer implements Serializer<String> {

    @Override
    public void configure(Map<String, ?> configs, boolean isKey) {
    }

    @Override
    public byte[] serialize(String topic, String data) {
        return data.getBytes();
    }

    @Override
    public void close() {
    }
}