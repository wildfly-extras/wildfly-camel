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

package org.wildfly.camel.test.hystrix;

import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.impl.DefaultCamelContext;
import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wildfly.camel.test.hystrix.subA.DelayedHttpResponseServlet;
import org.wildfly.extension.camel.CamelAware;

@CamelAware
@RunWith(Arquillian.class)
public class HystrixIntegrationTest {

    private static final String DELAYED_RESPONSE_WAR = "delayed-http-response.war";
    private static final Logger LOG = LoggerFactory.getLogger(HystrixIntegrationTest.class);

    @ArquillianResource
    Deployer deployer;

    @Deployment
    public static JavaArchive createDeployment() {
        return ShrinkWrap.create(JavaArchive.class, "camel-hystrix-tests.jar");
    }

    @Deployment(testable = false, managed = false, name = DELAYED_RESPONSE_WAR)
    public static WebArchive createDelayedResponseDeployment() {
        return ShrinkWrap.create(WebArchive.class, DELAYED_RESPONSE_WAR)
            .addClass(DelayedHttpResponseServlet.class);
    }

    @Test
    public void testHystrixCircuitBreakerNoFallback() throws Exception {
        CamelContext camelctx = new DefaultCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:start")
                .circuitBreaker()
                    .hystrixConfiguration()
                        .executionTimeoutInMilliseconds(5000)
                    .end()
                    .to("undertow:http://localhost:8080/delayed-http-response/delay-me")
                .onFallback()
                    .setBody(constant("Hello Kermit"))
                .end();
            }
        });

        try {
            deployer.deploy(DELAYED_RESPONSE_WAR);
            camelctx.start();

            ProducerTemplate template = camelctx.createProducerTemplate();
            String result = template.requestBody("direct:start", null, String.class);

            Assert.assertEquals("Hello World", result);
        } finally {
            camelctx.close();
            deployer.undeploy(DELAYED_RESPONSE_WAR);
        }
    }

    @Test
    public void testHystrixCircuitBreakerFallback() throws Exception {
        LOG.info("[wfc#1507] testHystrixCircuitBreakerFallback start: {}", System.currentTimeMillis());

        CamelContext camelctx = new DefaultCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:start")
                .circuitBreaker()
                    .hystrixConfiguration()
                        .executionTimeoutInMilliseconds(2000)
                    .end()
                    .to("undertow:http://localhost:8080/delayed-http-response/delay-me")
                .onFallback()
                    .setBody(constant("Hello Kermit"))
                .end();
            }
        });

        try {
            LOG.info("[wfc#1507] DelayedHttpResponseServlet deploy: {}", System.currentTimeMillis());
            deployer.deploy(DELAYED_RESPONSE_WAR);
            camelctx.start();

            ProducerTemplate template = camelctx.createProducerTemplate();

            LOG.info("[wfc#1507] ProducerTemplate.requestBody() start: {}", System.currentTimeMillis());
            String result = template.requestBody("direct:start", null, String.class);
            LOG.info("[wfc#1507] ProducerTemplate.requestBody() end, result = {} : {}", result, System.currentTimeMillis());

            Assert.assertEquals("Hello Kermit", result);
        } finally {
            LOG.info("[wfc#1507] Camel context shutdown: {}", System.currentTimeMillis());
            camelctx.close();

            LOG.info("[wfc#1507] DelayedHttpResponseServlet undeploy: {}", System.currentTimeMillis());
            deployer.undeploy(DELAYED_RESPONSE_WAR);
        }
    }
}
