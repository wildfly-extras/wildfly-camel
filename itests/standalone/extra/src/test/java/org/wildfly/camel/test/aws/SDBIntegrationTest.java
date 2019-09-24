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
import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.aws.sdb.SdbConstants;
import org.apache.camel.component.aws.sdb.SdbOperations;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.camel.test.aws.subA.SDBClientProducer;
import org.wildfly.camel.test.aws.subA.SDBClientProducer.SDBClientProvider;
import org.wildfly.camel.test.common.aws.AWSUtils;
import org.wildfly.camel.test.common.aws.BasicCredentialsProvider;
import org.wildfly.camel.test.common.aws.SDBUtils;
import org.wildfly.extension.camel.CamelAware;
import org.wildfly.extension.camel.WildFlyCamelContext;

import com.amazonaws.services.simpledb.AmazonSimpleDBClient;
import com.amazonaws.services.simpledb.model.Attribute;
import com.amazonaws.services.simpledb.model.DeleteDomainRequest;
import com.amazonaws.services.simpledb.model.ReplaceableAttribute;

@CamelAware
@RunWith(Arquillian.class)
public class SDBIntegrationTest {

    private static final String domainName = AWSUtils.toTimestampedName(SDBIntegrationTest.class);
    private static final String itemName = "Item-" + AWSUtils.toTimestampedName(SDBIntegrationTest.class);

    @Inject
    private SDBClientProvider provider;

    @Deployment
    public static JavaArchive deployment() {
        JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "aws-sdb-tests.jar");
        archive.addClasses(SDBClientProducer.class, SDBUtils.class, BasicCredentialsProvider.class, AWSUtils.class);
        archive.addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml");
        return archive;
    }

    public static void assertNoStaleDomain(AmazonSimpleDBClient client, String when) {
        List<String> staleInstances = client.listDomains().getDomainNames().stream() //
                .filter(name -> !name.startsWith(SDBIntegrationTest.class.getSimpleName())
                        || System.currentTimeMillis() - AWSUtils.toEpochMillis(name) > AWSUtils.HOUR) //
                .collect(Collectors.toList());
        Assert.assertEquals(String.format("Found stale SDB domans %s running the test: %s", when, staleInstances), 0,
                staleInstances.size());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void putAndGetAttributes() throws Exception {

        AmazonSimpleDBClient sdbClient = provider.getClient();
        Assume.assumeNotNull("AWS client not null", sdbClient);

        assertNoStaleDomain(sdbClient, "before");

        try {
            try {
                SDBUtils.createDomain(sdbClient, domainName);

                WildFlyCamelContext camelctx = new WildFlyCamelContext();
                camelctx.getNamingContext().bind("sdbClient", sdbClient);

                camelctx.addRoutes(new RouteBuilder() {
                    public void configure() {
                        from("direct:start").to("aws-sdb://" + domainName + "?amazonSDBClient=#sdbClient");
                    }
                });

                camelctx.start();
                try {
                    ReplaceableAttribute attr = new ReplaceableAttribute("SomeName", "SomeValue", true);

                    ProducerTemplate producer = camelctx.createProducerTemplate();
                    Exchange exchange = producer.send("direct:start", new Processor() {
                        public void process(Exchange exchange) throws Exception {
                            exchange.getIn().setHeader(SdbConstants.OPERATION, SdbOperations.PutAttributes);
                            exchange.getIn().setHeader(SdbConstants.ITEM_NAME, itemName);
                            exchange.getIn().setHeader(SdbConstants.REPLACEABLE_ATTRIBUTES,
                                    Collections.singletonList(attr));
                        }
                    });
                    Assert.assertNull(exchange.getException());

                    int retries = 10;
                    List<Attribute> result = Collections.emptyList();
                    while (result.isEmpty() && 0 < retries--) {
                        exchange = producer.send("direct:start", new Processor() {
                            public void process(Exchange exchange) throws Exception {
                                exchange.getIn().setHeader(SdbConstants.OPERATION, SdbOperations.GetAttributes);
                                exchange.getIn().setHeader(SdbConstants.ITEM_NAME, itemName);
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
                    camelctx.close();
                }
            } finally {
                sdbClient.deleteDomain(new DeleteDomainRequest(domainName));
            }
        } finally {
            assertNoStaleDomain(sdbClient, "after");
        }
    }
}
