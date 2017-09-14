/*
 * #%L
 * Wildfly Camel :: Testsuite
 * %%
 * Copyright (C) 2013 - 2016 RedHat
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

package org.wildfly.camel.test.ignite;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import javax.cache.Cache.Entry;
import javax.cache.expiry.CreatedExpiryPolicy;
import javax.cache.expiry.Duration;

import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.ignite.IgniteConstants;
import org.apache.camel.component.ignite.cache.IgniteCacheComponent;
import org.apache.camel.component.ignite.compute.IgniteComputeComponent;
import org.apache.camel.component.ignite.events.IgniteEventsComponent;
import org.apache.camel.component.ignite.idgen.IgniteIdGenComponent;
import org.apache.camel.component.ignite.messaging.IgniteMessagingComponent;
import org.apache.camel.component.ignite.queue.IgniteQueueComponent;
import org.apache.camel.component.ignite.set.IgniteSetComponent;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.IgniteException;
import org.apache.ignite.IgniteQueue;
import org.apache.ignite.IgniteSet;
import org.apache.ignite.Ignition;
import org.apache.ignite.cache.CachePeekMode;
import org.apache.ignite.cache.query.Query;
import org.apache.ignite.cache.query.ScanQuery;
import org.apache.ignite.compute.ComputeJob;
import org.apache.ignite.compute.ComputeJobResult;
import org.apache.ignite.compute.ComputeTaskSplitAdapter;
import org.apache.ignite.configuration.CollectionConfiguration;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.events.CacheEvent;
import org.apache.ignite.events.Event;
import org.apache.ignite.events.EventType;
import org.apache.ignite.lang.IgniteBiPredicate;
import org.apache.ignite.lang.IgniteCallable;
import org.apache.ignite.lang.IgniteReducer;
import org.apache.ignite.lang.IgniteRunnable;
import org.apache.ignite.spi.discovery.tcp.TcpDiscoverySpi;
import org.apache.ignite.spi.discovery.tcp.ipfinder.vm.TcpDiscoveryVmIpFinder;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.extension.camel.CamelAware;

/**
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 */
@CamelAware
@RunWith(Arquillian.class)
public class IgniteIntegrationTest {

    private static class ConcatReducer implements IgniteReducer<String, String> {
        private static final long serialVersionUID = 1L;
        private List<String> list = new ArrayList<>();

        @Override
        public boolean collect(String value) {
            list.add(value);
            return true;
        }

        @Override
        public String reduce() {
            String answer = list.stream().collect(Collectors.joining());
            list.clear();
            return answer;
        }
    }

    private class Counter implements IgniteRunnable {
        private static final long serialVersionUID = 386219709871673366L;

        @Override
        public void run() {
            counter.incrementAndGet();
        }
    }

    private static class HelloCallable implements IgniteCallable<String> {
        private static final long serialVersionUID = 986972344531961815L;

        @Override
        public String call() throws Exception {
            return "hello";
        }
    }

    private static class TopicListener implements IgniteBiPredicate<UUID, Object> {
        private static final long serialVersionUID = 1L;
        private final BlockingQueue<Object> messages = new LinkedBlockingQueue<>();

        @Override
        public boolean apply(UUID uuid, Object message) {
            try {
                messages.put(message);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return true;
        }
    }

    private static final AtomicInteger counter = new AtomicInteger(0);

    private static final Random rnd = new Random();

    @Deployment
    public static JavaArchive createdeployment() {
        return ShrinkWrap.create(JavaArchive.class, "camel-ignite-tests") //
        // .addClass(HelloCallable.class) //
        // .addClass(ConcatReducer.class)
        ;
    }

    private static String randomCacheName() {
        return IgniteIntegrationTest.class.getSimpleName() + "-cache-" + Math.abs(rnd.nextInt());
    }

