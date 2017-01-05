/*
 * #%L
 * Wildfly Camel :: Testsuite
 * %%
 * Copyright (C) 2013 - 2014 RedHat
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

package org.wildfly.camel.test.zookeeper;

import java.util.concurrent.TimeUnit;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.PollingConsumer;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.ExchangeBuilder;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.impl.DefaultCamelContext;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.camel.test.common.utils.AvailablePortFinder;
import org.wildfly.camel.test.common.zookeeper.EmbeddedZookeeper;
import org.wildfly.extension.camel.CamelAware;

@CamelAware
@RunWith(Arquillian.class)
public class ZookeeperConsumerIntegrationTest {

    static EmbeddedZookeeper server;

    @Deployment
    public static JavaArchive deployment() {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "zookeeper-integration-tests");
        archive.addClasses(EmbeddedZookeeper.class, AvailablePortFinder.class);
        return archive;
    }

    @Before
    public void before() throws Exception {
        server = new EmbeddedZookeeper().startup(1, TimeUnit.SECONDS);
    }

    @After
    public void after() throws Exception {
        if (server != null) {
            server.shutdown();
        }
    }

    @Test
    public void testZookeeperConsumer() throws Exception {

        CamelContext camelctx = new DefaultCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("zookeeper://" + server.getConnection() + "/somenode")
                .to("seda:end");
            }
        });

        PollingConsumer pollingConsumer = camelctx.getEndpoint("seda:end").createPollingConsumer();
        pollingConsumer.start();

        camelctx.start();
        try {
            // Write payload to znode
            ProducerTemplate producer = camelctx.createProducerTemplate();
            Exchange exchange = ExchangeBuilder.anExchange(camelctx).withBody("Kermit").build();
            producer.send("zookeeper://" + server.getConnection() + "/somenode?create=true", exchange);

            // Read back from the znode
            String result = pollingConsumer.receive(3000).getIn().getBody(String.class);
            Assert.assertEquals("Kermit", result);
        } finally {
            camelctx.stop();
        }
    }
}
