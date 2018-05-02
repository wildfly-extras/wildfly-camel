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
package org.wildfly.camel.test.saga;

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.camel.CamelContext;
import org.apache.camel.CamelExecutionException;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.impl.saga.InMemorySagaService;
import org.apache.camel.model.SagaPropagation;
import org.apache.camel.saga.CamelSagaService;
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
public class SagaIntegrationTest {

    @Deployment
    public static JavaArchive createDeployment() {
        return ShrinkWrap.create(JavaArchive.class, "camel-saga-tests.jar");
    }

    @Test
    public void testPropagationRequired() throws Exception {
        List<String> sagaIds = new LinkedList<>();

        CamelSagaService sagaService = new InMemorySagaService();

        CamelContext camelctx = new DefaultCamelContext();
        camelctx.addService(sagaService);
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:required")
                    .saga()
                    .process(addSagaIdToList(sagaIds))
                    .to("direct:required2");

                from("direct:required2")
                    .saga().propagation(SagaPropagation.REQUIRED)
                    .process(addSagaIdToList(sagaIds))
                    .to("direct:required3");

                from("direct:required3")
                    .saga()
                    .process(addSagaIdToList(sagaIds));
            }
        });

        camelctx.start();
        try {
            camelctx.createFluentProducerTemplate().to("direct:required").request();

            Assert.assertEquals(3, sagaIds.size());
            assertUniqueNonNullSagaIds(sagaIds, 1);
        } finally {
            camelctx.stop();
        }
    }

    @Test
    public void testPropagationRequiresNew() throws Exception {
        List<String> sagaIds = new LinkedList<>();

        CamelSagaService sagaService = new InMemorySagaService();

        CamelContext camelctx = new DefaultCamelContext();
        camelctx.addService(sagaService);
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:requiresNew")
                    .saga().propagation(SagaPropagation.REQUIRES_NEW)
                    .process(addSagaIdToList(sagaIds))
                    .to("direct:requiresNew2")
                    .to("direct:requiresNew2");

                from("direct:requiresNew2")
                    .saga().propagation(SagaPropagation.REQUIRES_NEW)
                    .process(addSagaIdToList(sagaIds));
            }
        });

        camelctx.start();
        try {
            camelctx.createFluentProducerTemplate().to("direct:requiresNew").request();

            Assert.assertEquals(3, sagaIds.size());
            assertUniqueNonNullSagaIds(sagaIds, 3);
        } finally {
            camelctx.stop();
        }
    }

    @Test
    public void testPropagationNotSupported() throws Exception {
        List<String> sagaIds = new LinkedList<>();

        CamelSagaService sagaService = new InMemorySagaService();

        CamelContext camelctx = new DefaultCamelContext();
        camelctx.addService(sagaService);
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:notSupported")
                    .process(addSagaIdToList(sagaIds))
                    .to("direct:notSupported2")
                    .to("direct:notSupported3");

                from("direct:notSupported2")
                    .saga()
                    .process(addSagaIdToList(sagaIds))
                    .to("direct:notSupported3");

                from("direct:notSupported3")
                    .saga().propagation(SagaPropagation.NOT_SUPPORTED)
                    .process(addSagaIdToList(sagaIds));
            }
        });

        camelctx.start();
        try {
            camelctx.createFluentProducerTemplate().to("direct:notSupported").request();

            Assert.assertEquals(4, sagaIds.size());
            assertNonNullSagaIds(sagaIds, 1);
        } finally {
            camelctx.stop();
        }
    }

    @Test
    public void testPropagationSupports() throws Exception {
        List<String> sagaIds = new LinkedList<>();

        CamelSagaService sagaService = new InMemorySagaService();

        CamelContext camelctx = new DefaultCamelContext();
        camelctx.addService(sagaService);
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:supports")
                    .to("direct:supports2")
                    .to("direct:supports3");

                from("direct:supports2")
                    .saga()
                    .to("direct:supports3");

                from("direct:supports3")
                    .saga().propagation(SagaPropagation.SUPPORTS)
                    .process(addSagaIdToList(sagaIds));
            }
        });

        camelctx.start();
        try {
            camelctx.createFluentProducerTemplate().to("direct:supports").request();

            Assert.assertEquals(2, sagaIds.size());
            assertNonNullSagaIds(sagaIds, 1);
        } finally {
            camelctx.stop();
        }
    }

    @Test
    public void testPropagationMandatory() throws Exception {
        List<String> sagaIds = new LinkedList<>();

        CamelSagaService sagaService = new InMemorySagaService();

        CamelContext camelctx = new DefaultCamelContext();
        camelctx.addService(sagaService);
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:mandatory")
                    .to("direct:mandatory2");

                from("direct:mandatory2")
                    .saga().propagation(SagaPropagation.MANDATORY)
                    .process(addSagaIdToList(sagaIds));
            }
        });

        camelctx.start();
        try {
            camelctx.createFluentProducerTemplate().to("direct:mandatory").request();
            Assert.fail("Expected CamelExecutionException to be thrown");
        } catch (CamelExecutionException e) {
            // Expected
        } finally {
            camelctx.stop();
        }
    }

    @Test
    public void testPropagationNever() throws Exception {
        List<String> sagaIds = new LinkedList<>();

        CamelSagaService sagaService = new InMemorySagaService();

        CamelContext camelctx = new DefaultCamelContext();
        camelctx.addService(sagaService);
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:never")
                    .saga()
                    .to("direct:never2");

                from("direct:never2")
                    .saga().propagation(SagaPropagation.NEVER)
                    .process(addSagaIdToList(sagaIds));
            }
        });

        camelctx.start();
        try {
            camelctx.createFluentProducerTemplate().to("direct:never").request();
            Assert.fail("Expected CamelExecutionException to be thrown");
        } catch (CamelExecutionException e) {
            // Expected
        } finally {
            camelctx.stop();
        }
    }

    private Processor addSagaIdToList(List<String> sagaIds) {
        return ex -> sagaIds.add(ex.getIn().getHeader(Exchange.SAGA_LONG_RUNNING_ACTION, String.class));
    }

    private void assertUniqueNonNullSagaIds(List<String> sagaIds, int num) {
        Set<String> uniqueNonNull = sagaIds.stream().filter(Objects::nonNull).collect(Collectors.toSet());
        if (uniqueNonNull.size() != num) {
            Assert.fail("Expeced size " + num + ", actual " + uniqueNonNull.size());
        }
    }

    private void assertNonNullSagaIds(List<String> sagaIds, int num) {
        List<String> nonNull = sagaIds.stream().filter(Objects::nonNull).collect(Collectors.toList());
        if (nonNull.size() != num) {
            Assert.fail("Expeced size " + num + ", actual " + nonNull.size());
        }
    }
}
