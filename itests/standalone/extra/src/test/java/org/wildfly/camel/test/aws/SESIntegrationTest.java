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

import java.util.Collections;

import javax.inject.Inject;

import org.apache.camel.Exchange;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.ExchangeBuilder;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.aws.ses.SesConstants;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.camel.test.aws.subA.SESClientProducer;
import org.wildfly.camel.test.aws.subA.SESClientProducer.SESClientProvider;
import org.wildfly.camel.test.common.aws.BasicCredentialsProvider;
import org.wildfly.camel.test.common.aws.SESUtils;
import org.wildfly.extension.camel.CamelAware;
import org.wildfly.extension.camel.WildFlyCamelContext;

import com.amazonaws.services.simpleemail.AmazonSimpleEmailServiceClient;

@CamelAware
@RunWith(Arquillian.class)
public class SESIntegrationTest {

    @Inject
    private SESClientProvider provider;

    @Deployment
    public static JavaArchive deployment() {
        JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "aws-ses-tests.jar");
        archive.addClasses(SESClientProducer.class, SESUtils.class, BasicCredentialsProvider.class);
        archive.addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml");
        return archive;
    }

    @Test
    public void sendSimpleMessage() throws Exception {

        AmazonSimpleEmailServiceClient sesClient = provider.getClient();
        Assume.assumeNotNull("AWS client not null", sesClient);

        WildFlyCamelContext camelctx = new WildFlyCamelContext();
        camelctx.getNamingContext().bind("sesClient", sesClient);

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
