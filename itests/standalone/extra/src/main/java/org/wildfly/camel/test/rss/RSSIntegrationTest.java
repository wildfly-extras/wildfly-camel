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

package org.wildfly.camel.test.rss;

import co.freeside.betamax.Betamax;
import co.freeside.betamax.Recorder;

import java.io.File;
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
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.extension.camel.CamelAware;

@CamelAware
@RunWith(Arquillian.class)
public class RSSIntegrationTest {

    @Rule
    public Recorder recorder = new Recorder();

    @Deployment
    public static WebArchive deployment() {
        File[] libraryDependencies = Maven.configureResolverViaPlugin().
                resolve("co.freeside:betamax", "org.codehaus.groovy:groovy-all").
                withTransitivity().
                asFile();
        final WebArchive archive = ShrinkWrap.create(WebArchive.class, "rss-tests.war");
        archive.addAsLibraries(libraryDependencies);
        archive.addAsResource("betamax.properties","betamax.properties");
        return archive;
    }

    @Test
    @Betamax(tape="redhat-rss")
    public void testEndpointClass() throws Exception {

    	final CountDownLatch latch = new CountDownLatch(1);

        CamelContext camelctx = new DefaultCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("rss://http://www.redhat.com/en/rss/press-releases?splitEntries=true&consumer.initialDelay=200&consumer.delay=1000")
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
}
