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
import org.apache.camel.component.aws.sqs.SqsConstants;
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
import org.wildfly.camel.test.aws.subA.SQSClientProducer;
import org.wildfly.camel.test.aws.subA.SQSClientProducer.SQSClientProvider;
import org.wildfly.camel.test.common.aws.AWSUtils;
import org.wildfly.camel.test.common.aws.BasicCredentialsProvider;
import org.wildfly.camel.test.common.aws.SQSUtils;
import org.wildfly.extension.camel.CamelAware;
import org.wildfly.extension.camel.WildFlyCamelContext;

import com.amazonaws.services.sqs.AmazonSQSClient;

@CamelAware
@RunWith(Arquillian.class)
public class SQSIntegrationTest {

    private static final String queueName = AWSUtils.toTimestampedName(SQSIntegrationTest.class);

    @Inject
    private SQSClientProvider provider;

    @Deployment
    public static JavaArchive deployment() {
        JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "aws-sqs-tests.jar");
        archive.addClasses(SQSClientProducer.class, SQSUtils.class, BasicCredentialsProvider.class, AWSUtils.class);
        archive.addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml");
        return archive;
    }

    public static void assertNoStaleQueue(AmazonSQSClient client, String when) {
        List<String> staleInstances = client.listQueues().getQueueUrls().stream() //
                .map(url -> url.substring(url.lastIndexOf(':') + 1))
                .filter(name -> !name.startsWith(SQSIntegrationTest.class.getSimpleName())
                        || System.currentTimeMillis() - AWSUtils.toEpochMillis(name) > AWSUtils.HOUR) //
                .collect(Collectors.toList());
        Assert.assertEquals(String.format("Found stale SQS queues %s running the test: %s", when, staleInstances), 0,
                staleInstances.size());
    }

    @Test
    public void sendInOnly() throws Exception {

        AmazonSQSClient sqsClient = provider.getClient();
        Assume.assumeNotNull("AWS client not null", sqsClient);

        assertNoStaleQueue(sqsClient, "before");

        try {

            final String url = sqsClient.createQueue(queueName).getQueueUrl();

            try {

                WildFlyCamelContext camelctx = new WildFlyCamelContext();
                camelctx.getNamingContext().bind("sqsClient", sqsClient);

                camelctx.addRoutes(new RouteBuilder() {
                    public void configure() {
                        from("direct:start").to("aws-sqs://" + queueName + "?amazonSQSClient=#sqsClient");

                        from("aws-sqs://" + queueName + "?amazonSQSClient=#sqsClient").to("mock:result");
                    }
                });

                MockEndpoint mockep = camelctx.getEndpoint("mock:result", MockEndpoint.class);
                mockep.expectedMessageCount(1);

                camelctx.start();
                try {
                    ProducerTemplate producer = camelctx.createProducerTemplate();

                    producer.send("direct:start", ExchangePattern.InOnly, new Processor() {
                        public void process(Exchange exchange) throws Exception {
                            exchange.getIn().setBody("This is my message text.");
                        }
                    });

                    mockep.assertIsSatisfied();

                    Exchange exchange = mockep.getExchanges().get(0);
                    Assert.assertEquals("This is my message text.", exchange.getIn().getBody());
                    Assert.assertNotNull(exchange.getIn().getHeader(SqsConstants.MESSAGE_ID));
                    Assert.assertNotNull(exchange.getIn().getHeader(SqsConstants.RECEIPT_HANDLE));
                    Assert.assertEquals("6a1559560f67c5e7a7d5d838bf0272ee",
                            exchange.getIn().getHeader(SqsConstants.MD5_OF_BODY));
                    Assert.assertNotNull(exchange.getIn().getHeader(SqsConstants.ATTRIBUTES));
                    Assert.assertNotNull(exchange.getIn().getHeader(SqsConstants.MESSAGE_ATTRIBUTES));
                } finally {
                    camelctx.stop();
                }
            } finally {
                sqsClient.deleteQueue(url);
            }
        } finally {
            assertNoStaleQueue(sqsClient, "after");
        }
    }
}