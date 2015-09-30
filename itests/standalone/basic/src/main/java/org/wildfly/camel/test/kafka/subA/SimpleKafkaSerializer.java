package org.wildfly.camel.test.kafka.subA;

import kafka.serializer.Encoder;

public class SimpleKafkaSerializer implements Encoder<String> {
    @Override
    public byte[] toBytes(String s) {
        return s.getBytes();
    }
}
