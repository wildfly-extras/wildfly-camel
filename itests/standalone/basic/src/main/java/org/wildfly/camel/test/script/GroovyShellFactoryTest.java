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

package org.wildfly.camel.test.script;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.annotation.Resource;

import org.apache.camel.CamelContext;
import org.apache.camel.Consumer;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.ProducerTemplate;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.camel.test.script.subA.CustomGroovyShellFactory;
import org.wildfly.extension.camel.CamelAware;

@CamelAware
@RunWith(Arquillian.class)
public class GroovyShellFactoryTest {

    private static final String GROOVY_SCRIPT = "groovy-script.grv";

    @Resource(name = "java:jboss/camel/context/spring-context")
    CamelContext camelctx;

    @Deployment
    public static JavaArchive deployment() {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "script-tests.jar");
        archive.addClasses(CustomGroovyShellFactory.class);
        archive.addAsResource("script/groovy-transform-camel-context.xml");
        archive.addAsResource("script/" + GROOVY_SCRIPT, GROOVY_SCRIPT);
        archive.addAsManifestResource(new StringAsset(""), "beans.xml");
        return archive;
    }

    @Test
    public void testGroovy() throws Exception {

        CountDownProcessor proc = new CountDownProcessor(1);
        Consumer consumer = camelctx.getEndpoint("seda:end").createConsumer(proc);

        consumer.start();
        try {
            ProducerTemplate producer = camelctx.createProducerTemplate();
            producer.sendBodyAndHeader("direct:start", "10", "foo", "bar");
            producer.sendBodyAndHeader("direct:start", "2", "foo", "bar");

            Assert.assertTrue("Message not processed by consumer", proc.await(3, TimeUnit.SECONDS));
            Assert.assertEquals("Hello 1010", proc.getExchange().getIn().getBody(String.class));
        } finally {
            consumer.stop();
        }
    }

    class CountDownProcessor implements Processor {

        private final CountDownLatch latch;
        private Exchange exchange;

        CountDownProcessor(int count) {
            latch = new CountDownLatch(count);
        }

        @Override
        public synchronized void process(Exchange exchange) throws Exception {
            this.exchange = exchange;
            latch.countDown();
            System.out.println(exchange);
        }

        synchronized Exchange getExchange() {
            return exchange;
        }

        boolean await(long timeout, TimeUnit unit) throws InterruptedException {
            return latch.await(timeout, unit);
        }
    }
}
