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

package org.wildfly.camel.test.sql;

import java.io.IOException;
import java.util.Map;

import javax.annotation.Resource;
import javax.sql.DataSource;

import org.apache.camel.CamelContext;
import org.apache.camel.PollingConsumer;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.processor.idempotent.jdbc.JdbcMessageIdRepository;
import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.camel.test.sql.subA.CdiRouteBuilder;
import org.wildfly.extension.camel.CamelAware;
import org.wildfly.extension.camel.CamelContextRegistry;

@CamelAware
@RunWith(Arquillian.class)
public class SQLIntegrationTest {

    private static final String CAMEL_SQL_CDI_ROUTES_JAR = "camel-sql-cdi-routes.jar";

    @ArquillianResource
    CamelContextRegistry contextRegistry;

    @ArquillianResource
    Deployer deployer;

    @Resource(name = "java:jboss/datasources/ExampleDS")
    DataSource dataSource;

    @Deployment
    public static JavaArchive createDeployment() throws IOException {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "camel-sql-tests.jar");
        archive.addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml");
        return archive;
    }

    @Deployment(managed = false, name = CAMEL_SQL_CDI_ROUTES_JAR)
    public static JavaArchive createCDIDeployment() throws IOException {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, CAMEL_SQL_CDI_ROUTES_JAR);
        archive.addPackage(CdiRouteBuilder.class.getPackage());
        archive.addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml");
        return archive;
    }

    @Test
    public void testSQLEndpoint() throws Exception {
        Assert.assertNotNull("DataSource not null", dataSource);

        CamelContext camelctx = new DefaultCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("sql:select name from information_schema.users?dataSource=java:jboss/datasources/ExampleDS")
                .to("direct:end");
            }
        });

        camelctx.start();
        try {
            PollingConsumer pollingConsumer = camelctx.getEndpoint("direct:end").createPollingConsumer();
            pollingConsumer.start();

            String result = (String) pollingConsumer.receive().getIn().getBody(Map.class).get("NAME");
            Assert.assertEquals("SA", result);
        } finally {
            camelctx.stop();
        }
    }

    @Test
    public void testSQLEndpointWithCDIContext() throws Exception {
        try {
            deployer.deploy(CAMEL_SQL_CDI_ROUTES_JAR);

            CamelContext camelctx = contextRegistry.getCamelContext("camel-sql-cdi-context");
            Assert.assertNotNull("Camel context not null", camelctx);

            PollingConsumer pollingConsumer = camelctx.getEndpoint("direct:end").createPollingConsumer();
            pollingConsumer.start();

            String result = (String) pollingConsumer.receive().getIn().getBody(Map.class).get("NAME");
            Assert.assertEquals("SA", result);
        } finally {
            deployer.undeploy(CAMEL_SQL_CDI_ROUTES_JAR);
        }
    }

    @Test
    public void testSqlIdempotentConsumer() throws Exception {
        Assert.assertNotNull("DataSource not null", dataSource);

        final JdbcMessageIdRepository jdbcMessageIdRepository = new JdbcMessageIdRepository(dataSource, "myProcessorName");

        CamelContext camelctx = new DefaultCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:start")
                .idempotentConsumer(simple("${header.messageId}"), jdbcMessageIdRepository)
                .to("mock:result");
            }
        });

        camelctx.start();
        try {
            MockEndpoint mockEndpoint = camelctx.getEndpoint("mock:result", MockEndpoint.class);
            mockEndpoint.expectedMessageCount(1);

            // Send 5 messages with the same messageId header. Only 1 should be forwarded to the mock:result endpoint
            ProducerTemplate template = camelctx.createProducerTemplate();
            for (int i = 0; i < 5; i++) {
                template.requestBodyAndHeader("direct:start", null, "messageId", "12345");
            }

            mockEndpoint.assertIsSatisfied();
        } finally {
            camelctx.stop();
        }
    }
}
