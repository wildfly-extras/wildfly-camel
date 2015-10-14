/*
 * #%L
 * Wildfly Camel :: Testsuite
 * %%
 * Copyright (C) 2013 - 2015 RedHat
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

package org.wildfly.camel.test.threading;

import java.io.IOException;
import java.util.concurrent.ExecutorService;

import javax.annotation.Resource;
import javax.inject.Inject;

import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.impl.DefaultCamelContext;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.camel.test.threading.subA.InventoryService;
import org.wildfly.camel.test.threading.subA.WildFlyCamelThreadPoolFactory;
import org.wildfly.extension.camel.CamelAware;

@CamelAware
@RunWith(Arquillian.class)
public class ThreadingIntegrationTest {

    @Resource(lookup = "java:jboss/ee/concurrency/executor/default")
    ExecutorService executorService;

    @Inject
    WildFlyCamelThreadPoolFactory wildFlyCamelThreadPoolFactory;

    @Deployment
    public static JavaArchive createDeployment() throws IOException {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "camel-thread-tests.jar");
        archive.addPackage(WildFlyCamelThreadPoolFactory.class.getPackage());
        archive.addAsResource("threading/bigfile.csv", "bigfile.csv");
        archive.addAsResource("threading/smallfile.csv", "smallfile.csv");
        archive.addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml");
        return archive;
    }

    @Test
    public void testCustomThreadPoolFactory() throws Exception {
        CamelContext camelctx = new DefaultCamelContext();
        camelctx.getExecutorServiceManager().setThreadPoolFactory(wildFlyCamelThreadPoolFactory);

        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:start")
                    .log("Starting to process big file: ${header.CamelFileName}")
                    .split(body().tokenize("\n")).streaming().parallelProcessing()
                    .bean(InventoryService.class, "csvToObject")
                    .to("direct:update")
                    .end()
                    .log("Done processing big file: ${header.CamelFileName}");

                from("direct:update")
                    .bean(InventoryService.class, "updateInventory")
                    .to("mock:end");
            }
        });
        camelctx.start();
        try {
            MockEndpoint mockEndpoint = camelctx.getEndpoint("mock:end", MockEndpoint.class);
            mockEndpoint.expectedMessageCount(100);

            ProducerTemplate producerTemplate = camelctx.createProducerTemplate();
            producerTemplate.requestBody("direct:start", getClass().getResource("/bigfile.csv"));

            mockEndpoint.assertIsSatisfied();
        } finally {
            camelctx.stop();
        }
    }

    @Test
    public void testContainerManagedExecutorService() throws Exception {
        CamelContext camelctx = new DefaultCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:start")
                    .log("Starting to process big file: ${header.CamelFileName}")
                    .split(body().tokenize("\n")).streaming().parallelProcessing().executorService(executorService)
                    .bean(InventoryService.class, "csvToObject")
                    .to("direct:update")
                    .end()
                    .log("Done processing big file: ${header.CamelFileName}");

                from("direct:update")
                    .bean(InventoryService.class, "updateInventory")
                    .to("mock:end");
            }
        });
        camelctx.start();
        try {
            MockEndpoint mockEndpoint = camelctx.getEndpoint("mock:end", MockEndpoint.class);
            mockEndpoint.expectedMessageCount(10);

            ProducerTemplate producerTemplate = camelctx.createProducerTemplate();
            producerTemplate.requestBody("direct:start", getClass().getResource("/smallfile.csv"));

            mockEndpoint.assertIsSatisfied();
        } finally {
            camelctx.stop();
        }
    }
}
