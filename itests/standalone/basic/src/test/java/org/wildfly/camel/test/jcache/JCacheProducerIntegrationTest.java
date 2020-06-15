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
package org.wildfly.camel.test.jcache;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.Predicate;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.jcache.JCacheConstants;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.impl.DefaultCamelContext;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.extension.camel.CamelAware;

@CamelAware
@RunWith(Arquillian.class)
public class JCacheProducerIntegrationTest {

    private UUID randomUUID = UUID.randomUUID();

    @Deployment
    public static JavaArchive createdeployment() {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "camel-jcache-tests");
        return archive;
    }

    @Test
    @Ignore("[CAMEL-15167] Clarify use of sysprops for HazelcastCachingProvider")
    public void testPutGetAndRemove() throws Exception {

        try (CamelContext camelctx = new DefaultCamelContext()) {
        	
            camelctx.addRoutes(new RouteBuilder() {
                public void configure() {
                    from("direct:put")
                    .to("jcache://test-cache")
                        .to("mock:put");
                    from("direct:get")
                        .to("jcache://test-cache")
                            .to("mock:get");
                }
            });

            final Map<String, Object> headers = new HashMap<>();

            final String key = randomUUID.toString();
            final String val = randomUUID.toString();

            camelctx.start();

            ProducerTemplate producer = camelctx.createProducerTemplate();

            headers.clear();
            headers.put(JCacheConstants.ACTION, "PUT");
            headers.put(JCacheConstants.KEY, key);
            producer.sendBodyAndHeaders("direct:put", val, headers);

            MockEndpoint mock = camelctx.getEndpoint("mock:put", MockEndpoint.class);
            mock.expectedMinimumMessageCount(1);
            mock.expectedHeaderReceived(JCacheConstants.KEY, key);
            mock.expectedMessagesMatches(new Predicate() {
                @Override
                public boolean matches(Exchange exchange) {
                    Assert.assertNotNull("body", exchange.getIn().getBody());
                    return exchange.getIn().getBody().equals(val);
                }
            });
            mock.assertIsSatisfied();

            headers.clear();
            headers.put(JCacheConstants.ACTION, "GETANDREMOVE");
            headers.put(JCacheConstants.KEY, key);
            producer.sendBodyAndHeaders("direct:get", null, headers);

            mock = camelctx.getEndpoint("mock:put", MockEndpoint.class);
            mock.expectedMinimumMessageCount(1);
            mock.expectedHeaderReceived(JCacheConstants.KEY, key);
            mock.expectedMessagesMatches(new Predicate() {
                @Override
                public boolean matches(Exchange exchange) {
                    Assert.assertNotNull("body", exchange.getIn().getBody());
                    return exchange.getIn().getBody().equals(val);
                }
            });
            mock.assertIsSatisfied();
        }
    }

    @Test
    @Ignore("[CAMEL-15167] Clarify use of sysprops for HazelcastCachingProvider")
    public void testJCacheLoadsCachingProviderHazelcast() throws Exception {
    	
        try (CamelContext camelctx = new DefaultCamelContext()) {
        	
            camelctx.addRoutes(new RouteBuilder() {
                public void configure() {
                    from("jcache://test-cacheA?cachingProvider=com.hazelcast.cache.HazelcastCachingProvider")
                    .to("mock:resultA");
                }
            });
            
            // Just ensure we can start up without any class loading issues
            camelctx.start();
        }
    }

    @Test
    public void testJCacheLoadsCachingProviderEhcache() throws Exception {
    	
        try (CamelContext camelctx = new DefaultCamelContext()) {
        	
            camelctx.addRoutes(new RouteBuilder() {
                public void configure() {
                    from("jcache://test-cacheB?cachingProvider=org.ehcache.jsr107.EhcacheCachingProvider")
                    .to("mock:resultB");
                }
            });
            
            // Just ensure we can start up without any class loading issues
            camelctx.start();
        }
    }

    @Test
    public void testJCacheLoadsCachingProviderJCaching() throws Exception {
    	
        try (CamelContext camelctx = new DefaultCamelContext()) {
        	
            camelctx.addRoutes(new RouteBuilder() {
                public void configure() {
                    from("jcache://test-cacheC?cachingProvider=org.infinispan.jcache.embedded.JCachingProvider")
                    .to("mock:resultC");
                }
            });
            
            // Just ensure we can start up without any class loading issues
            camelctx.start();
        }
    }
}
