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
import org.apache.camel.component.aws.sns.SnsConstants;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.camel.test.aws.subA.SNSClientProducer;
import org.wildfly.camel.test.aws.subA.SNSClientProducer.SNSClientProvider;
import org.wildfly.camel.test.common.aws.AWSUtils;
import org.wildfly.camel.test.common.aws.BasicCredentialsProvider;
import org.wildfly.camel.test.common.aws.SNSUtils;
import org.wildfly.extension.camel.CamelAware;
import org.wildfly.extension.camel.WildFlyCamelContext;

import com.amazonaws.services.sns.AmazonSNSClient;

@CamelAware
@RunWith(Arquillian.class)
public class SNSIntegrationTest {

    private static final String topicName = AWSUtils.toTimestampedName(SNSIntegrationTest.class);

    @Inject
    private SNSClientProvider provider;

    @Deployment
    public static JavaArchive deployment() {
        JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "aws-sns-tests.jar");
        archive.addClasses(SNSClientProducer.class, SNSUtils.class, BasicCredentialsProvider.class, AWSUtils.class);
        archive.addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml");
        return archive;
    }

    public static void assertNoStaleTopic(AmazonSNSClient client, String when) {
        /* Remove the topic created by the the old version of this test. Note that this may break the old tests running
         * in parallel. */
        client.listTopics().getTopics().stream()
                .filter(topic -> topic.getTopicArn().endsWith("MyNewTopic"))
                .forEach(t -> client.deleteTopic(t.getTopicArn()));
        List<String> staleInstances = client.listTopics().getTopics().stream() //
                .map(topic -> topic.getTopicArn().substring(topic.getTopicArn().lastIndexOf(':') + 1)) // extract the
                                                                                                       // topic name
                                                                                                       // from the ARN
                .filter(name -> !name.startsWith(SNSIntegrationTest.class.getSimpleName())
                        || System.currentTimeMillis() - AWSUtils.toEpochMillis(name) > AWSUtils.HOUR) //
                .collect(Collectors.toList());
        Assert.assertEquals(String.format("Found stale SNS topics %s running the test: %s", when, staleInstances), 0,
                staleInstances.size());
    }

    @Test
    public void sendInOnly() throws Exception {

        AmazonSNSClient snsClient = provider.getClient();
        Assume.assumeNotNull("AWS client not null", snsClient);

        // Temporary workaround for https://issues.apache.org/jira/browse/CAMEL-12379
        // assertNoStaleTopic(snsClient, "before");

        try {

            final String arn = snsClient.createTopic(topicName).getTopicArn();

            try {
                WildFlyCamelContext camelctx = new WildFlyCamelContext();
                camelctx.getNamingContext().bind("snsClientA", snsClient);

                camelctx.addRoutes(new RouteBuilder() {
                    public void configure() {
                        from("direct:start").to("aws-sns://" + topicName + "?amazonSNSClient=#snsClientA");
                    }
                });

                camelctx.start();
                try {
                    ProducerTemplate producer = camelctx.createProducerTemplate();
                    Exchange exchange = producer.send("direct:start", ExchangePattern.InOnly, new Processor() {
                        public void process(Exchange exchange) throws Exception {
                            exchange.getIn().setHeader(SnsConstants.SUBJECT, "This is my subject");
                            exchange.getIn().setBody("This is my message text.");
                        }
                    });

                    Assert.assertNotNull(exchange.getIn().getHeader(SnsConstants.MESSAGE_ID));

                } finally {
                    camelctx.stop();
                }
            } finally {
                // Temporary workaround for https://issues.apache.org/jira/browse/CAMEL-12379
                // snsClient.deleteTopic(arn);
            }
        } finally {
            // Temporary workaround for https://issues.apache.org/jira/browse/CAMEL-12379
            // assertNoStaleTopic(snsClient, "after");
        }
    }

    @Test
    public void sendInOut() throws Exception {

        AmazonSNSClient snsClient = provider.getClient();
        Assume.assumeNotNull("AWS client not null", snsClient);

        // Temporary workaround for https://issues.apache.org/jira/browse/CAMEL-12379
        // assertNoStaleTopic(snsClient, "before");

        try {

            final String arn = snsClient.createTopic(topicName).getTopicArn();

            try {

                WildFlyCamelContext camelctx = new WildFlyCamelContext();
                camelctx.getNamingContext().bind("snsClientB", snsClient);

                camelctx.addRoutes(new RouteBuilder() {
                    public void configure() {
                        from("direct:start").to("aws-sns://" + topicName + "?amazonSNSClient=#snsClientB");
                    }
                });

                camelctx.start();
                try {
                    ProducerTemplate producer = camelctx.createProducerTemplate();
                    Exchange exchange = producer.send("direct:start", ExchangePattern.InOut, new Processor() {
                        public void process(Exchange exchange) throws Exception {
                            exchange.getIn().setHeader(SnsConstants.SUBJECT, "This is my subject");
                            exchange.getIn().setBody("This is my message text.");
                        }
                    });

                    Assert.assertNotNull(exchange.getOut().getHeader(SnsConstants.MESSAGE_ID));

                } finally {
                    camelctx.stop();
                }
            } finally {
                // Temporary workaround for https://issues.apache.org/jira/browse/CAMEL-12379
                // snsClient.deleteTopic(arn);
            }
        } finally {
            // Temporary workaround for https://issues.apache.org/jira/browse/CAMEL-12379
            // assertNoStaleTopic(snsClient, "after");
        }
    }
}
