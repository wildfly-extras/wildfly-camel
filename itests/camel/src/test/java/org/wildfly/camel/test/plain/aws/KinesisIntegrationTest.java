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
package org.wildfly.camel.test.plain.aws;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.ExchangePattern;
import org.apache.camel.Processor;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.aws.kinesis.KinesisConstants;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.impl.SimpleRegistry;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;
import org.wildfly.camel.test.common.aws.KinesisUtils;

import com.amazonaws.services.kinesis.AmazonKinesisClient;
import com.amazonaws.services.kinesis.model.Record;

public class KinesisIntegrationTest {

    private static AmazonKinesisClient kinClient;
    
    @BeforeClass
    public static void beforeClass() throws Exception {
        kinClient = KinesisUtils.createKinesisClient();
        if (kinClient != null) {
            KinesisUtils.createStream(kinClient);
        }
    }

    @AfterClass
    public static void afterClass() throws Exception {
        if (kinClient != null) {
            KinesisUtils.deleteStream(kinClient);
        }
    }

    @Test
    public void send() throws Exception {

        Assume.assumeNotNull("AWS client not null", kinClient);

        SimpleRegistry registry = new SimpleRegistry();
        registry.put("kinClient", kinClient);

        CamelContext camelctx = new DefaultCamelContext(registry);
        camelctx.addRoutes(new RouteBuilder() {
            public void configure() {
                from("direct:start")
                .to("aws-kinesis://" + KinesisUtils.STREAM_NAME + "?amazonKinesisClient=#kinClient");
                
                from("aws-kinesis://" + KinesisUtils.STREAM_NAME + "?amazonKinesisClient=#kinClient")
                .to("mock:result");
            }
        });
        
        MockEndpoint mockep = camelctx.getEndpoint("mock:result", MockEndpoint.class);
        mockep.expectedMessageCount(2);
        
        camelctx.start();
        try {
            ProducerTemplate producer = camelctx.createProducerTemplate();

            Exchange exchange = producer.send("direct:start", ExchangePattern.InOnly, new Processor() {
                public void process(Exchange exchange) throws Exception {
                    exchange.getIn().setHeader(KinesisConstants.PARTITION_KEY, "partition-1");
                    exchange.getIn().setBody("Kinesis Event 1.");
                }
            });
            Assert.assertNull(exchange.getException());
            
            exchange = producer.send("direct:start", ExchangePattern.InOut, new Processor() {
                public void process(Exchange exchange) throws Exception {
                    exchange.getIn().setHeader(KinesisConstants.PARTITION_KEY, "partition-1");
                    exchange.getIn().setBody("Kinesis Event 2.");
                }
            });
            Assert.assertNull(exchange.getException());

            mockep.assertIsSatisfied();

            assertResultExchange(mockep.getExchanges().get(0), "Kinesis Event 1.", "partition-1");
            assertResultExchange(mockep.getExchanges().get(1), "Kinesis Event 2.", "partition-1");
        } finally {
            camelctx.stop();
        }
    }

    private void assertResultExchange(Exchange resultExchange, String data, String partition) {
        Record record = resultExchange.getIn().getBody(Record.class);
        Assert.assertEquals(data, new String(record.getData().array()));
        Assert.assertEquals(partition, resultExchange.getIn().getHeader(KinesisConstants.PARTITION_KEY));
        Assert.assertNotNull(resultExchange.getIn().getHeader(KinesisConstants.APPROX_ARRIVAL_TIME));
        Assert.assertNotNull(resultExchange.getIn().getHeader(KinesisConstants.SEQUENCE_NUMBER));
    }

}
