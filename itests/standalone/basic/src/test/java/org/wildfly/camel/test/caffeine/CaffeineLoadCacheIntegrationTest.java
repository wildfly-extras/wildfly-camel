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
package org.wildfly.camel.test.caffeine;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.naming.Context;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.caffeine.CaffeineConstants;
import org.apache.camel.component.mock.MockEndpoint;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.extension.camel.CamelAware;
import org.wildfly.extension.camel.WildFlyCamelContext;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.CacheLoader;
import com.github.benmanes.caffeine.cache.Caffeine;

@CamelAware
@RunWith(Arquillian.class)
public class CaffeineLoadCacheIntegrationTest {

    private WildFlyCamelContext camelctx;
    private Cache<Integer, Integer> cache;

    @Deployment
    public static JavaArchive deployment() {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "caffeine-loadcache-tests");
        return archive;
    }

    @Before
    public void before() throws Exception {
        CacheLoader<Integer, Integer> cl = new CacheLoader<Integer, Integer>() {
            public Integer load(Integer key) throws Exception {
                return key + 1;
            }
        };
        cache = Caffeine.newBuilder().build(cl);

        camelctx = new WildFlyCamelContext();
        Context jndi = camelctx.getNamingContext();
        jndi.rebind("cache", cache);
    }

    @Test
    public void testCacheClear() throws Exception {

        camelctx.addRoutes(createRouteBuilder());

        camelctx.start();
        try {
            MockEndpoint mock = camelctx.getEndpoint("mock:result", MockEndpoint.class);
            mock.expectedMinimumMessageCount(1);
            mock.expectedBodiesReceived((Object)null);
            mock.expectedHeaderReceived(CaffeineConstants.ACTION_HAS_RESULT, false);
            mock.expectedHeaderReceived(CaffeineConstants.ACTION_SUCCEEDED, true);

            camelctx.createFluentProducerTemplate().to("direct://start")
            .withHeader(CaffeineConstants.ACTION, CaffeineConstants.ACTION_CLEANUP)
            .send();

            mock.assertIsSatisfied();
        } finally {
            camelctx.stop();
        }
    }

    @Test
    public void testCachePut() throws Exception {
        final Integer key = 1;
        final Integer val = 3;

        camelctx.addRoutes(createRouteBuilder());

        camelctx.start();
        try {
            MockEndpoint mock = camelctx.getEndpoint("mock:result", MockEndpoint.class);
            mock.expectedMinimumMessageCount(1);
            mock.expectedBodiesReceived(val);
            mock.expectedHeaderReceived(CaffeineConstants.ACTION_HAS_RESULT, false);
            mock.expectedHeaderReceived(CaffeineConstants.ACTION_SUCCEEDED, true);

            camelctx.createFluentProducerTemplate().to("direct://start")
            .withHeader(CaffeineConstants.ACTION, CaffeineConstants.ACTION_PUT)
            .withHeader(CaffeineConstants.KEY, key)
            .withBody(val)
            .send();

            Assert.assertTrue(cache.getIfPresent(key) != null);
            Assert.assertEquals(val, cache.getIfPresent(key));
        } finally {
            camelctx.stop();
        }
    }

    @Test
    public void testCachePutAll() throws Exception {
        final Map<Integer, Integer> map = new HashMap<>();
        map.put(1, 1);
        map.put(2, 2);
        map.put(3, 3);
        final Set<Integer> keys = map.keySet().stream().limit(2).collect(Collectors.toSet());

        camelctx.addRoutes(createRouteBuilder());

        camelctx.start();
        try {
            MockEndpoint mock = camelctx.getEndpoint("mock:result", MockEndpoint.class);
            mock.expectedMinimumMessageCount(1);
            mock.expectedHeaderReceived(CaffeineConstants.ACTION_HAS_RESULT, false);
            mock.expectedHeaderReceived(CaffeineConstants.ACTION_SUCCEEDED, true);

            camelctx.createFluentProducerTemplate().to("direct://start")
            .withHeader(CaffeineConstants.ACTION, CaffeineConstants.ACTION_PUT_ALL)
            .withBody(map)
            .send();

            final Map<Integer, Integer> elements = cache.getAllPresent(keys);
            keys.forEach(k -> {
                Assert.assertTrue(elements.containsKey(k));
                Assert.assertEquals(map.get(k), elements.get(k));
            });

            mock.assertIsSatisfied();
        } finally {
            camelctx.stop();
        }
    }

    @Test
    public void testCacheGet() throws Exception {
        final Integer key = 1;
        final Integer val = 2;

        camelctx.addRoutes(createRouteBuilder());

        camelctx.start();
        try {
            MockEndpoint mock = camelctx.getEndpoint("mock:result", MockEndpoint.class);
            mock.expectedMinimumMessageCount(1);
            mock.expectedBodiesReceived(val);
            mock.expectedHeaderReceived(CaffeineConstants.ACTION_HAS_RESULT, true);
            mock.expectedHeaderReceived(CaffeineConstants.ACTION_SUCCEEDED, true);

            camelctx.createFluentProducerTemplate().to("direct://start")
            .withHeader(CaffeineConstants.ACTION, CaffeineConstants.ACTION_GET)
            .withHeader(CaffeineConstants.KEY, key)
            .withBody(val)
            .send();

            mock.assertIsSatisfied();
        } finally {
            camelctx.stop();
        }
    }

    @Test
    public void testCacheGetAll() throws Exception {
        final Map<Integer, Integer> map = new HashMap<>();
        map.put(1, 1);
        map.put(2, 2);
        map.put(3, 3);
        final Set<Integer> keys = map.keySet().stream().limit(2).collect(Collectors.toSet());

        cache.putAll(map);

        camelctx.addRoutes(createRouteBuilder());

        camelctx.start();
        try {
            MockEndpoint mock = camelctx.getEndpoint("mock:result", MockEndpoint.class);
            mock.expectedMinimumMessageCount(1);
            mock.expectedHeaderReceived(CaffeineConstants.ACTION_HAS_RESULT, true);
            mock.expectedHeaderReceived(CaffeineConstants.ACTION_SUCCEEDED, true);

            camelctx.createFluentProducerTemplate().to("direct://start")
            .withHeader(CaffeineConstants.ACTION, CaffeineConstants.ACTION_GET_ALL)
            .withHeader(CaffeineConstants.KEYS, keys)
            .send();

            mock.assertIsSatisfied();

            final Map<Integer, Integer> elements = mock.getExchanges().get(0).getIn().getBody(Map.class);
            keys.forEach(k -> {
                Assert.assertTrue(elements.containsKey(k));
                Assert.assertEquals(map.get(k), elements.get(k));
            });
        } finally {
            camelctx.stop();
        }
    }

    @Test
    public void testCacheInvalidate() throws Exception {
        final Integer key = 1;
        final Integer val = 1;

        cache.put(key, val);

        camelctx.addRoutes(createRouteBuilder());

        camelctx.start();
        try {
            MockEndpoint mock = camelctx.getEndpoint("mock:result", MockEndpoint.class);
            mock.expectedMinimumMessageCount(1);
            mock.expectedHeaderReceived(CaffeineConstants.ACTION_HAS_RESULT, false);
            mock.expectedHeaderReceived(CaffeineConstants.ACTION_SUCCEEDED, true);

            camelctx.createFluentProducerTemplate().to("direct://start")
            .withHeader(CaffeineConstants.ACTION, CaffeineConstants.ACTION_INVALIDATE)
            .withHeader(CaffeineConstants.KEY, key)
            .send();

            mock.assertIsSatisfied();

            Assert.assertFalse(cache.getIfPresent(key) != null);
        } finally {
            camelctx.stop();
        }
    }

    @Test
    public void testCacheInvalidateAll() throws Exception {
        final Map<Integer, Integer> map = new HashMap<>();
        map.put(1, 1);
        map.put(2, 2);
        map.put(3, 3);
        final Set<Integer> keys = map.keySet().stream().limit(2).collect(Collectors.toSet());

        cache.putAll(map);

        camelctx.addRoutes(createRouteBuilder());

        camelctx.start();
        try {
            MockEndpoint mock = camelctx.getEndpoint("mock:result", MockEndpoint.class);
            mock.expectedMinimumMessageCount(1);
            mock.expectedHeaderReceived(CaffeineConstants.ACTION_HAS_RESULT, false);
            mock.expectedHeaderReceived(CaffeineConstants.ACTION_SUCCEEDED, true);

            camelctx.createFluentProducerTemplate().to("direct://start")
            .withHeader(CaffeineConstants.ACTION, CaffeineConstants.ACTION_INVALIDATE_ALL)
            .withHeader(CaffeineConstants.KEYS, keys)
            .send();

            mock.assertIsSatisfied();

            final Map<Integer, Integer> elements = cache.getAllPresent(keys);
            keys.forEach(k -> {
                Assert.assertFalse(elements.containsKey(k));
            });
        } finally {
            camelctx.stop();
        }
    }

    private RouteBuilder createRouteBuilder() throws Exception {
        return new RouteBuilder() {
            public void configure() {
                from("direct://start")
                .toF("caffeine-loadcache://%s?cache=#cache", "test")
                .to("log:org.apache.camel.component.caffeine?level=INFO&showAll=true&multiline=true")
                .to("mock:result");
            }
        };
    }
}
