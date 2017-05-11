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
import org.apache.camel.component.aws.sqs.SqsConstants;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.impl.SimpleRegistry;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;
import org.wildfly.camel.test.common.aws.SQSUtils;

import com.amazonaws.services.sqs.AmazonSQSClient;

public class SQSIntegrationTest {
    
    public static AmazonSQSClient sqsClient;
    
    @BeforeClass
    public static void beforeClass() throws Exception {
        sqsClient = SQSUtils.createSQSClient();
        if (sqsClient != null) {
            SQSUtils.createQueue(sqsClient);
        }
    }

    @AfterClass
    public static void afterClass() throws Exception {
        if (sqsClient != null) {
            SQSUtils.deleteQueue(sqsClient);
        }
    }
    
    @Test
    public void sendInOnly() throws Exception {

        Assume.assumeNotNull("AWS client not null", sqsClient);

        SimpleRegistry registry = new SimpleRegistry();
        registry.put("sqsClient", sqsClient);

        CamelContext camelctx = new DefaultCamelContext(registry);
        camelctx.addRoutes(new RouteBuilder() {
            public void configure() {
                from("direct:start")
                .to("aws-sqs://" + SQSUtils.QUEUE_NAME + "?amazonSQSClient=#sqsClient");
            
                from("aws-sqs://" + SQSUtils.QUEUE_NAME + "?amazonSQSClient=#sqsClient")
                .to("mock:result");
            }
        });
        
        MockEndpoint result = camelctx.getEndpoint("mock:result", MockEndpoint.class);
        result.expectedMessageCount(1);
        
        camelctx.start();
        try {
            ProducerTemplate producer = camelctx.createProducerTemplate();
            
            producer.send("direct:start", ExchangePattern.InOnly, new Processor() {
                public void process(Exchange exchange) throws Exception {
                    exchange.getIn().setBody("This is my message text.");
                }
            });
            
            result.assertIsSatisfied();
            
            Exchange exchange = result.getExchanges().get(0);
            Assert.assertEquals("This is my message text.", exchange.getIn().getBody());
            Assert.assertNotNull(exchange.getIn().getHeader(SqsConstants.MESSAGE_ID));
            Assert.assertNotNull(exchange.getIn().getHeader(SqsConstants.RECEIPT_HANDLE));
            Assert.assertEquals("6a1559560f67c5e7a7d5d838bf0272ee", exchange.getIn().getHeader(SqsConstants.MD5_OF_BODY));
            Assert.assertNotNull(exchange.getIn().getHeader(SqsConstants.ATTRIBUTES));
            Assert.assertNotNull(exchange.getIn().getHeader(SqsConstants.MESSAGE_ATTRIBUTES));
        } finally {
            camelctx.stop();
        }
    }
    
}