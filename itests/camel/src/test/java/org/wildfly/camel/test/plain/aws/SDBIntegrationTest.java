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
import java.util.List;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.aws.sdb.SdbConstants;
import org.apache.camel.component.aws.sdb.SdbOperations;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.impl.SimpleRegistry;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;
import org.wildfly.camel.test.common.aws.SDBUtils;

import com.amazonaws.services.simpledb.AmazonSimpleDBClient;
import com.amazonaws.services.simpledb.model.Attribute;
import com.amazonaws.services.simpledb.model.ReplaceableAttribute;

public class SDBIntegrationTest {

    public static AmazonSimpleDBClient sdbClient;

    @BeforeClass
    public static void beforeClass() throws Exception {
        sdbClient = SDBUtils.createDBClient();
        if (sdbClient != null) {
            SDBUtils.createDomain(sdbClient);
        }
    }

    @AfterClass
    public static void afterClass() throws Exception {
        if (sdbClient != null) {
            SDBUtils.deleteDomain(sdbClient);
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    public void putAndGetAttributes() throws Exception {

        Assume.assumeNotNull("AWS client not null", sdbClient);

        SimpleRegistry registry = new SimpleRegistry();
        registry.put("sdbClient", sdbClient);

        CamelContext camelctx = new DefaultCamelContext(registry);
        camelctx.addRoutes(new RouteBuilder() {
            public void configure() {
                from("direct:start").to("aws-sdb://" + SDBUtils.DOMAIN_NAME + "?amazonSDBClient=#sdbClient");
            }
        });

        camelctx.start();
        try {
            ReplaceableAttribute attr = new ReplaceableAttribute("SomeName", "SomeValue", true);
            
            ProducerTemplate producer = camelctx.createProducerTemplate();
            Exchange exchange = producer.send("direct:start", new Processor() {
                public void process(Exchange exchange) throws Exception {
                    exchange.getIn().setHeader(SdbConstants.OPERATION, SdbOperations.PutAttributes);
                    exchange.getIn().setHeader(SdbConstants.ITEM_NAME, SDBUtils.ITEM_NAME);
                    exchange.getIn().setHeader(SdbConstants.REPLACEABLE_ATTRIBUTES, Collections.singletonList(attr));
                }
            });
            Assert.assertNull(exchange.getException());

            int retries = 10;
            List<Attribute> result = Collections.emptyList();
            while (result.isEmpty() && 0 < retries--) {
                exchange = producer.send("direct:start", new Processor() {
                    public void process(Exchange exchange) throws Exception {
                        exchange.getIn().setHeader(SdbConstants.OPERATION, SdbOperations.GetAttributes);
                        exchange.getIn().setHeader(SdbConstants.ITEM_NAME, SDBUtils.ITEM_NAME);
                    }
                });
                Assert.assertNull(exchange.getException());
                result = exchange.getIn().getHeader(SdbConstants.ATTRIBUTES, List.class);
                System.out.println(retries + ": " + result);
                Thread.sleep(500);
            }
            Assert.assertEquals(1, result.size());
            Assert.assertEquals(attr.getName(), result.get(0).getName());
            Assert.assertEquals(attr.getValue(), result.get(0).getValue());
            
        } finally {
            camelctx.stop();
        }
    }
}
