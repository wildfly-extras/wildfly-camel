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

package org.wildfly.camel.test.braintree;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.braintree.BraintreeComponent;
import org.apache.camel.component.braintree.BraintreeConfiguration;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.support.IntrospectionSupport;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.After;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.camel.test.common.utils.EnvironmentUtils;
import org.wildfly.extension.camel.CamelAware;

import com.braintreegateway.Customer;
import com.braintreegateway.CustomerRequest;
import com.braintreegateway.Result;

/*
 * To run this test you need to set the following environment variables with
 * Braintree's keys:
 *
 *   CAMEL_BRAINTREE_ENVIRONMENT=SANDBOX
 *   CAMEL_BRAINTREE_MERCHANT_ID=...
 *   CAMEL_BRAINTREE_PUBLIC_KEY=...
 *   CAMEL_BRAINTREE_PRIVATE_KEY=...
 */
@CamelAware
@RunWith(Arquillian.class)
public class BraintreeIntegrationTest {

    // Enum values correspond to environment variable names
    private enum BraintreeOption {
        CAMEL_BRAINTREE_ENVIRONMENT("environment"),
        CAMEL_BRAINTREE_MERCHANT_ID("merchantId"),
        CAMEL_BRAINTREE_PUBLIC_KEY("publicKey"),
        CAMEL_BRAINTREE_PRIVATE_KEY("privateKey");

        private String configPropertyName;

        BraintreeOption(String configPropertyName) {
            this.configPropertyName = configPropertyName;
        }
    }

    @Deployment
    public static JavaArchive deployment() {
        return ShrinkWrap.create(JavaArchive.class, "camel-braintree-tests")
            .addClasses(EnvironmentUtils.class);
    }

    @Before
    public void setUp() {
        if (EnvironmentUtils.isIbmJDK()) {
            System.setProperty("https.protocols", "TLSv1.2");
        }
    }

    @After
    public void tearDown() {
        if (EnvironmentUtils.isIbmJDK()) {
            System.clearProperty("https.protocols");
        }
    }

    @Test
    public void testBraintreeClientTokenGateway() throws Exception {

        Map<String, Object> braintreeOptions = createBraintreeOptions();

        Assume.assumeTrue("[#1679] Enable Braintree testing in Jenkins",
                braintreeOptions.size() == BraintreeOption.values().length);

        final CountDownLatch latch = new CountDownLatch(1);
        final CamelContext camelctx = new DefaultCamelContext();
        final BraintreeConfiguration configuration = new BraintreeConfiguration();
        configuration.setHttpLogLevel(Level.WARNING);
        IntrospectionSupport.setProperties(configuration, braintreeOptions);

        // add BraintreeComponent to Camel context
        final BraintreeComponent component = new BraintreeComponent(camelctx);
        component.setConfiguration(configuration);
        camelctx.addComponent("braintree", component);

        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("timer://braintree?repeatCount=1")
                    .to("braintree:clientToken/generate")
                    .process(new Processor() {
                        @Override
                        public void process(Exchange exchange) throws Exception {
                            latch.countDown();
                        }})
                    .to("mock:result");
            }
        });

        camelctx.start();
        try {
            Assert.assertTrue("Countdown reached zero", latch.await(5, TimeUnit.MINUTES));
        } finally {
            camelctx.close();
        }
    }

    @Test
    public void testBraintreeCustomerGateway() throws Exception {

        Map<String, Object> braintreeOptions = createBraintreeOptions();

        // Do nothing if the required credentials are not present
        Assume.assumeTrue(braintreeOptions.size() == BraintreeOption.values().length);

        final CountDownLatch latch = new CountDownLatch(2);
        final CamelContext camelctx = new DefaultCamelContext();
        final BraintreeConfiguration configuration = new BraintreeConfiguration();
        IntrospectionSupport.setProperties(configuration, braintreeOptions);

        // add BraintreeComponent to Camel context
        final BraintreeComponent component = new BraintreeComponent(camelctx);
        component.setConfiguration(configuration);
        camelctx.addComponent("braintree", component);


        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:create")
                    .to("braintree:customer/create?inBody=request")
                    .process(new Processor() {
                        @Override
                        public void process(Exchange exchange) throws Exception {
                            latch.countDown();
                        }});
                from("direct:delete")
                    .to("braintree:customer/delete?inBody=id")
                    .process(new Processor() {
                        @Override
                        public void process(Exchange exchange) throws Exception {
                            latch.countDown();
                        }});

            }
        });

        camelctx.start();

        // ****************************
        // Create a customer
        // ****************************

        @SuppressWarnings("unchecked")
        Result<Customer> createResult = camelctx.createProducerTemplate().requestBody(
            "direct:create",
            new CustomerRequest()
                .firstName("user")
                .lastName(UUID.randomUUID().toString())
                .company("Apache")
                .email("user@braintree.camel")
                .website("http://user.braintree.camel"),
            Result.class
        );

        Assert.assertNotNull(createResult);
        Assert.assertTrue(createResult.isSuccess());
        Assert.assertNotNull(createResult.getTarget());
        Assert.assertNotNull(createResult.getTarget().getId());

        // ****************************
        // Delete the customer
        // ****************************

        @SuppressWarnings("unchecked")
        Result<Customer> deleteResult = camelctx.createProducerTemplate().requestBody(
            "direct:delete",
            createResult.getTarget().getId(),
            Result.class);

        Assert.assertNotNull(deleteResult);
        Assert.assertTrue(deleteResult.isSuccess());
        Assert.assertNull(deleteResult.getTarget());

        try {
            Assert.assertTrue("Countdown reached zero", latch.await(5, TimeUnit.MINUTES));
        } finally {
            camelctx.close();
        }
    }

    protected Map<String, Object> createBraintreeOptions() throws Exception {
        final Map<String, Object> options = new HashMap<>();

        for (BraintreeOption option : BraintreeOption.values()) {
            String envVar = System.getenv(option.name());
            if (envVar == null || envVar.length() == 0) {
                options.clear();
            } else {
                options.put(option.configPropertyName, envVar);
            }
        }

        return options;
    }
}
