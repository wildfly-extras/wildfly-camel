/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.wildfly.camel.test.spring.redis;

import javax.naming.Context;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.Processor;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.redis.RedisConstants;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.gravia.runtime.ServiceLocator;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.wildfly.camel.test.common.utils.AvailablePortFinder;
import org.wildfly.extension.camel.CamelAware;
import org.wildfly.extension.camel.CamelContextFactory;
import org.wildfly.extension.camel.CamelContextRegistry;
import org.wildfly.extension.camel.WildFlyCamelContext;

import redis.embedded.RedisServer;

@CamelAware
@RunWith(Arquillian.class)
@ServerSetup({ SpringRedisIntegrationTest.RedisServerSetupTask.class })
public class SpringRedisIntegrationTest {

    @ArquillianResource
    CamelContextRegistry contextRegistry;

    static class RedisServerSetupTask implements ServerSetupTask {

        RedisServer redisServer;
        
        @Override
        public void setup(final ManagementClient managementClient, String containerId) throws Exception {
            int port = AvailablePortFinder.getNextAvailable(6379);
            AvailablePortFinder.storePortInfo("redis-port", port);
            redisServer = new RedisServer(port);
            redisServer.start();
        }

        @Override
        public void tearDown(final ManagementClient managementClient, String containerId) throws Exception {
            if (redisServer != null) {
                redisServer.stop();       
            }
        }
    }

    @Deployment
    public static JavaArchive deployment() {
        JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "camel-redis-tests");
        archive.addClasses(AvailablePortFinder.class);
        return archive;
    }

    @Test
    @SuppressWarnings("rawtypes")
    public void testRedisRoute() throws Exception {

        JedisConnectionFactory connectionFactory = new JedisConnectionFactory();
        connectionFactory.afterPropertiesSet();
        
        RedisTemplate redisTemplate = new RedisTemplate();
        redisTemplate.setConnectionFactory(connectionFactory);
        redisTemplate.afterPropertiesSet();
        
        CamelContextFactory contextFactory = ServiceLocator.getRequiredService(CamelContextFactory.class);
        WildFlyCamelContext camelctx = contextFactory.createCamelContext(getClass().getClassLoader());
        Context jndictx = camelctx.getNamingContext();
        jndictx.bind("redisTemplate", redisTemplate);
        
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                int redisPort = AvailablePortFinder.readPortInfo("redis-port");
                from("direct:start")
                .to("spring-redis://localhost:" + redisPort + "?redisTemplate=#redisTemplate");
            }
        });

        camelctx.start();
        try {
            Object[] headers = new Object[] {
                    RedisConstants.COMMAND, "SET",
                    RedisConstants.KEY, "key1",
                    RedisConstants.VALUE, "value"
                    };
            
            ProducerTemplate producer = camelctx.createProducerTemplate();
            producer.send("direct:start", new Processor() {
                public void process(Exchange exchange) throws Exception {
                    Message in = exchange.getIn();
                    for (int i = 0; i < headers.length; i = i + 2) {
                        in.setHeader(headers[i].toString(), headers[i + 1]);
                    }
                }
            });
            Assert.assertEquals("value", redisTemplate.opsForValue().get("key1"));
        } finally {
            camelctx.stop();
        }
    }
}