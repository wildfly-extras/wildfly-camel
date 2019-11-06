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

package org.wildfly.camel.test.zipkin;

import java.util.concurrent.TimeUnit;

import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.NotifyBuilder;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.zipkin.ZipkinTracer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wildfly.extension.camel.CamelAware;

import zipkin2.Span;
import zipkin2.reporter.Reporter;

@CamelAware
@RunWith(Arquillian.class)
public class ZipkinIntegrationTest {

    private static final Logger LOG = LoggerFactory.getLogger(ZipkinIntegrationTest.class);

    @Deployment
    public static JavaArchive createDeployment() {
        return ShrinkWrap.create(JavaArchive.class, "camel-zipkin-tests");
    }

    @Test
    public void testZipkin() throws Exception {

        CamelContext camelctx = new DefaultCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("seda:dude").routeId("dude")
                .log("routing at ${routeId}")
                .delay(simple("${random(1000,2000)}"));
            }
        });

        ZipkinTracer zipkin = new ZipkinTracer();
        zipkin.setServiceName("dude");
        zipkin.setSpanReporter(new LoggingReporter());
        zipkin.init(camelctx);

        NotifyBuilder notify = new NotifyBuilder(camelctx).whenDone(5).create();

        camelctx.start();
        try {
            ProducerTemplate template = camelctx.createProducerTemplate();
            for (int i = 0; i < 5; i++) {
                template.sendBody("seda:dude", "Hello World");
            }
            Assert.assertTrue(notify.matches(30, TimeUnit.SECONDS));
        } finally {
            camelctx.close();
        }
    }

    static class LoggingReporter implements Reporter<Span> {

        @Override
        public void report(Span span) {
            if (!LOG.isInfoEnabled()) {
                return;
            }

            if (span == null) {
                throw new NullPointerException("span == null");
            }

            LOG.info(span.toString());
        }

        @Override
        public String toString() {
            return "LoggingReporter{name=" + LOG.getName() + "}";
        }
    }
}
