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
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.extension.camel.CamelAware;

@CamelAware
@RunWith(Arquillian.class)
public class JCacheProducerIntegrationTest {

    @Deployment
    public static JavaArchive createdeployment() {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "camel-jcache-tests");
        return archive;
    }
    
    @Test
    public void testPutGetAndRemove() throws Exception {

        CamelContext camelctx = new DefaultCamelContext();
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

        final String key = randomString();
        final String val = randomString();

        camelctx.start();
        try {
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
        } finally {
            camelctx.stop();
        }
    }

    private String randomString() {
        return UUID.randomUUID().toString();
    }
}
