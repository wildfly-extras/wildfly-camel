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

import java.util.Map;

import org.apache.camel.CamelContext;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.hazelcast.HazelcastConstants;
import org.apache.camel.component.hazelcast.listener.MapEntryListener;
import org.apache.camel.component.hazelcast.map.HazelcastMapComponent;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.impl.DefaultCamelContext;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.objenesis.Objenesis;
import org.wildfly.extension.camel.CamelAware;

import com.hazelcast.core.EntryEvent;
import com.hazelcast.core.EntryEventType;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;

@CamelAware
@RunWith(Arquillian.class)
public class HazelcastMapConsumerIntegrationTest {

    @Mock
    private IMap<Object, Object> map;

    @Mock
    private HazelcastInstance hazelcastInstance;

    private ArgumentCaptor<MapEntryListener> argument;

    @Deployment
    public static JavaArchive deployment() {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "hazelcast-map-consumer-tests");
        archive.addPackages(true, Mockito.class.getPackage(), Objenesis.class.getPackage());
        return archive;
    }

    @After
    public final void verifyHazelcastInstanceMock() {
        verifyHazelcastInstance(hazelcastInstance);
        Mockito.verifyNoMoreInteractions(hazelcastInstance);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testAdd() throws Exception {
        CamelContext camelctx = createCamelContext();
        camelctx.start();
        try {
            MockEndpoint mock = camelctx.getEndpoint("mock:added", MockEndpoint.class);
            mock.expectedMessageCount(1);

            EntryEvent<Object, Object> event = new EntryEvent<Object, Object>("foo", null, EntryEventType.ADDED.getType(), "4711", "my-foo");
            argument.getValue().entryAdded(event);
            mock.assertIsSatisfied(3000);

            checkHeaders(mock.getExchanges().get(0).getIn().getHeaders(), HazelcastConstants.ADDED);
        } finally {
            camelctx.stop();
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testEvict() throws Exception {
        CamelContext camelctx = createCamelContext();
        camelctx.start();
        try {
            MockEndpoint mock = camelctx.getEndpoint("mock:evicted", MockEndpoint.class);
            mock.expectedMessageCount(1);

            EntryEvent<Object, Object> event = new EntryEvent<Object, Object>("foo", null, EntryEventType.EVICTED.getType(), "4711", "my-foo");
            argument.getValue().entryEvicted(event);

            mock.assertIsSatisfied(3000);
        } finally {
            camelctx.stop();
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testUpdate() throws Exception {
        CamelContext camelctx = createCamelContext();
        camelctx.start();
        try {
            MockEndpoint mock = camelctx.getEndpoint("mock:updated", MockEndpoint.class);
            mock.expectedMessageCount(1);

            EntryEvent<Object, Object> event = new EntryEvent<Object, Object>("foo", null, EntryEventType.UPDATED.getType(), "4711", "my-foo");
            argument.getValue().entryUpdated(event);

            mock.assertIsSatisfied(3000);

            checkHeaders(mock.getExchanges().get(0).getIn().getHeaders(), HazelcastConstants.UPDATED);
        } finally {
            camelctx.stop();
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testRemove() throws Exception {
        CamelContext camelctx = createCamelContext();
        camelctx.start();
        try {
            MockEndpoint mock = camelctx.getEndpoint("mock:removed", MockEndpoint.class);
            mock.expectedMessageCount(1);

            EntryEvent<Object, Object> event = new EntryEvent<Object, Object>("foo", null, EntryEventType.REMOVED.getType(), "4711", "my-foo");
            argument.getValue().entryRemoved(event);

            mock.assertIsSatisfied(3000);
            checkHeaders(mock.getExchanges().get(0).getIn().getHeaders(), HazelcastConstants.REMOVED);
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
                from(String.format("hazelcast-%sfoo", HazelcastConstants.MAP_PREFIX))
                .log("object...")
                .choice()
                .when(header(HazelcastConstants.LISTENER_ACTION)
                        .isEqualTo(HazelcastConstants.ADDED))
                        .log("...added").to("mock:added")
                .when(header(HazelcastConstants.LISTENER_ACTION)
                        .isEqualTo(HazelcastConstants.EVICTED))
                        .log("...evicted").to("mock:evicted")
                .when(header(HazelcastConstants.LISTENER_ACTION)
                        .isEqualTo(HazelcastConstants.UPDATED))
                        .log("...updated").to("mock:updated")
                .when(header(HazelcastConstants.LISTENER_ACTION)
                        .isEqualTo(HazelcastConstants.REMOVED))
                        .log("...removed").to("mock:removed")
                .otherwise().log("fail!");
            }
        };
    }

    private void checkHeaders(Map<String, Object> headers, String action) {
        Assert.assertEquals(action, headers.get(HazelcastConstants.LISTENER_ACTION));
        Assert.assertEquals(HazelcastConstants.CACHE_LISTENER, headers.get(HazelcastConstants.LISTENER_TYPE));
        Assert.assertEquals("4711", headers.get(HazelcastConstants.OBJECT_ID));
        Assert.assertNotNull(headers.get(HazelcastConstants.LISTENER_TIME));
    }

    private void trainHazelcastInstance(HazelcastInstance hazelcastInstance) {
        Mockito.when(hazelcastInstance.getMap("foo")).thenReturn(map);
        argument = ArgumentCaptor.forClass(MapEntryListener.class);
        Mockito.when(map.addEntryListener(argument.capture(), Mockito.eq(true))).thenReturn("foo");
    }

    private void verifyHazelcastInstance(HazelcastInstance hazelcastInstance) {
        Mockito.verify(hazelcastInstance).getMap("foo");
        Mockito.verify(map).addEntryListener(Mockito.any(MapEntryListener.class), Mockito.eq(true));
    }
}