    @Test
    public void cache() throws Exception {

        final String cacheName = randomCacheName();

        final CamelContext camelctx = new DefaultCamelContext();

        final IgniteCacheComponent igniteComponent = camelctx.getComponent("ignite-cache", IgniteCacheComponent.class);
        igniteComponent.setIgniteConfiguration(new IgniteConfiguration());

        camelctx.start();

        try {

            final IgniteCache<String, String> cache = igniteComponent.getIgnite().getOrCreateCache(cacheName);
            Assert.assertEquals(0, cache.size(CachePeekMode.ALL));

            final ProducerTemplate template = camelctx.createProducerTemplate();

            /* Put single */
            template.requestBodyAndHeader("ignite-cache:" + cacheName + "?operation=PUT", "v1",
                    IgniteConstants.IGNITE_CACHE_KEY, "k1");
            Assert.assertEquals(1, cache.size(CachePeekMode.ALL));
            Assert.assertEquals("v1", cache.get("k1"));

            cache.removeAll();
            Assert.assertEquals(0, cache.size(CachePeekMode.ALL));

            /* Put multiple */
            final Map<String, String> entries = new LinkedHashMap<String, String>() {
                private static final long serialVersionUID = 1L;
                {
                    put("k2", "v2");
                    put("k3", "v3");
                }
            };
            template.requestBody("ignite-cache:" + cacheName + "?operation=PUT", entries);
            Assert.assertEquals(2, cache.size(CachePeekMode.ALL));
            Assert.assertEquals("v2", cache.get("k2"));
            Assert.assertEquals("v3", cache.get("k3"));

            /* Get one */
            Assert.assertEquals("v2",
                    template.requestBody("ignite-cache:" + cacheName + "?operation=GET", "k2", String.class));
            Assert.assertEquals("v3", template.requestBodyAndHeader("ignite-cache:" + cacheName + "?operation=GET",
                    "this value won't be used", IgniteConstants.IGNITE_CACHE_KEY, "k3", String.class));

            Set<String> keys = new LinkedHashSet<>(Arrays.asList("k2", "k3"));
            /* Get many */
            Assert.assertEquals(entries,
                    template.requestBody("ignite-cache:" + cacheName + "?operation=GET", keys, Map.class));

            /* Size */
            Assert.assertEquals(2, template
                    .requestBody("ignite-cache:" + cacheName + "?operation=SIZE", keys, Integer.class).intValue());

            /* Query */
            Query<Entry<String, String>> query = new ScanQuery<String, String>(new IgniteBiPredicate<String, String>() {
                private static final long serialVersionUID = 1L;

                @Override
                public boolean apply(String key, String value) {
                    return Integer.parseInt(key.replace("k", "")) >= 3;
                }
            });
            List<?> results = template.requestBodyAndHeader("ignite-cache:" + cacheName + "?operation=QUERY", keys,
                    IgniteConstants.IGNITE_CACHE_QUERY, query, List.class);
            Assert.assertEquals(1, results.size());

            /* Remove */
            template.requestBody("ignite-cache:" + cacheName + "?operation=REMOVE", "k2");
            Assert.assertEquals(1, cache.size(CachePeekMode.ALL));
            Assert.assertNull(cache.get("k2"));

            template.requestBodyAndHeader("ignite-cache:" + cacheName + "?operation=REMOVE", "this value won't be used",
                    IgniteConstants.IGNITE_CACHE_KEY, "k3");
            Assert.assertEquals(0, cache.size(CachePeekMode.ALL));
            Assert.assertNull(cache.get("k3"));

            /* Clear */
            cache.put("k4", "v4");
            cache.put("k5", "v5");
            Assert.assertEquals(2, cache.size(CachePeekMode.ALL));
            template.requestBody("ignite-cache:" + cacheName + "?operation=CLEAR", "this value won't be used");
            Assert.assertEquals(0, cache.size(CachePeekMode.ALL));

            cache.clear();

        } finally {
            camelctx.stop();
        }

    }

