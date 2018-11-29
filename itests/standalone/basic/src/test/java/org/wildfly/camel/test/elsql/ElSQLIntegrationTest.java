/*
 * #%L
 * Wildfly Camel :: Testsuite
 * %%
 * Copyright (C) 2013 - 2017 RedHat
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
package org.wildfly.camel.test.elsql;

import java.util.List;
import java.util.Map;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.support.jndi.JndiBeanRepository;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.extension.camel.CamelAware;

@CamelAware
@RunWith(Arquillian.class)
public class ElSQLIntegrationTest {

    @Deployment
    public static JavaArchive createDeployment() {
        return ShrinkWrap.create(JavaArchive.class, "camel-elsql-tests.jar")
            .addAsResource("elsql/users.elsql", "users.elsql");
    }

    @Test
    public void testElSQLConsumer() throws Exception {
        CamelContext camelctx = new DefaultCamelContext(new JndiBeanRepository());
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("elsql:dbusers:users.elsql?dataSource=java:jboss/datasources/ExampleDS")
                .to("mock:end");
            }
        });

        MockEndpoint mockEndpoint = camelctx.getEndpoint("mock:end", MockEndpoint.class);
        mockEndpoint.expectedMessageCount(1);

        camelctx.start();
        try {
            mockEndpoint.assertIsSatisfied();

            List<Exchange> exchanges = mockEndpoint.getReceivedExchanges();
            Assert.assertEquals("SA", exchanges.get(0).getIn().getBody(Map.class).get("NAME"));
        } finally {
            camelctx.stop();
        }
    }

    @Test
    public void testElSQLProducer() throws Exception {
        CamelContext camelctx = new DefaultCamelContext(new JndiBeanRepository());
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:start")
                .to("elsql:dbusers:users.elsql?dataSource=java:jboss/datasources/ExampleDS");
            }
        });

        camelctx.start();
        try {
            ProducerTemplate template = camelctx.createProducerTemplate();
            List<Map<String, String>> result = template.requestBody("direct:start", null, List.class);
            Map<String, String> row = result.get(0);
            Assert.assertEquals("SA", row.get("NAME"));
        } finally {
            camelctx.stop();
        }
    }
}
