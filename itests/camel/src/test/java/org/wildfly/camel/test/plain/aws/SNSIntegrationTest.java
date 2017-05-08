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
import org.apache.camel.component.aws.sns.SnsConstants;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.impl.SimpleRegistry;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;
import org.wildfly.camel.test.common.aws.SNSUtils;

import com.amazonaws.services.sns.AmazonSNSClient;

public class SNSIntegrationTest {

    private static AmazonSNSClient snsClient;
    
    @BeforeClass
    public static void beforeClass() throws Exception {
        snsClient = SNSUtils.createNotificationClient();
    }
    
    @Test
    public void sendInOnly() throws Exception {
        
        Assume.assumeNotNull("AWS client not null", snsClient);
        
        SimpleRegistry registry = new SimpleRegistry();
        registry.put("snsClient", snsClient);
        
        CamelContext camelctx = new DefaultCamelContext(registry);
        camelctx.addRoutes(new RouteBuilder() {
            public void configure() {
                from("direct:start")
                .to("aws-sns://" + SNSUtils.TOPIC_NAME + "?amazonSNSClient=#snsClient");
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
    }

    @Test
    public void sendInOut() throws Exception {
        
        Assume.assumeNotNull("AWS client not null", snsClient);
        
        SimpleRegistry registry = new SimpleRegistry();
        registry.put("snsClient", snsClient);
        
        CamelContext camelctx = new DefaultCamelContext(registry);
        camelctx.addRoutes(new RouteBuilder() {
            public void configure() {
                from("direct:start")
                .to("aws-sns://MyNewTopic?amazonSNSClient=#snsClient");
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
    }

}
