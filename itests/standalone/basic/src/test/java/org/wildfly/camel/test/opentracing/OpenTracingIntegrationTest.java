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
package org.wildfly.camel.test.opentracing;

import java.util.concurrent.TimeUnit;

import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.NotifyBuilder;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.opentracing.OpenTracingTracer;
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
public class OpenTracingIntegrationTest {

    @Deployment
    public static JavaArchive createDeployment() {
        return ShrinkWrap.create(JavaArchive.class, "camel-opentracing-tests.jar");
    }

    @Test
    public void testOpenTracingComponent() throws Exception {
        CamelContext camelctx = new DefaultCamelContext();

        OpenTracingTracer tracer = new OpenTracingTracer();
        tracer.init(camelctx);

        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("seda:dude").routeId("dude")
                .log("routing at ${routeId}")
                .delay(simple("${random(1000,2000)}"));
            }
        });

        camelctx.start();
        try {
            ProducerTemplate template = camelctx.createProducerTemplate();
            NotifyBuilder notify = new NotifyBuilder(camelctx).whenDone(5).create();

            for (int i = 0; i < 5; i++) {
                template.sendBody("seda:dude", "Hello World");
            }

            Assert.assertTrue(notify.matches(30, TimeUnit.SECONDS));
        } finally {
            camelctx.close();
        }
    }
}
