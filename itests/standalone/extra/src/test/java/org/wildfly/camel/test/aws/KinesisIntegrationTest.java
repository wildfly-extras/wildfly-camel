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
package org.wildfly.camel.test.aws;

import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.camel.Exchange;
import org.apache.camel.ExchangePattern;
import org.apache.camel.Processor;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.aws.kinesis.KinesisConstants;
import org.apache.camel.component.mock.MockEndpoint;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.camel.test.aws.subA.KinesisClientProducer;
import org.wildfly.camel.test.aws.subA.KinesisClientProducer.KinesisClientProvider;
import org.wildfly.camel.test.common.aws.AWSUtils;
import org.wildfly.camel.test.common.aws.BasicCredentialsProvider;
import org.wildfly.camel.test.common.aws.KinesisUtils;
import org.wildfly.extension.camel.CamelAware;
import org.wildfly.extension.camel.WildFlyCamelContext;

import com.amazonaws.services.kinesis.AmazonKinesisClient;
import com.amazonaws.services.kinesis.model.Record;

@CamelAware
@RunWith(Arquillian.class)
public class KinesisIntegrationTest {

    private static final String streamName = AWSUtils.toTimestampedName(KinesisIntegrationTest.class);
    @Inject
    private KinesisClientProvider provider;

    @Deployment
    public static JavaArchive deployment() {
        JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "aws-kinesis-tests.jar");
        archive.addClasses(KinesisClientProducer.class, KinesisUtils.class, BasicCredentialsProvider.class,
                AWSUtils.class);
        archive.addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml");
        return archive;
    }

    public static void assertNoStaleStreams(AmazonKinesisClient client, String when) {
        List<String> staleInstances = client.listStreams().getStreamNames().stream() //
        .filter(s -> !s.startsWith(KinesisIntegrationTest.class.getSimpleName()) ||
                System.currentTimeMillis() - AWSUtils.toEpochMillis(s) > AWSUtils.HOUR || !client.describeStream(s).getStreamDescription().getStreamStatus().equals("DELETING")) //
        .collect(Collectors.toList());
        Assert.assertEquals(String.format("Found stale Kinesis streams %s running the test: %s", when, staleInstances), 0, staleInstances.size());
    }

    @Test
    public void send() throws Exception {

        AmazonKinesisClient kinClient = provider.getClient();
        Assume.assumeNotNull("AWS client not null", kinClient);

        // Temporary workaround for https://issues.apache.org/jira/browse/CAMEL-12379
        // assertNoStaleStreams(kinClient, "before");
        try {
            KinesisUtils.createStream(kinClient, streamName);
            try {

                WildFlyCamelContext camelctx = new WildFlyCamelContext();
                camelctx.getNamingContext().bind("kinClient", kinClient);

                camelctx.addRoutes(new RouteBuilder() {
                    public void configure() {
                        from("direct:start").to("aws-kinesis://" + streamName + "?amazonKinesisClient=#kinClient");

                        from("aws-kinesis://" + streamName + "?amazonKinesisClient=#kinClient").to("mock:result");
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
            } finally {
                // Temporary workaround for https://issues.apache.org/jira/browse/CAMEL-12379
                // kinClient.deleteStream(streamName);
            }
        } finally {
            // Temporary workaround for https://issues.apache.org/jira/browse/CAMEL-12379
            // assertNoStaleStreams(kinClient, "after");
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
