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
package org.wildfly.camel.test.leveldb;

import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

import org.apache.camel.AggregationStrategy;
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.leveldb.LevelDBAggregationRepository;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.impl.DefaultCamelContext;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.camel.test.common.utils.FileUtils;
import org.wildfly.extension.camel.CamelAware;

@CamelAware
@RunWith(Arquillian.class)
public class LevelDBIntegrationTest {

    @Deployment
    public static JavaArchive deployment() {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "camel-leveldb-tests");
        archive.addClasses(FileUtils.class);
        return archive;
    }

    @BeforeClass
    public static void beforeClass() throws Exception {
        FileUtils.deleteDirectory(Paths.get("target/leveldb"));
    }

    @Test
    public void testLevelDBAggregate() throws Exception {

        CamelContext camelctx = new DefaultCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {

                LevelDBAggregationRepository repo = new LevelDBAggregationRepository("repo1", "target/leveldb/leveldb.dat");

                from("direct:start")
                    .aggregate(header("id"), new MyAggregationStrategy())
                        .completionSize(5).aggregationRepository(repo)
                        .to("mock:aggregated");
            }
        });

        MockEndpoint mockAggregated = camelctx.getEndpoint("mock:aggregated", MockEndpoint.class);
        mockAggregated.expectedBodiesReceived("ABCDE");

        camelctx.start();
        try {
            ProducerTemplate template = camelctx.createProducerTemplate();

            template.sendBodyAndHeader("direct:start", "A", "id", 123);
            template.sendBodyAndHeader("direct:start", "B", "id", 123);
            template.sendBodyAndHeader("direct:start", "C", "id", 123);
            template.sendBodyAndHeader("direct:start", "D", "id", 123);
            template.sendBodyAndHeader("direct:start", "E", "id", 123);

            MockEndpoint.assertIsSatisfied(camelctx, 30, TimeUnit.SECONDS);
        } finally {
            camelctx.close();
        }
    }

    static class MyAggregationStrategy implements AggregationStrategy {

        public Exchange aggregate(Exchange oldExchange, Exchange newExchange) {
            if (oldExchange == null) {
                return newExchange;
            }
            String body1 = oldExchange.getIn().getBody(String.class);
            String body2 = newExchange.getIn().getBody(String.class);

            oldExchange.getIn().setBody(body1 + body2);
            return oldExchange;
        }
    }
}
