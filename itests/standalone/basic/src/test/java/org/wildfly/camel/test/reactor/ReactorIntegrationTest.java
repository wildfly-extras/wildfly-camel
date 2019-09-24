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
package org.wildfly.camel.test.reactor;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import javax.naming.Context;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.FailedToStartRouteException;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.component.reactive.streams.api.CamelReactiveStreams;
import org.apache.camel.component.reactive.streams.api.CamelReactiveStreamsService;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.support.DefaultExchange;
import org.apache.camel.support.ExchangeHelper;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.reactivestreams.Publisher;
import org.wildfly.extension.camel.CamelAware;
import org.wildfly.extension.camel.WildFlyCamelContext;

import reactor.core.Disposable;
import reactor.core.publisher.Flux;

@CamelAware
@RunWith(Arquillian.class)
public class ReactorIntegrationTest {

    static final AtomicBoolean boundAlready = new AtomicBoolean();

    @Deployment
    public static JavaArchive createDeployment() {
        return ShrinkWrap.create(JavaArchive.class, "camel-reactor-tests.jar");
    }

    public static class SampleBean {
        public String hello(String name) {
            return "Hello " + name;
        }
    }

    @Test
    public void testFromStreamDirect() throws Exception {

        CamelContext camelctx = new DefaultCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            public void configure() {
                from("direct:reactive")
                    .to("reactive-streams:numbers");
            }
        });

        camelctx.start();
        try {
            AtomicInteger value = new AtomicInteger(0);

            CamelReactiveStreamsService crs = CamelReactiveStreams.get(camelctx);
            Flux.from(crs.fromStream("numbers", Integer.class))
                .doOnNext(res -> Assert.assertEquals(value.incrementAndGet(), res.intValue()))
                .subscribe();

            ProducerTemplate template = camelctx.createProducerTemplate();
            template.sendBody("direct:reactive", 1);
            template.sendBody("direct:reactive", 2);
            template.sendBody("direct:reactive", 3);

            Assert.assertEquals(3, value.get());
        } finally {
            camelctx.close();
        }
    }

    @Test
    public void testFromStreamTimer() throws Exception {

        CamelContext camelctx = new DefaultCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("timer:tick?period=5&repeatCount=30")
                    .setBody().header(Exchange.TIMER_COUNTER)
                    .to("reactive-streams:tick");
            }
        });

        final int num = 30;
        final CountDownLatch latch = new CountDownLatch(num);
        final AtomicInteger value = new AtomicInteger(0);

        CamelReactiveStreamsService crs = CamelReactiveStreams.get(camelctx);
        Flux.from(crs.fromStream("tick", Integer.class))
            .doOnNext(res -> Assert.assertEquals(value.incrementAndGet(), res.intValue()))
            .doOnNext(n -> latch.countDown())
            .subscribe();

        camelctx.start();
        try {
            latch.await(5, TimeUnit.SECONDS);
            Assert.assertEquals(num, value.get());
        } finally {
            camelctx.close();
        }
    }

    @Test
    public void testFromStreamMultipleSubscriptionsWithDirect() throws Exception {

        CamelContext camelctx = new DefaultCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:reactive")
                    .to("reactive-streams:direct");
            }
        });

        camelctx.start();
        try {
            CamelReactiveStreamsService crs = CamelReactiveStreams.get(camelctx);

            CountDownLatch latch1 = new CountDownLatch(2);
            Flux.from(crs.fromStream("direct", Integer.class))
                .doOnNext(res -> latch1.countDown())
                .subscribe();

            CountDownLatch latch2 = new CountDownLatch(2);
            Flux.from(crs.fromStream("direct", Integer.class))
                .doOnNext(res -> latch2.countDown())
                .subscribe();

            ProducerTemplate template = camelctx.createProducerTemplate();
            template.sendBody("direct:reactive", 1);
            template.sendBody("direct:reactive", 2);

            Assert.assertTrue(latch1.await(5, TimeUnit.SECONDS));
            Assert.assertTrue(latch2.await(5, TimeUnit.SECONDS));
        } finally {
            camelctx.close();
        }
    }

    @Test
    public void testMultipleSubscriptionsWithTimer() throws Exception {

        CamelContext camelctx = new DefaultCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("timer:tick?period=50")
                    .setBody().header(Exchange.TIMER_COUNTER)
                    .to("reactive-streams:tick");
            }
        });

        CountDownLatch latch1 = new CountDownLatch(5);
        CamelReactiveStreamsService crs = CamelReactiveStreams.get(camelctx);
        Disposable disp1 = Flux.from(crs.fromStream("tick", Integer.class)).subscribe(res -> latch1.countDown());

        camelctx.start();
        try {
            // Add another subscription
            CountDownLatch latch2 = new CountDownLatch(5);
            Disposable disp2 = Flux.from(crs.fromStream("tick", Integer.class)).subscribe(res -> latch2.countDown());

            Assert.assertTrue(latch1.await(5, TimeUnit.SECONDS));
            Assert.assertTrue(latch2.await(5, TimeUnit.SECONDS));

            // Un subscribe both
            disp1.dispose();
            disp2.dispose();

            // No active subscriptions, warnings expected
            Thread.sleep(60);

            // Add another subscription
            CountDownLatch latch3 = new CountDownLatch(5);
            Disposable disp3 = Flux.from(crs.fromStream("tick", Integer.class)).subscribe(res -> latch3.countDown());

            Assert.assertTrue(latch3.await(5, TimeUnit.SECONDS));
            disp3.dispose();
        } finally {
            camelctx.close();
        }
    }

    @Test
    public void testFrom() throws Exception {
        CamelContext camelctx = new DefaultCamelContext();
        camelctx.start();
        try {
            CamelReactiveStreamsService crs = CamelReactiveStreams.get(camelctx);
            Publisher<Exchange> timer = crs.from("timer:reactive?period=250&repeatCount=3");

            AtomicInteger value = new AtomicInteger(0);
            CountDownLatch latch = new CountDownLatch(3);

            Flux.from(timer)
                .map(exchange -> ExchangeHelper.getHeaderOrProperty(exchange, Exchange.TIMER_COUNTER, Integer.class))
                .doOnNext(res -> Assert.assertEquals(value.incrementAndGet(), res.intValue()))
                .doOnNext(res -> latch.countDown())
                .subscribe();

            Assert.assertTrue(latch.await(2, TimeUnit.SECONDS));
        } finally {
            camelctx.close();
        }
    }

    @Test
    public void testFromPublisher() throws Exception {

        CamelContext camelctx = new DefaultCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:source")
                    .to("direct:stream")
                    .setBody()
                        .simple("after stream: ${body}");
            }
        });

        camelctx.start();
        try {
            CamelReactiveStreamsService crs = CamelReactiveStreams.get(camelctx);
            crs.process("direct:stream",
                publisher ->
                    Flux.from(publisher)
                        .map(e -> {
                            int i = e.getIn().getBody(Integer.class);
                            e.getOut().setBody(-i);

                            return e;
                        }
                    )
            );

            ProducerTemplate template = camelctx.createProducerTemplate();
            for (int i = 1; i <= 3; i++) {
                Assert.assertEquals(
                    "after stream: " + (-i),
                    template.requestBody("direct:source", i, String.class)
                );
            }
        } finally {
            camelctx.close();
        }
    }

    @Test
    public void testFromPublisherWithConversion() throws Exception {
        CamelContext camelctx = new DefaultCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:source")
                    .to("direct:stream")
                    .setBody()
                        .simple("after stream: ${body}");
            }
        });

        camelctx.start();
        try {
            CamelReactiveStreamsService crs = CamelReactiveStreams.get(camelctx);

            crs.process("direct:stream",
                Integer.class,
                publisher ->
                    Flux.from(publisher).map(Math::negateExact)
            );

            ProducerTemplate template = camelctx.createProducerTemplate();
            for (int i = 1; i <= 3; i++) {
                Assert.assertEquals(
                    "after stream: " + (-i),
                    template.requestBody("direct:source", i, String.class)
                );
            }
        } finally {
            camelctx.close();
        }
    }

    // ************************************************
    // toStream/to
    // ************************************************

    @Test
    public void testToStream() throws Exception {
        CamelContext camelctx = new DefaultCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            public void configure() {
                from("reactive-streams:reactive")
                    .setBody().constant("123");
            }
        });

        camelctx.start();
        try {
            CamelReactiveStreamsService crs = CamelReactiveStreams.get(camelctx);
            Publisher<Exchange> publisher = crs.toStream("reactive", new DefaultExchange(camelctx));
            Exchange res = Flux.from(publisher).blockFirst();

            Assert.assertNotNull(res);

            String content = res.getIn().getBody(String.class);

            Assert.assertNotNull(content);
            Assert.assertEquals("123", content);
        } finally {
            camelctx.close();
        }
    }

    @Test
    public void testTo() throws Exception {
        CamelContext camelctx = createWildFlyCamelContext();
        camelctx.start();
        try {
            CamelReactiveStreamsService crs = CamelReactiveStreams.get(camelctx);

            Set<String> values = Collections.synchronizedSet(new TreeSet<>());
            CountDownLatch latch = new CountDownLatch(3);

            Flux.just(1, 2, 3)
                .flatMap(e -> crs.to("bean:hello", e, String.class))
                .doOnNext(res -> values.add(res))
                .doOnNext(res -> latch.countDown())
                .subscribe();

            Assert.assertTrue(latch.await(2, TimeUnit.SECONDS));
            Assert.assertEquals(new TreeSet<>(Arrays.asList("Hello 1", "Hello 2", "Hello 3")), values);
        } finally {
            camelctx.close();
        }
    }

    @Test
    public void testToWithExchange() throws Exception {
        CamelContext camelctx = createWildFlyCamelContext();
        camelctx.start();
        try {
            CamelReactiveStreamsService crs = CamelReactiveStreams.get(camelctx);

            Set<String> values = Collections.synchronizedSet(new TreeSet<>());
            CountDownLatch latch = new CountDownLatch(3);

            Flux.just(1, 2, 3)
                .flatMap(e -> crs.to("bean:hello", e))
                .map(e -> e.getOut())
                .map(e -> e.getBody(String.class))
                .doOnNext(res -> values.add(res))
                .doOnNext(res -> latch.countDown())
                .subscribe();

            Assert.assertTrue(latch.await(2, TimeUnit.SECONDS));
            Assert.assertEquals(new TreeSet<>(Arrays.asList("Hello 1", "Hello 2", "Hello 3")), values);
        } finally {
            camelctx.close();
        }
    }

    @Test
    public void testToFunction() throws Exception {
        CamelContext camelctx = createWildFlyCamelContext();
        camelctx.start();
        try {
            CamelReactiveStreamsService crs = CamelReactiveStreams.get(camelctx);

            /* A TreeSet will order the messages alphabetically regardless of the insertion order
             * This is important because in the Flux returned by Flux.flatMap(Function<? super T, ? extends
             * Publisher<? extends R>>) the emissions may interleave */
            Set<String> values = Collections.synchronizedSet(new TreeSet<>());
            CountDownLatch latch = new CountDownLatch(3);
            Function<Object, Publisher<String>> fun = crs.to("bean:hello", String.class);

            Flux.just(1, 2, 3)
                .flatMap(fun)
                .doOnNext(res -> values.add(res))
                .doOnNext(res -> latch.countDown())
                .subscribe();

            Assert.assertTrue(latch.await(2, TimeUnit.SECONDS));
            Assert.assertEquals(new TreeSet<>(Arrays.asList("Hello 1", "Hello 2", "Hello 3")), values);
        } finally {
            camelctx.close();
        }
    }

    @Test
    public void testToFunctionWithExchange() throws Exception {
        CamelContext camelctx = createWildFlyCamelContext();
        camelctx.start();
        try {
            CamelReactiveStreamsService crs = CamelReactiveStreams.get(camelctx);
            Set<String> values = Collections.synchronizedSet(new TreeSet<>());
            CountDownLatch latch = new CountDownLatch(3);
            Function<Object, Publisher<Exchange>> fun = crs.to("bean:hello");

            Flux.just(1, 2, 3)
                .flatMap(fun)
                .map(e -> e.getOut())
                .map(e -> e.getBody(String.class))
                .doOnNext(res -> values.add(res))
                .doOnNext(res -> latch.countDown())
                .subscribe();

            Assert.assertTrue(latch.await(2, TimeUnit.SECONDS));
            Assert.assertEquals(new TreeSet<>(Arrays.asList("Hello 1", "Hello 2", "Hello 3")), values);
        } finally {
            camelctx.close();
        }
    }

    @Test
    public void testSubscriber() throws Exception {
        CamelContext camelctx = new DefaultCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:reactor")
                    .to("mock:result");
            }
        });

        camelctx.start();
        try {
            CamelReactiveStreamsService crs = CamelReactiveStreams.get(camelctx);

            Flux.just(1, 2, 3)
                .subscribe(crs.subscriber("direct:reactor", Integer.class));

            MockEndpoint mock = camelctx.getEndpoint("mock:result", MockEndpoint.class);
            mock.expectedMessageCount(3);
            mock.assertIsSatisfied();

            int idx = 1;
            for (Exchange ex : mock.getExchanges()) {
                Assert.assertEquals(new Integer(idx++), ex.getIn().getBody(Integer.class));
            }
        } finally {
            camelctx.close();
        }
    }

    @Test(expected = FailedToStartRouteException.class)
    public void testOnlyOneCamelProducerPerPublisher() throws Exception {
        CamelContext camelctx = new DefaultCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:one")
                    .to("reactive-streams:stream");
                from("direct:two")
                    .to("reactive-streams:stream");
            }
        });

        camelctx.start();
    }

    private CamelContext createWildFlyCamelContext() throws Exception {
        WildFlyCamelContext camelctx = new WildFlyCamelContext();
        if (boundAlready.compareAndSet(false, true)) {
            Context jndictx = camelctx.getNamingContext();
            jndictx.bind("hello", new SampleBean());
        }
        return camelctx;
    }
}
