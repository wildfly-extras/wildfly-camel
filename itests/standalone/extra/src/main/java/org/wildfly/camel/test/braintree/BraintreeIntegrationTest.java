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

package org.wildfly.camel.test.braintree;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.braintree.BraintreeComponent;
import org.apache.camel.component.braintree.BraintreeConfiguration;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.util.IntrospectionSupport;
import org.apache.commons.lang3.StringUtils;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.gravia.resource.ManifestBuilder;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.extension.camel.CamelAware;

/*
 * To run this test you need to set the following environment variables with
 * Braintree's keys:
 *
 *   CAMEL_BRAINTREE_ENVIRONMENT=SANDBOX
 *   CAMEL_BRAINTREE_MERCHANT_ID=...
 *   CAMEL_BRAINTREE_PUBLIC_KEY=...
 *   CAMEL_BRAINTREE_PRIVATE_KEY=...
 */
@Ignore("[#1058] Requires Braintree tokens")
@CamelAware
@RunWith(Arquillian.class)
public class BraintreeIntegrationTest {
    private static final String BRAINTREE_PROPERTIES = "braintree/braintree.properties";

    @Deployment
    public static JavaArchive deployment() {
        return ShrinkWrap.create(JavaArchive.class, "camel-braintree-tests")
            .addAsResource(BRAINTREE_PROPERTIES, BRAINTREE_PROPERTIES)
            .setManifest(new Asset() {
                @Override
                public InputStream openStream() {
                    ManifestBuilder builder = new ManifestBuilder();
                    builder.addManifestHeader("Dependencies", "org.apache.commons.lang3");
                    return builder.openStream();
                }
            });
    }

    @Test
    public void testEndpointClass() throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);

        CamelContext camelctx = createCamelContext();
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
            Assert.assertTrue("Countdown reached zero", latch.await(30, TimeUnit.SECONDS));
        } finally {
            camelctx.stop();
        }
    }

    protected CamelContext createCamelContext() throws Exception {
        CamelContext context = new DefaultCamelContext();

        // try read Braintree component configuration from BRAINTREE_PROPERTIES
        final Properties properties = new Properties();
        try {
            InputStream stream = getClass().getResourceAsStream(BRAINTREE_PROPERTIES);
            if (stream != null) {
                properties.load(stream);
            }
        } catch (Exception e) {
            throw new IOException(String.format("%s could not be loaded: %s", BRAINTREE_PROPERTIES, e.getMessage()), e);
        }

        Map<String, Object> options = new HashMap<>();
        for (Map.Entry<Object, Object> entry : properties.entrySet()) {
            options.put(entry.getKey().toString(), entry.getValue());
        }

        // Add options from environment if not defined in BRAINTREE_PROPERTIES
        // useful for CI tests
        addOptionFromEnvIfMissing(options, "environment", "CAMEL_BRAINTREE_ENVIRONMENT");
        addOptionFromEnvIfMissing(options, "merchantId" , "CAMEL_BRAINTREE_MERCHANT_ID");
        addOptionFromEnvIfMissing(options, "publicKey"  , "CAMEL_BRAINTREE_PUBLIC_KEY");
        addOptionFromEnvIfMissing(options, "privateKey" , "CAMEL_BRAINTREE_PRIVATE_KEY");

        final BraintreeConfiguration configuration = new BraintreeConfiguration();
        IntrospectionSupport.setProperties(configuration, options);

        // add BraintreeComponent to Camel context
        final BraintreeComponent component = new BraintreeComponent(context);
        component.setConfiguration(configuration);


        context.addComponent("braintree", component);

        return context;
    }

    protected void addOptionFromEnvIfMissing(Map<String, Object> options, String name, String envName) {
        if (!options.containsKey(name)) {
            String value = System.getenv(envName);
            if (StringUtils.isNotBlank(value)) {
                options.put(name, value);
            }
        }
    }
}
