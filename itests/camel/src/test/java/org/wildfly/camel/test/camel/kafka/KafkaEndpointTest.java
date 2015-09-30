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

package org.wildfly.camel.test.camel.kafka;

import java.util.concurrent.TimeUnit;

import org.apache.camel.CamelContext;
import org.apache.camel.Endpoint;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.impl.DefaultCamelContext;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.wildfly.camel.test.common.zookeeper.EmbeddedZookeeperServer;

public class KafkaEndpointTest {

    static EmbeddedZookeeperServer server;

    @Before
    public void before() throws Exception {
        server = new EmbeddedZookeeperServer().startup(1, TimeUnit.SECONDS);
    }

    @After
    public void after() throws Exception {
        if (server != null) {
            server.shutdown();
        }
    }

    @Test
    public void testKafkaEndpoint() throws Exception {

        String zkhost = "&zookeeperHost=localhost";
        String zkport = "&zookeeperPort=" + server.getServerPort();
        String serializer = "&serializerClass=kafka.serializer.StringEncoder";
        final String epuri = "kafka:localhost:9092?topic=test&groupId=group1" + zkhost + zkport + serializer;

        CamelContext camelctx = new DefaultCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:start").to(epuri);
            }
        });

        camelctx.start();
        try {
            Endpoint endpoint = camelctx.getEndpoint(epuri);
            Assert.assertEquals(endpoint.getClass().getName(), "org.apache.camel.component.kafka.KafkaEndpoint");

            // [TODO] Send and verify an actual message
            //ProducerTemplate producer = camelctx.createProducerTemplate();
            //String result = producer.requestBody("direct:start", "Kermit", String.class);

        } finally {
            camelctx.stop();
        }
    }
}
