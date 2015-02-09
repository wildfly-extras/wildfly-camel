/*
 * #%L
 * Wildfly Camel :: Testsuite
 * %%
 * Copyright (C) 2013 - 2014 RedHat
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

package org.wildfly.camel.test.quartz;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.camel.CamelContext;
import org.apache.camel.Endpoint;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.impl.DefaultCamelContext;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(Arquillian.class)
public class QuartzIntegrationTest {

    @Deployment
    public static JavaArchive deployment() {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "quartz-tests");
        return archive;
    }

    @Test
    public void testEndpointClass() throws Exception {
    	
    	final CountDownLatch latch = new CountDownLatch(3);
    	
        // [FIXME #292] Camel endpoint discovery depends on TCCL
        Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
        
        CamelContext camelctx = new DefaultCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("quartz2://mytimer?trigger.repeatCount=3&trigger.repeatInterval=100")
                .process(new Processor() {
					@Override
					public void process(Exchange exchange) throws Exception {
						latch.countDown();
					}})
                .to("mock:result");
            }
        });
        camelctx.start();
        
        Endpoint endpoint = camelctx.getEndpoints().iterator().next();
        Assert.assertEquals("org.apache.camel.component.quartz2.QuartzEndpoint", endpoint.getClass().getName());
        
        Assert.assertTrue("Countdown reached zero", latch.await(500, TimeUnit.MILLISECONDS));
    }
}
