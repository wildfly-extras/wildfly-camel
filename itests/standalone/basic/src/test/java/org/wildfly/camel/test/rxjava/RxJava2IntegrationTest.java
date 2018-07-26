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
package org.wildfly.camel.test.rxjava;

import io.reactivex.Flowable;

import java.util.concurrent.atomic.AtomicLong;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.component.reactive.streams.ReactiveStreamsComponent;
import org.apache.camel.component.reactive.streams.ReactiveStreamsConstants;
import org.apache.camel.component.reactive.streams.api.CamelReactiveStreams;
import org.apache.camel.component.reactive.streams.api.CamelReactiveStreamsService;
import org.apache.camel.component.rxjava2.engine.RxJavaStreamsConstants;
import org.apache.camel.impl.DefaultCamelContext;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.wildfly.extension.camel.CamelAware;

@CamelAware
@RunWith(Arquillian.class)
public class RxJava2IntegrationTest {

    @Deployment
    public static JavaArchive createDeployment() {
        return ShrinkWrap.create(JavaArchive.class, "camel-rxjava2-tests.jar");
    }

    @Test
    public void testOnCompleteHeaderForwarded() throws Exception {
        CamelContext camelctx = createCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("reactive-streams:numbers?forwardOnComplete=true")
                    .to("mock:endpoint");
            }
        });

        CamelReactiveStreamsService crs = CamelReactiveStreams.get(camelctx);
        Subscriber<Integer> numbers = crs.streamSubscriber("numbers", Integer.class);

        camelctx.start();
        try {
            Flowable.<Integer>empty().subscribe(numbers);

            MockEndpoint endpoint = camelctx.getEndpoint("mock:endpoint", MockEndpoint.class);
            endpoint.expectedMessageCount(1);
            endpoint.expectedHeaderReceived(ReactiveStreamsConstants.REACTIVE_STREAMS_EVENT_TYPE, "onComplete");
            endpoint.expectedBodiesReceived(new Object[]{null});
            endpoint.assertIsSatisfied();
        } finally {
            camelctx.stop();
        }
    }

    @Test
    public void testOnCompleteHeaderNotForwarded() throws Exception {
        CamelContext camelctx = createCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("reactive-streams:numbers")
                    .to("mock:endpoint");
            }
        });

        CamelReactiveStreamsService crs = CamelReactiveStreams.get(camelctx);
        Subscriber<Integer> numbers = crs.streamSubscriber("numbers", Integer.class);

        camelctx.start();
        try {
            Flowable.<Integer>empty().subscribe(numbers);

            MockEndpoint endpoint = camelctx.getEndpoint("mock:endpoint", MockEndpoint.class);
            endpoint.expectedMessageCount(0);
            endpoint.assertIsSatisfied(200);
        } finally {
            camelctx.stop();
        }
    }

    @Test
    public void testOnNextHeaderForwarded() throws Exception {
        CamelContext camelctx = createCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("reactive-streams:numbers")
                    .to("mock:endpoint");
            }
        });

        CamelReactiveStreamsService crs = CamelReactiveStreams.get(camelctx);
        Subscriber<Integer> numbers = crs.streamSubscriber("numbers", Integer.class);

        camelctx.start();
        try {
            Flowable.just(1).subscribe(numbers);

            MockEndpoint endpoint = camelctx.getEndpoint("mock:endpoint", MockEndpoint.class);
            endpoint.expectedHeaderReceived(ReactiveStreamsConstants.REACTIVE_STREAMS_EVENT_TYPE, "onNext");
            endpoint.expectedMessageCount(1);
            endpoint.assertIsSatisfied();

            Exchange ex = endpoint.getExchanges().get(0);
            Assert.assertEquals(1, ex.getIn().getBody());
        } finally {
            camelctx.stop();
        }
    }

    @Test
    public void testOnErrorHeaderForwarded() throws Exception {
        CamelContext camelctx = createCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("reactive-streams:numbers?forwardOnError=true")
                    .to("mock:endpoint");
            }
        });

        CamelReactiveStreamsService crs = CamelReactiveStreams.get(camelctx);
        Subscriber<Integer> numbers = crs.streamSubscriber("numbers", Integer.class);

        camelctx.start();
        try {
            RuntimeException ex = new RuntimeException("1");

            Flowable.just(1)
                .map(n -> {
                    if (n == 1) {
                        throw ex;
                    }
                    return n;
                })
                .subscribe(numbers);


            MockEndpoint endpoint = camelctx.getEndpoint("mock:endpoint", MockEndpoint.class);
            endpoint.expectedMessageCount(1);
            endpoint.expectedHeaderReceived(ReactiveStreamsConstants.REACTIVE_STREAMS_EVENT_TYPE, "onError");
            endpoint.assertIsSatisfied();

            Exchange exchange = endpoint.getExchanges().get(0);
            Assert.assertEquals(ex, exchange.getIn().getBody());
        } finally {
            camelctx.stop();
        }
    }

    @Test
    public void testOnErrorHeaderNotForwarded() throws Exception {
        CamelContext camelctx = createCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("reactive-streams:numbers")
                    .to("mock:endpoint");
            }
        });

        CamelReactiveStreamsService crs = CamelReactiveStreams.get(camelctx);
        Subscriber<Integer> numbers = crs.streamSubscriber("numbers", Integer.class);

        camelctx.start();
        try {
            RuntimeException ex = new RuntimeException("1");

            Flowable.just(1)
                .map(n -> {
                    if (n == 1) {
                        throw ex;
                    }
                    return n;
                })
                .subscribe(numbers);

            MockEndpoint endpoint = camelctx.getEndpoint("mock:endpoint", MockEndpoint.class);
            endpoint.expectedMessageCount(0);
            endpoint.assertIsSatisfied(200);
        } finally {
            camelctx.stop();
        }
    }

    @Test
    public void testSubscriber() throws Exception {
        CamelContext camelctx = createCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("reactive-streams:sub1")
                    .to("mock:sub1");
                from("reactive-streams:sub2")
                    .to("mock:sub2");
                from("timer:tick?period=50")
                    .setBody()
                    .simple("random(500)")
                    .to("mock:sub3")
                    .to("reactive-streams:pub");
            }
        });

        CamelReactiveStreamsService crs = CamelReactiveStreams.get(camelctx);
        Subscriber<Integer> sub1 = crs.streamSubscriber("sub1", Integer.class);
        Subscriber<Integer> sub2 = crs.streamSubscriber("sub2", Integer.class);
        Publisher<Integer> pub = crs.fromStream("pub", Integer.class);

        pub.subscribe(sub1);
        pub.subscribe(sub2);

        camelctx.start();
        try {
            int count = 2;

            MockEndpoint e1 = camelctx.getEndpoint("mock:sub1", MockEndpoint.class);
            e1.expectedMinimumMessageCount(count);
            e1.assertIsSatisfied();

            MockEndpoint e2 = camelctx.getEndpoint("mock:sub2", MockEndpoint.class);
            e2.expectedMinimumMessageCount(count);
            e2.assertIsSatisfied();

            MockEndpoint e3 = camelctx.getEndpoint("mock:sub3", MockEndpoint.class);
            e3.expectedMinimumMessageCount(count);
            e3.assertIsSatisfied();

            for (int i = 0; i < count; i++) {
                Exchange ex1 = e1.getExchanges().get(i);
                Exchange ex2 = e2.getExchanges().get(i);
                Exchange ex3 = e3.getExchanges().get(i);

                Assert.assertEquals(ex1.getIn().getBody(), ex2.getIn().getBody());
                Assert.assertEquals(ex1.getIn().getBody(), ex3.getIn().getBody());
            }
        } finally {
            camelctx.stop();
        }
    }

    @Test
    public void testSingleConsumer() throws Exception {
        CamelContext camelctx = createCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("reactive-streams:singleConsumer")
                    .process()
                    .message(m -> m.setHeader("thread", Thread.currentThread().getId()))
                    .to("mock:singleBucket");
            }
        });

        CamelReactiveStreamsService crs = CamelReactiveStreams.get(camelctx);

        camelctx.start();
        try {

            Flowable.range(0, 1000).subscribe(
                crs.streamSubscriber("singleConsumer", Number.class)
            );

            MockEndpoint endpoint = camelctx.getEndpoint("mock:singleBucket", MockEndpoint.class);
            endpoint.expectedMessageCount(1000);
            endpoint.assertIsSatisfied();

            Assert.assertEquals(
                1,
                endpoint.getExchanges().stream()
                    .map(x -> x.getIn().getHeader("thread", String.class))
                    .distinct()
                    .count()
            );

            // Ensure order is preserved when using a single consumer
            AtomicLong num = new AtomicLong(0);

            endpoint.getExchanges().stream()
                .map(x -> x.getIn().getBody(Long.class))
                .forEach(n -> Assert.assertEquals(num.getAndIncrement(), n.longValue()));
        } finally {
            camelctx.stop();
        }
    }

    @Test
    public void testMultipleConsumers() throws Exception {
        CamelContext camelctx = createCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("reactive-streams:multipleConsumers?concurrentConsumers=3")
                    .process()
                    .message(m -> m.setHeader("thread", Thread.currentThread().getId()))
                    .to("mock:multipleBucket");
            }
        });

        CamelReactiveStreamsService crs = CamelReactiveStreams.get(camelctx);

        camelctx.start();
        try {
            Flowable.range(0, 1000).subscribe(
                crs.streamSubscriber("multipleConsumers", Number.class)
            );

            MockEndpoint endpoint = camelctx.getEndpoint("mock:multipleBucket", MockEndpoint.class);
            endpoint.expectedMessageCount(1000);
            endpoint.assertIsSatisfied();

            Assert.assertEquals(
                3,
                endpoint.getExchanges().stream()
                    .map(x -> x.getIn().getHeader("thread", String.class))
                    .distinct()
                    .count()
            );
        } finally {
            camelctx.stop();
        }
    }

    private CamelContext createCamelContext() throws Exception {
        CamelContext camelctx = new DefaultCamelContext();
        camelctx.addComponent(
            ReactiveStreamsConstants.SCHEME,
            ReactiveStreamsComponent.withServiceType(RxJavaStreamsConstants.SERVICE_NAME)
        );
        return camelctx;
    }
}