    @Test
    public void computeCall() throws Exception {
        final CamelContext camelctx = new DefaultCamelContext();

        final IgniteComputeComponent igniteComponent = camelctx.getComponent("ignite-compute",
                IgniteComputeComponent.class);
        igniteComponent.setIgniteConfiguration(new IgniteConfiguration().setClassLoader(getClass().getClassLoader()));

        camelctx.start();

        try {
            final ProducerTemplate template = camelctx.createProducerTemplate();
            final IgniteCallable<String> callable = new HelloCallable();

            /* Single Callable */
            String result = template.requestBody("ignite-compute:abc?executionType=CALL", callable, String.class);
            Assert.assertEquals("hello", result);

            /* Collection of Callables */
            @SuppressWarnings("unchecked")
            Collection<String> colResult = template.requestBody("ignite-compute:abc?executionType=CALL",
                    Arrays.asList(callable, callable, callable), Collection.class);
            Assert.assertEquals(Arrays.asList("hello", "hello", "hello"), colResult);

            /* Callables with a Reducer */
            IgniteReducer<String, String> reducer = new ConcatReducer();
            String reduced = template.requestBodyAndHeader("ignite-compute:abc?executionType=CALL",
                    Arrays.asList(callable, callable, callable), IgniteConstants.IGNITE_COMPUTE_REDUCER, reducer,
                    String.class);
            Assert.assertEquals("hellohellohello", reduced);

        } finally {
            camelctx.stop();
        }

    }

    @Test
    public void computeExecute() throws Exception {
        final CamelContext camelctx = new DefaultCamelContext();

        final IgniteComputeComponent igniteComponent = camelctx.getComponent("ignite-compute",
                IgniteComputeComponent.class);
        igniteComponent.setIgniteConfiguration(newMultiConfig());

        camelctx.start();

        final List<Ignite> igniteInstances = new ArrayList<>();

        ComputeTaskSplitAdapter<Integer, String> task = new ComputeTaskSplitAdapter<Integer, String>() {
            private static final long serialVersionUID = 3040624379256407732L;

            @Override
            public String reduce(List<ComputeJobResult> results) throws IgniteException {
                StringBuilder answer = new StringBuilder();
                for (ComputeJobResult res : results) {
                    Object data = res.getData();
                    answer.append(data).append(",");
                }
                answer.deleteCharAt(answer.length() - 1);
                return answer.toString();
            }

            @Override
            protected Collection<? extends ComputeJob> split(int gridSize, final Integer arg) throws IgniteException {
                Set<ComputeJob> answer = new HashSet<>();
                for (int i = 0; i < arg; i++) {
                    final int c = i;
                    answer.add(new ComputeJob() {
                        private static final long serialVersionUID = 3365213549618276779L;

                        @Override
                        public void cancel() {
                            // nothing
                        }

                        @Override
                        public Object execute() throws IgniteException {
                            return "a" + c;
                        }
                    });
                }
                return answer;
            }
        };

        try {
            igniteInstances.add(Ignition.start(newMultiConfig()));
            igniteInstances.add(Ignition.start(newMultiConfig()));

            final ProducerTemplate template = camelctx.createProducerTemplate();

            String result = template.requestBodyAndHeader("ignite-compute:abc?executionType=EXECUTE", task,
                    IgniteConstants.IGNITE_COMPUTE_PARAMS, 10, String.class);
            Assert.assertNotNull(result);
            List<String> sortedListResult = Arrays.stream(result.split(",")).sorted().collect(Collectors.toList());
            Assert.assertEquals(Arrays.asList("a0", "a1", "a2", "a3", "a4", "a5", "a6", "a7", "a8", "a9"),
                    sortedListResult);

        } finally {
            for (Ignite inst : igniteInstances) {
                inst.close();
            }
            camelctx.stop();
        }

    }

    @Test
    public void computeRun() throws Exception {
        final CamelContext camelctx = new DefaultCamelContext();

        final IgniteComputeComponent igniteComponent = camelctx.getComponent("ignite-compute",
                IgniteComputeComponent.class);
        igniteComponent.setIgniteConfiguration(new IgniteConfiguration().setClassLoader(getClass().getClassLoader()));

        camelctx.start();

        try {
            final ProducerTemplate template = camelctx.createProducerTemplate();

            /* Single Runnable */
            counter.set(0);
            Object runResult = template.requestBody("ignite-compute:abc?executionType=RUN", new Counter(),
                    Object.class);
            Assert.assertNull(runResult);
            Assert.assertEquals(1, counter.get());

            /* Multiple Runnables */
            counter.set(0);
            Collection<?> runResults = template.requestBody("ignite-compute:abc?executionType=RUN",
                    Arrays.asList(new Counter(), new Counter(), new Counter()), Collection.class);
            Assert.assertNull(runResults);
            Assert.assertEquals(3, counter.get());

        } finally {
            camelctx.stop();
        }

    }

