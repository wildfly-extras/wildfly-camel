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

package org.wildfly.camel.test.connector;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.camel.CamelContext;
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
import org.wildfly.camel.test.connector.foo.FooComponent;
import org.wildfly.extension.camel.CamelAware;

@CamelAware
@RunWith(Arquillian.class)
public class FooConnectorTest {

    @Deployment
    public static JavaArchive deployment() {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "camel-foo-tests");
        archive.addClasses(FooComponent.class);
        String fooPath = FooComponent.class.getPackage().getName().replace('.', '/');
        archive.addAsManifestResource("connector/META-INF/services/foo", "services/" + fooPath);
        archive.addAsResource("connector/camel-connector-schema.json", "camel-connector-schema.json");
        archive.addAsResource("connector/camel-connector.json", "camel-connector.json");
        return archive;
    }

    @Test
    public void testFooConnector() throws Exception {

        final CountDownLatch latch = new CountDownLatch(1);

        CamelContext camelctx = new DefaultCamelContext();

        FooComponent foo = new FooComponent();
        camelctx.addComponent(foo.getComponentName(), foo);

        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("foo:timerName")
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
            camelctx.stop();
        }
    }
}
