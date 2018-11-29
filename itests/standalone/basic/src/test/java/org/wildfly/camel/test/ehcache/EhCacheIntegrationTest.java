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
package org.wildfly.camel.test.ehcache;

import java.net.URL;

import javax.naming.InitialContext;

import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.ehcache.EhcacheConstants;
import org.apache.camel.component.ehcache.processor.idempotent.EhcacheIdempotentRepository;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.support.jndi.JndiBeanRepository;
import org.ehcache.Cache;
import org.ehcache.CacheManager;
import org.ehcache.Status;
import org.ehcache.config.Configuration;
import org.ehcache.config.builders.CacheManagerBuilder;
import org.ehcache.event.EventType;
import org.ehcache.xml.XmlConfiguration;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.extension.camel.CamelAware;

@CamelAware
@RunWith(Arquillian.class)
public class EhCacheIntegrationTest {

    @ArquillianResource
    private InitialContext context;
    private CacheManager cacheManager;

    @Deployment
    public static JavaArchive createDeployment() {
        return ShrinkWrap.create(JavaArchive.class, "camel-ehcache-tests.jar")
            .addAsResource("ehcache/cache-configuration.xml", "cache-configuration.xml");
    }

    @Before
    public void setUp() throws Exception {
        URL url = EhCacheIntegrationTest.class.getResource("/cache-configuration.xml");
        Configuration xmlConfig = new XmlConfiguration(url);

        cacheManager = CacheManagerBuilder.newCacheManager(xmlConfig);
        cacheManager.init();

        context.bind("cacheManager", cacheManager);
    }

    @After
    public void tearDown() throws Exception {
        if (cacheManager != null) {
            if (!cacheManager.getStatus().equals(Status.UNINITIALIZED)) {
                cacheManager.close();
            }
            context.unbind("cacheManager");
        }
    }

    @Test
    public void testEhCacheEventConsumer() throws Exception {
        CamelContext camelctx = new DefaultCamelContext(new JndiBeanRepository());
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("ehcache://wfccache?cacheManager=#cacheManager&eventTypes=CREATED")
                .to("mock:result");
            }
        });

        MockEndpoint mockEndpoint = camelctx.getEndpoint("mock:result", MockEndpoint.class);
        mockEndpoint.expectedHeaderReceived(EhcacheConstants.KEY, "foo");
        mockEndpoint.expectedHeaderReceived(EhcacheConstants.EVENT_TYPE, EventType.CREATED);
        mockEndpoint.expectedMessageCount(1);

        camelctx.start();
        try {
            Cache<Object, Object> cache = cacheManager.getCache("wfccache", Object.class, Object.class);
            cache.put("foo", "bar");

            mockEndpoint.assertIsSatisfied();
        } finally {
            camelctx.stop();
        }
    }

    @Test
    public void testEhCacheEventProducer() throws Exception {
        CamelContext camelctx = new DefaultCamelContext(new JndiBeanRepository());
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:start")
                .to("ehcache://wfccache?cacheManager=#cacheManager");
            }
        });

        camelctx.start();
        try {
            camelctx.createFluentProducerTemplate()
                .withHeader(EhcacheConstants.ACTION, EhcacheConstants.ACTION_PUT)
                .withHeader(EhcacheConstants.KEY, "foo")
                .withBody("bar")
                .to("direct://start")
                .request();

            Cache<Object, Object> cache = cacheManager.getCache("wfccache", Object.class, Object.class);
            Object cacheValue = cache.get("foo");

            Assert.assertNotNull("Cache value for key 'foo' is null", cacheValue);
            Assert.assertEquals("bar", cacheValue);
        } finally {
            camelctx.stop();
        }
    }

    @Test
    public void testEhCacheIdempotentRepository() throws Exception {
        EhcacheIdempotentRepository repository = new EhcacheIdempotentRepository(cacheManager, "idempotent");

        CamelContext camelctx = new DefaultCamelContext(new JndiBeanRepository());
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:start")
                .idempotentConsumer(header("messageId"), repository)
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
