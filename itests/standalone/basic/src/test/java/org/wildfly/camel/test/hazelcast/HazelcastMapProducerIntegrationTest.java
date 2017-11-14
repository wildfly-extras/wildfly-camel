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
package org.wildfly.camel.test.hazelcast;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.apache.camel.CamelContext;
import org.apache.camel.CamelExecutionException;
import org.apache.camel.ConsumerTemplate;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.hazelcast.HazelcastConstants;
import org.apache.camel.component.hazelcast.HazelcastOperation;
import org.apache.camel.component.hazelcast.map.HazelcastMapComponent;
import org.apache.camel.impl.DefaultCamelContext;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.objenesis.Objenesis;
import org.wildfly.extension.camel.CamelAware;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.hazelcast.query.SqlPredicate;

import net.bytebuddy.ByteBuddy;

@CamelAware
@RunWith(Arquillian.class)
public class HazelcastMapProducerIntegrationTest {

    @Mock
    private IMap<Object, Object> map;

    @Mock
    private HazelcastInstance hazelcastInstance;

    @Deployment
    public static JavaArchive deployment() {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "hazelcast-map-producer-tests");
        archive.addPackages(true, Mockito.class.getPackage(), Objenesis.class.getPackage(), ByteBuddy.class.getPackage());
        return archive;
    }

    @After
    public final void verifyHazelcastInstanceMock() {
        verifyHazelcastInstance(hazelcastInstance);
        Mockito.verifyNoMoreInteractions(hazelcastInstance);
        Mockito.verifyNoMoreInteractions(map);
    }

    @Test(expected = CamelExecutionException.class)
    public void testWithInvalidOperation() throws Exception {
        CamelContext camelctx = createCamelContext();
        camelctx.start();
        try {
            ProducerTemplate template = camelctx.createProducerTemplate();
            template.sendBody("direct:putInvalid", "my-foo");
        } finally {
            camelctx.stop();
        }
    }

    @Test
    public void testPut() throws Exception {
        CamelContext camelctx = createCamelContext();
        camelctx.start();
        try {
            ProducerTemplate template = camelctx.createProducerTemplate();
            template.sendBodyAndHeader("direct:put", "my-foo", HazelcastConstants.OBJECT_ID, "4711");
            Mockito.verify(map).put("4711", "my-foo");
        } finally {
            camelctx.stop();
        }
    }

    @Test
    public void testPutWithOperationNumber() throws Exception {
        CamelContext camelctx = createCamelContext();
        camelctx.start();
        try {
            ProducerTemplate template = camelctx.createProducerTemplate();
            template.sendBodyAndHeader("direct:putWithOperationNumber", "my-foo", HazelcastConstants.OBJECT_ID, "4711");
            Mockito.verify(map).put("4711", "my-foo");
        } finally {
            camelctx.stop();
        }
    }

    @Test
    public void testPutWithOperationName() throws Exception {
        CamelContext camelctx = createCamelContext();
        camelctx.start();
        try {
            ProducerTemplate template = camelctx.createProducerTemplate();
            template.sendBodyAndHeader("direct:putWithOperationName", "my-foo", HazelcastConstants.OBJECT_ID, "4711");
            Mockito.verify(map).put("4711", "my-foo");
        } finally {
            camelctx.stop();
        }
    }

    @Test
    public void testPutWithTTL() throws Exception {
        CamelContext camelctx = createCamelContext();
        camelctx.start();
        try {
            Map<String, Object> headers = new HashMap<String, Object>();
            headers.put(HazelcastConstants.OBJECT_ID, "4711");
            headers.put(HazelcastConstants.TTL_VALUE, new Long(1));
            headers.put(HazelcastConstants.TTL_UNIT, TimeUnit.MINUTES);

            ProducerTemplate template = camelctx.createProducerTemplate();
            template.sendBodyAndHeaders("direct:put", "test", headers);
            Mockito.verify(map).put("4711", "test", 1, TimeUnit.MINUTES);
        } finally {
            camelctx.stop();
        }
    }
    
    @Test
    public void testUpdate() throws Exception {
        CamelContext camelctx = createCamelContext();
        camelctx.start();
        try {
            ProducerTemplate template = camelctx.createProducerTemplate();
            template.sendBodyAndHeader("direct:update", "my-fooo", HazelcastConstants.OBJECT_ID, "4711");
            Mockito.verify(map).lock("4711");
            Mockito.verify(map).replace("4711", "my-fooo");
            Mockito.verify(map).unlock("4711");
        } finally {
            camelctx.stop();
        }
    }

    @Test
    public void testGet() throws Exception {
        CamelContext camelctx = createCamelContext();
        camelctx.start();
        try {
            ProducerTemplate template = camelctx.createProducerTemplate();
            Mockito.when(map.get("4711")).thenReturn("my-foo");
            template.sendBodyAndHeader("direct:get", null, HazelcastConstants.OBJECT_ID, "4711");

            ConsumerTemplate consumer = camelctx.createConsumerTemplate();
            String body = consumer.receiveBody("seda:out", 5000, String.class);
            Mockito.verify(map).get("4711");
            Assert.assertEquals("my-foo", body);
        } finally {
            camelctx.stop();
        }
    }
    
    @Test
    public void testGetAllEmptySet() throws Exception {
        CamelContext camelctx = createCamelContext();
        camelctx.start();
        try {
            Set<Object> l = new HashSet<Object>();
            Map t = new HashMap();
            t.put("key1", "value1");
            t.put("key2", "value2");
            t.put("key3", "value3");
            Mockito.when(map.getAll(Mockito.anySet())).thenReturn(t);

            ProducerTemplate template = camelctx.createProducerTemplate();
            template.sendBodyAndHeader("direct:getAll", null, HazelcastConstants.OBJECT_ID, l);

            ConsumerTemplate consumer = camelctx.createConsumerTemplate();
            String body = consumer.receiveBody("seda:out", 5000, String.class);
            Mockito.verify(map).getAll(l);
            Assert.assertTrue(body.contains("key1=value1"));
            Assert.assertTrue(body.contains("key2=value2"));
            Assert.assertTrue(body.contains("key3=value3"));
        } finally {
            camelctx.stop();
        }
    }
    
    @Test
    public void testGetAllOnlyOneKey() throws Exception {
        CamelContext camelctx = createCamelContext();
        camelctx.start();
        try {
            Set<Object> l = new HashSet<Object>();
            l.add("key1");
            Map t = new HashMap();
            t.put("key1", "value1");
            Mockito.when(map.getAll(l)).thenReturn(t);

            ProducerTemplate template = camelctx.createProducerTemplate();
            template.sendBodyAndHeader("direct:getAll", null, HazelcastConstants.OBJECT_ID, l);

            ConsumerTemplate consumer = camelctx.createConsumerTemplate();
            String body = consumer.receiveBody("seda:out", 5000, String.class);
            Mockito.verify(map).getAll(l);
            Assert.assertEquals("{key1=value1}", body);
        } finally {
            camelctx.stop();
        }
    }

    @Test
    public void testDelete() throws Exception {
        CamelContext camelctx = createCamelContext();
        camelctx.start();
        try {
            ProducerTemplate template = camelctx.createProducerTemplate();
            template.sendBodyAndHeader("direct:delete", null, HazelcastConstants.OBJECT_ID, 4711);
            Mockito.verify(map).remove(4711);
        } finally {
            camelctx.stop();
        }
    }

    @Test
    public void testQuery() throws Exception {
        CamelContext camelctx = createCamelContext();
        camelctx.start();
        try {
            String sql = "bar > 1000";
            Mockito.when(map.values(Mockito.any(SqlPredicate.class))).thenReturn(Arrays.<Object>asList(new Dummy("beta", 2000), new Dummy("gamma", 3000)));

            ProducerTemplate template = camelctx.createProducerTemplate();
            template.sendBodyAndHeader("direct:queue", null, HazelcastConstants.QUERY, sql);
            Mockito.verify(map).values(Mockito.any(SqlPredicate.class));

            ConsumerTemplate consumer = camelctx.createConsumerTemplate();
            Collection<?> b1 = consumer.receiveBody("seda:out", 5000, Collection.class);

            Assert.assertNotNull(b1);
            Assert.assertEquals(2, b1.size());
        } finally {
            camelctx.stop();
        }
    }
    
    @Test
    public void testEmptyQuery() throws Exception {
        CamelContext camelctx = createCamelContext();
        camelctx.start();
        try {
            Mockito.when(map.values()).thenReturn(Arrays.<Object>asList(new Dummy("beta", 2000), new Dummy("gamma", 3000), new Dummy("delta", 4000)));

            ProducerTemplate template = camelctx.createProducerTemplate();
            template.sendBody("direct:queue", null);
            Mockito. verify(map).values();

            ConsumerTemplate consumer = camelctx.createConsumerTemplate();
            Collection<?> b1 = consumer.receiveBody("seda:out", 5000, Collection.class);

            Assert.assertNotNull(b1);
            Assert.assertEquals(3, b1.size());
        } finally {
            camelctx.stop();
        }
    }
    
    @Test
    public void testUpdateOldValue() throws Exception {
        CamelContext camelctx = createCamelContext();
        camelctx.start();
        try {
            Map<String, Object> headers = new HashMap<String, Object>();
            headers.put(HazelcastConstants.OBJECT_ID, "4711");
            headers.put(HazelcastConstants.OBJECT_VALUE, "my-foo");

            ProducerTemplate template = camelctx.createProducerTemplate();
            template.sendBodyAndHeaders("direct:update", "replaced", headers);
            Mockito.verify(map).lock("4711");
            Mockito.verify(map).replace("4711", "my-foo", "replaced");
            Mockito.verify(map).unlock("4711");
        } finally {
            camelctx.stop();
        }
    }
    
    @Test
    public void testPutIfAbsent() throws Exception {
        CamelContext camelctx = createCamelContext();
        camelctx.start();
        try {
            Map<String, Object> headers = new HashMap<String, Object>();
            headers.put(HazelcastConstants.OBJECT_ID, "4711");

            ProducerTemplate template = camelctx.createProducerTemplate();
            template.sendBodyAndHeaders("direct:putIfAbsent", "replaced", headers);
            Mockito.verify(map).putIfAbsent("4711", "replaced");
        } finally {
            camelctx.stop();
        }
    }
    
    @Test
    public void testPutIfAbsentWithTtl() throws Exception {
        CamelContext camelctx = createCamelContext();
        camelctx.start();
        try {
            Map<String, Object> headers = new HashMap<String, Object>();
            headers.put(HazelcastConstants.OBJECT_ID, "4711");
            headers.put(HazelcastConstants.TTL_VALUE, new Long(1));
            headers.put(HazelcastConstants.TTL_UNIT, TimeUnit.MINUTES);

            ProducerTemplate template = camelctx.createProducerTemplate();
            template.sendBodyAndHeaders("direct:putIfAbsent", "replaced", headers);
            Mockito.verify(map).putIfAbsent("4711", "replaced", new Long(1), TimeUnit.MINUTES);
        } finally {
            camelctx.stop();
        }
    }
    
    @Test
    public void testEvict() throws Exception {
        CamelContext camelctx = createCamelContext();
        camelctx.start();
        try {
            Map<String, Object> headers = new HashMap<String, Object>();
            headers.put(HazelcastConstants.OBJECT_ID, "4711");
            
            ProducerTemplate template = camelctx.createProducerTemplate();
            template.sendBodyAndHeaders("direct:evict", "", headers);
            Mockito.verify(map).evict("4711");
        } finally {
            camelctx.stop();
        }
    }
    
    @Test
    public void testEvictAll() throws Exception {
        CamelContext camelctx = createCamelContext();
        camelctx.start();
        try {
            Map<String, Object> headers = new HashMap<String, Object>();

            ProducerTemplate template = camelctx.createProducerTemplate();
            template.sendBodyAndHeaders("direct:evictAll", "", headers);
            Mockito.verify(map).evictAll();
        } finally {
            camelctx.stop();
        }
    }
    
    @Test
    public void testClear() throws Exception {
        CamelContext camelctx = createCamelContext();
        camelctx.start();
        try {
            ProducerTemplate template = camelctx.createProducerTemplate();
            template.sendBody("direct:clear", "test");
            Mockito.verify(map).clear();
        } finally {
            camelctx.stop();
        }
    }
    
    @Test
    public void testContainsKey() throws Exception {
        CamelContext camelctx = createCamelContext();
        camelctx.start();
        try {
            Mockito.when(map.containsKey("testOk")).thenReturn(true);
            Mockito.when(map.containsKey("testKo")).thenReturn(false);

            ProducerTemplate template = camelctx.createProducerTemplate();
            template.sendBodyAndHeader("direct:containsKey", null, HazelcastConstants.OBJECT_ID, "testOk");

            ConsumerTemplate consumer = camelctx.createConsumerTemplate();
            Boolean body = consumer.receiveBody("seda:out", 5000, Boolean.class);
            Mockito.verify(map).containsKey("testOk");
            Assert.assertEquals(true, body);
            template.sendBodyAndHeader("direct:containsKey", null, HazelcastConstants.OBJECT_ID, "testKo");
            body = consumer.receiveBody("seda:out", 5000, Boolean.class);
            Mockito.verify(map).containsKey("testKo");
            Assert.assertEquals(false, body);
        } finally {
            camelctx.stop();
        }
    }
    
    @Test
    public void testContainsValue() throws Exception {
        CamelContext camelctx = createCamelContext();
        camelctx.start();
        try {
            Mockito.when(map.containsValue("testOk")).thenReturn(true);
            Mockito.when(map.containsValue("testKo")).thenReturn(false);

            ProducerTemplate template = camelctx.createProducerTemplate();
            template.sendBody("direct:containsValue", "testOk");

            ConsumerTemplate consumer = camelctx.createConsumerTemplate();
            Boolean body = consumer.receiveBody("seda:out", 5000, Boolean.class);
            Mockito.verify(map).containsValue("testOk");
            Assert.assertEquals(true, body);
            template.sendBody("direct:containsValue", "testKo");
            body = consumer.receiveBody("seda:out", 5000, Boolean.class);
            Mockito.verify(map).containsValue("testKo");
            Assert.assertEquals(false, body);
        } finally {
            camelctx.stop();
        }
    }

    private CamelContext createCamelContext() throws Exception {
        MockitoAnnotations.initMocks(this);
        CamelContext context = new DefaultCamelContext();
        context.addRoutes(createRouteBuilder());
        HazelcastMapComponent map = new HazelcastMapComponent(context);
        map.setHazelcastInstance(hazelcastInstance);
        context.addComponent("hazelcast-map", map);
        trainHazelcastInstance(hazelcastInstance);
        return context;
    }

    private RouteBuilder createRouteBuilder() throws Exception {
        return new RouteBuilder() {
            @Override
            public void configure() throws Exception {

                from("direct:putInvalid")
                .setHeader(HazelcastConstants.OPERATION, constant("bogus"))
                .to(String.format("hazelcast-%sfoo", HazelcastConstants.MAP_PREFIX));

                from("direct:put")
                .setHeader(HazelcastConstants.OPERATION, constant(HazelcastOperation.PUT))
                .to(String.format("hazelcast-%sfoo", HazelcastConstants.MAP_PREFIX));

                from("direct:putIfAbsent")
                .setHeader(HazelcastConstants.OPERATION, constant(HazelcastOperation.PUT_IF_ABSENT))
                .to(String.format("hazelcast-%sfoo", HazelcastConstants.MAP_PREFIX));

                from("direct:update")
                .setHeader(HazelcastConstants.OPERATION, constant(HazelcastOperation.UPDATE))
                .to(String.format("hazelcast-%sfoo", HazelcastConstants.MAP_PREFIX));

                from("direct:get")
                .setHeader(HazelcastConstants.OPERATION, constant(HazelcastOperation.GET))
                .to(String.format("hazelcast-%sfoo", HazelcastConstants.MAP_PREFIX))
                .to("seda:out");

                from("direct:getAll")
                .setHeader(HazelcastConstants.OPERATION, constant(HazelcastOperation.GET_ALL))
                .to(String.format("hazelcast-%sfoo", HazelcastConstants.MAP_PREFIX))
                .to("seda:out");

                from("direct:delete")
                .setHeader(HazelcastConstants.OPERATION, constant(HazelcastOperation.DELETE))
                .to(String.format("hazelcast-%sfoo", HazelcastConstants.MAP_PREFIX));

                from("direct:queue")
                .setHeader(HazelcastConstants.OPERATION, constant(HazelcastOperation.QUERY))
                .to(String.format("hazelcast-%sfoo", HazelcastConstants.MAP_PREFIX))
                .to("seda:out");

                from("direct:clear")
                .setHeader(HazelcastConstants.OPERATION, constant(HazelcastOperation.CLEAR))
                .to(String.format("hazelcast-%sfoo", HazelcastConstants.MAP_PREFIX));

                from("direct:evict")
                .setHeader(HazelcastConstants.OPERATION, constant(HazelcastOperation.EVICT))
                .to(String.format("hazelcast-%sfoo", HazelcastConstants.MAP_PREFIX))
                .to("seda:out");

                from("direct:evictAll")
                .setHeader(HazelcastConstants.OPERATION, constant(HazelcastOperation.EVICT_ALL))
                .to(String.format("hazelcast-%sfoo", HazelcastConstants.MAP_PREFIX))
                .to("seda:out");

                from("direct:containsKey")
                .setHeader(HazelcastConstants.OPERATION, constant(HazelcastOperation.CONTAINS_KEY))
                .to(String.format("hazelcast-%sfoo", HazelcastConstants.MAP_PREFIX))
                .to("seda:out");

                from("direct:containsValue")
                .setHeader(HazelcastConstants.OPERATION, constant(HazelcastOperation.CONTAINS_VALUE))
                .to(String.format("hazelcast-%sfoo", HazelcastConstants.MAP_PREFIX))
                .to("seda:out");

                from("direct:putWithOperationNumber")
                .toF("hazelcast-%sfoo?operation=%s", HazelcastConstants.MAP_PREFIX, HazelcastOperation.PUT);
                
                from("direct:putWithOperationName")
                .toF("hazelcast-%sfoo?operation=PUT", HazelcastConstants.MAP_PREFIX);
            }
        };
    }

    private void trainHazelcastInstance(HazelcastInstance hazelcastInstance) {
        Mockito.when(hazelcastInstance.getMap("foo")).thenReturn(map);
    }

    private void verifyHazelcastInstance(HazelcastInstance hazelcastInstance) {
        Mockito.verify(hazelcastInstance, Mockito.atLeastOnce()).getMap("foo");
    }

    static class Dummy implements Serializable {

        private static final long serialVersionUID = 1L;

        private String foo;
        private int bar;

        public Dummy(String foo, int bar) {
            this.foo = foo;
            this.bar = bar;
        }

        public String getFoo() {
            return foo;
        }

        public void setFoo(String foo) {
            this.foo = foo;
        }

        public int getBar() {
            return bar;
        }

        public void setBar(int bar) {
            this.bar = bar;
        }
    }
}