    @Test
    public void events() throws Exception {
        final String cacheName = randomCacheName();

        final CamelContext camelctx = new DefaultCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("ignite-events:" + cacheName).to("mock:test1");
            }
        });

        final IgniteEventsComponent igniteComponent = camelctx.getComponent("ignite-events",
                IgniteEventsComponent.class);
        igniteComponent.setIgniteConfiguration(
                new IgniteConfiguration().setIncludeEventTypes(EventType.EVTS_ALL_MINUS_METRIC_UPDATE));

        final MockEndpoint mockEndpoint = camelctx.getEndpoint("mock:test1", MockEndpoint.class);

        camelctx.start();
        try {

            final IgniteCache<String, String> cache = igniteComponent.getIgnite().getOrCreateCache(cacheName);

            /* Generate some cache activity */
            cache.put("abc", "123");
            cache.get("abc");
            cache.remove("abc");
            cache.withExpiryPolicy(CreatedExpiryPolicy.factoryOf(new Duration(TimeUnit.MILLISECONDS, 100)).create())
                    .put("abc", "123");

            Thread.sleep(150); // wait for the expiration of the above entry

            cache.get("abc");

            final List<Integer> actualTypes = mockEndpoint.getExchanges().stream() //
                    /*
                     * There are some DiscoveryEvents here and there in the list that are hard to predict so let's keep
                     * just the CacheEvents we have generated above
                     */
                    .filter(exchange -> exchange.getIn().getBody(Event.class) instanceof CacheEvent)
                    .map(exchange -> exchange.getIn().getBody(Event.class).type()).collect(Collectors.toList());

            final List<Integer> expectedTypes = Arrays.asList(EventType.EVT_CACHE_STARTED,
                    EventType.EVT_CACHE_ENTRY_CREATED, EventType.EVT_CACHE_OBJECT_PUT, EventType.EVT_CACHE_OBJECT_READ,
                    EventType.EVT_CACHE_OBJECT_REMOVED, EventType.EVT_CACHE_OBJECT_PUT,
                    EventType.EVT_CACHE_OBJECT_EXPIRED);

            Assert.assertEquals(expectedTypes, actualTypes);

        } finally {
            camelctx.stop();
        }

    }

    @Test
    public void idGen() throws Exception {
        final CamelContext camelctx = new DefaultCamelContext();

        final IgniteIdGenComponent igniteComponent = camelctx.getComponent("ignite-idgen", IgniteIdGenComponent.class);
        igniteComponent.setIgniteConfiguration(new IgniteConfiguration());

        camelctx.start();

        try {
            final ProducerTemplate template = camelctx.createProducerTemplate();

            Assert.assertEquals(0, template
                    .requestBody("ignite-idgen:abc?initialValue=0&operation=GET", null, Long.class).longValue());
            Assert.assertEquals(0, template
                    .requestBody("ignite-idgen:abc?initialValue=0&operation=GET_AND_INCREMENT", null, Long.class)
                    .longValue());
            Assert.assertEquals(2, template
                    .requestBody("ignite-idgen:abc?initialValue=0&operation=INCREMENT_AND_GET", null, Long.class)
                    .longValue());
            Assert.assertEquals(7, template
                    .requestBody("ignite-idgen:abc?initialValue=0&operation=ADD_AND_GET", 5, Long.class).longValue());
            Assert.assertEquals(7, template
                    .requestBody("ignite-idgen:abc?initialValue=0&operation=GET_AND_ADD", 5, Long.class).longValue());
            Assert.assertEquals(12,
                    template.requestBody("ignite-idgen:abc?initialValue=0&operation=GET", 5, Long.class).longValue());

        } finally {
            igniteComponent.getIgnite().atomicSequence("abc", 0, false);
            camelctx.stop();
        }
    }

    @Test
    public void messaging() throws Exception {

        final CamelContext camelctx = new DefaultCamelContext();

        final IgniteMessagingComponent igniteComponent = camelctx.getComponent("ignite-messaging",
                IgniteMessagingComponent.class);
        igniteComponent.setIgniteConfiguration(new IgniteConfiguration());

        camelctx.start();
        UUID listenerId = null;
        try {
            final ProducerTemplate template = camelctx.createProducerTemplate();

            TopicListener listener = new TopicListener();
            listenerId = igniteComponent.getIgnite().message().remoteListen("TOPIC1", listener);

            Object sent = 1;
            template.requestBody("ignite-messaging:TOPIC1", sent);

            Object received = listener.messages.poll(5, TimeUnit.SECONDS);
            Assert.assertEquals(sent, received);

        } finally {
            if (listenerId != null) {
                igniteComponent.getIgnite().message().stopRemoteListen(listenerId);
            }
            camelctx.stop();
        }

    }

    /**
     * @return a new {@link IgniteConfiguration} with a random grid name and discovery SPI set so that it is possible to
     *         start multiple instances at once
     */
    private IgniteConfiguration newMultiConfig() {
        return new IgniteConfiguration() //
                .setGridName(UUID.randomUUID().toString())
                .setIncludeEventTypes(EventType.EVT_JOB_FINISHED, EventType.EVT_JOB_RESULTED)
                .setDiscoverySpi(new TcpDiscoverySpi().setIpFinder(new TcpDiscoveryVmIpFinder(false) {
                    {
                        setAddresses(Collections.singleton("127.0.0.1:47500..47509"));
                    }
                })).setClassLoader(getClass().getClassLoader()) //
        ;
    }

    @Test
    public void queue() throws Exception {

        final CamelContext camelctx = new DefaultCamelContext();

        final IgniteQueueComponent igniteComponent = camelctx.getComponent("ignite-queue", IgniteQueueComponent.class);
        igniteComponent.setIgniteConfiguration(new IgniteConfiguration());

        camelctx.start();
        try {
            final ProducerTemplate template = camelctx.createProducerTemplate();

            final Ignite ignite = igniteComponent.getIgnite();

            boolean result = template.requestBody("ignite-queue:abc?operation=ADD", "hello", boolean.class);
            Assert.assertTrue(result);
            IgniteQueue<Object> q = ignite.queue("abc", 0, new CollectionConfiguration());
            Assert.assertTrue("Queue [" + q + "] should contain [hello]", q.contains("hello"));

            result = template.requestBody("ignite-queue:abc?operation=CONTAINS", "hello", boolean.class);
            Assert.assertTrue(result);

            result = template.requestBody("ignite-queue:abc?operation=REMOVE", "hello", boolean.class);
            Assert.assertTrue(result);
            q = ignite.queue("abc", 0, new CollectionConfiguration());
            Assert.assertFalse("Queue [" + q + "] should not contain [hello]", q.contains("hello"));

            result = template.requestBody("ignite-queue:abc?operation=CONTAINS", "hello", boolean.class);
            Assert.assertFalse(result);

        } finally {
            camelctx.stop();
        }

    }

    @Test
    public void set() throws Exception {

        final CamelContext camelctx = new DefaultCamelContext();

        final IgniteSetComponent igniteComponent = camelctx.getComponent("ignite-set", IgniteSetComponent.class);
        igniteComponent.setIgniteConfiguration(new IgniteConfiguration());

        camelctx.start();
        try {
            final ProducerTemplate template = camelctx.createProducerTemplate();

            final Ignite ignite = igniteComponent.getIgnite();

            boolean result = template.requestBody("ignite-set:abc?operation=ADD", "hello", boolean.class);
            Assert.assertTrue(result);
            IgniteSet<Object> q = ignite.set("abc", new CollectionConfiguration());
            Assert.assertTrue("Queue [" + q + "] should contain [hello]", q.contains("hello"));

            result = template.requestBody("ignite-set:abc?operation=CONTAINS", "hello", boolean.class);
            Assert.assertTrue(result);

            result = template.requestBody("ignite-set:abc?operation=REMOVE", "hello", boolean.class);
            Assert.assertTrue(result);
            q = ignite.set("abc", new CollectionConfiguration());
            Assert.assertFalse("Queue [" + q + "] should not contain [hello]", q.contains("hello"));

            result = template.requestBody("ignite-set:abc?operation=CONTAINS", "hello", boolean.class);
            Assert.assertFalse(result);

        } finally {
            camelctx.stop();
        }

    }

}
