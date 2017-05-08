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

import java.util.Collections;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.ExchangeBuilder;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.aws.ses.SesConstants;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.impl.SimpleRegistry;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;
import org.wildfly.camel.test.common.aws.SESUtils;

import com.amazonaws.services.simpleemail.AmazonSimpleEmailServiceClient;

public class SESIntegrationTest {

    private static AmazonSimpleEmailServiceClient sesClient;
    
    @BeforeClass
    public static void beforeClass() throws Exception {
        sesClient = SESUtils.createEmailClient();
    }
    
    @Test
    public void sendSimpleMessage() throws Exception {
        
        Assume.assumeNotNull("AWS client not null", sesClient);
        
        SimpleRegistry registry = new SimpleRegistry();
        registry.put("sesClient", sesClient);
        
        CamelContext camelctx = new DefaultCamelContext(registry);
        camelctx.addRoutes(new RouteBuilder() {
            public void configure() {
                from("direct:start")
                .to("aws-ses://" + SESUtils.FROM + "?amazonSESClient=#sesClient");
            }
        });
        
        camelctx.start();
        try {
            Exchange exchange = ExchangeBuilder.anExchange(camelctx)
                    .withHeader(SesConstants.SUBJECT, SESUtils.SUBJECT)
                    .withHeader(SesConstants.TO, Collections.singletonList(SESUtils.TO))
                    .withBody("Hello world!")
                    .build();
                    
            ProducerTemplate producer = camelctx.createProducerTemplate();
            Exchange result = producer.send("direct:start", exchange);
            
            String messageId = result.getIn().getHeader(SesConstants.MESSAGE_ID, String.class);
            Assert.assertNotNull("MessageId not null", messageId);
            
        } finally {
            camelctx.stop();
        }
    }
}
