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
import org.wildfly.camel.test.common.aws.BasicCredentialsProvider;
import org.wildfly.camel.test.common.aws.KinesisUtils;
import org.wildfly.extension.camel.CamelAware;
import org.wildfly.extension.camel.WildFlyCamelContext;

import com.amazonaws.services.kinesis.AmazonKinesisClient;
import com.amazonaws.services.kinesis.model.Record;

@CamelAware
@RunWith(Arquillian.class)
public class KinesisIntegrationTest {

    @Inject
    private KinesisClientProvider provider;
    
    @Deployment
    public static JavaArchive deployment() {
        JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "aws-kinesis-tests.jar");
        archive.addClasses(KinesisClientProducer.class, KinesisUtils.class, BasicCredentialsProvider.class);
        archive.addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml");
        return archive;
    }

    @Test
    public void send() throws Exception {

        AmazonKinesisClient kinClient = provider.getClient();
        Assume.assumeNotNull("AWS client not null", kinClient);

        WildFlyCamelContext camelctx = new WildFlyCamelContext();
        camelctx.getNamingContext().bind("kinClient", kinClient);
        
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
