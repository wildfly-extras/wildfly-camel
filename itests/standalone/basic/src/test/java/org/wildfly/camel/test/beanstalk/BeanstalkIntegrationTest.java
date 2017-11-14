/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.wildfly.camel.test.beanstalk;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.ExchangePattern;
import org.apache.camel.Processor;
import org.apache.camel.Producer;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.beanstalk.BeanstalkCommand;
import org.apache.camel.component.beanstalk.BeanstalkComponent;
import org.apache.camel.component.beanstalk.BeanstalkEndpoint;
import org.apache.camel.component.beanstalk.BeanstalkProducer;
import org.apache.camel.component.beanstalk.ConnectionSettings;
import org.apache.camel.component.beanstalk.ConnectionSettingsFactory;
import org.apache.camel.component.beanstalk.Headers;
import org.apache.camel.component.beanstalk.processors.DeleteCommand;
import org.apache.camel.component.beanstalk.processors.PutCommand;
import org.apache.camel.component.beanstalk.processors.ReleaseCommand;
import org.apache.camel.component.beanstalk.processors.TouchCommand;
import org.apache.camel.impl.DefaultCamelContext;
import org.hamcrest.CoreMatchers;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.objenesis.Objenesis;
import org.wildfly.extension.camel.CamelAware;

import com.surftools.BeanstalkClient.Client;

import net.bytebuddy.ByteBuddy;

@CamelAware
@RunWith(Arquillian.class)
public class BeanstalkIntegrationTest {

    private static final String TEST_MESSAGE = "hello, world";

    @Deployment
    public static JavaArchive createdeployment() {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "camel-beanstalk-tests");
        archive.addPackages(true, Mockito.class.getPackage(), Objenesis.class.getPackage(), ByteBuddy.class.getPackage());
        return archive;
    }

    private Client client;

    @Before
    public void setUp() throws Exception {
        client = Mockito.mock(Client.class);
        BeanstalkComponent.setConnectionSettingsFactory(new ConnectionSettingsFactory() {
            @Override
            public ConnectionSettings parseUri(String uri) {
                return new MockConnectionSettings(client);
            }
        });
    }

    @After
    public void tearDown() throws Exception {
        BeanstalkComponent.setConnectionSettingsFactory(ConnectionSettingsFactory.DEFAULT);
    }

    @Test
    public void testPut() throws Exception {
        final long priority = BeanstalkComponent.DEFAULT_PRIORITY;
        final int delay = BeanstalkComponent.DEFAULT_DELAY;
        final int timeToRun = BeanstalkComponent.DEFAULT_TIME_TO_RUN;
        final byte[] payload = stringToBytes(TEST_MESSAGE);
        final long jobId = 111;

        Mockito.when(client.put(priority, delay, timeToRun, payload)).thenReturn(jobId);

        CamelContext camelctx = createCamelContext();
        BeanstalkEndpoint beanstalkEndpoint = camelctx.getEndpoint("beanstalk:tube", BeanstalkEndpoint.class);

        final Producer producer = beanstalkEndpoint.createProducer();
        Assert.assertNotNull("Producer", producer);
        Assert.assertThat("Producer class", producer, CoreMatchers.instanceOf(BeanstalkProducer.class));
        Assert.assertThat("Processor class", ((BeanstalkProducer) producer).getCommand(), CoreMatchers.instanceOf(PutCommand.class));

        camelctx.start();
        try {
            ProducerTemplate template = camelctx.createProducerTemplate();
            final Exchange exchange = template.send(beanstalkEndpoint, ExchangePattern.InOnly, new Processor() {
                public void process(Exchange exchange) {
                    exchange.getIn().setBody(TEST_MESSAGE);
                }
            });

            Assert.assertEquals("Job ID in exchange", Long.valueOf(jobId), exchange.getIn().getHeader(Headers.JOB_ID, Long.class));
            Mockito.verify(client).put(priority, delay, timeToRun, payload);
        } finally {
            camelctx.stop();
        }
    }

    @Test
    public void testDelete() throws Exception {
        final long jobId = 111;

        CamelContext camelctx = createCamelContext();
        BeanstalkEndpoint beanstalkEndpoint = camelctx.getEndpoint("beanstalk:tube", BeanstalkEndpoint.class);

        beanstalkEndpoint.setCommand(BeanstalkCommand.delete);
        Producer producer = beanstalkEndpoint.createProducer();
        Assert.assertNotNull("Producer", producer);
        Assert.assertThat("Producer class", producer, CoreMatchers.instanceOf(BeanstalkProducer.class));
        Assert.assertThat("Processor class", ((BeanstalkProducer) producer).getCommand(), CoreMatchers.instanceOf(DeleteCommand.class));

        Mockito.when(client.delete(jobId)).thenReturn(true);

        camelctx.start();
        try {
            ProducerTemplate template = camelctx.createProducerTemplate();
            final Exchange exchange = template.send(beanstalkEndpoint, ExchangePattern.InOnly, new Processor() {
                public void process(Exchange exchange) {
                    exchange.getIn().setHeader(Headers.JOB_ID, jobId);
                }
            });

            Assert.assertEquals("Op result", Boolean.TRUE, exchange.getIn().getHeader(Headers.RESULT, Boolean.class));
            Assert.assertEquals("Job ID in exchange", Long.valueOf(jobId), exchange.getIn().getHeader(Headers.JOB_ID, Long.class));
            Mockito.verify(client).delete(jobId);
        } finally {
            camelctx.stop();
        }
    }

    @Test
    public void testRelease() throws Exception {
        final long priority = BeanstalkComponent.DEFAULT_PRIORITY;
        final int delay = BeanstalkComponent.DEFAULT_DELAY;
        final long jobId = 111;

        CamelContext camelctx = createCamelContext();
        BeanstalkEndpoint beanstalkEndpoint = camelctx.getEndpoint("beanstalk:tube", BeanstalkEndpoint.class);

        beanstalkEndpoint.setCommand(BeanstalkCommand.release);
        Producer producer = beanstalkEndpoint.createProducer();
        Assert.assertNotNull("Producer", producer);
        Assert.assertThat("Producer class", producer, CoreMatchers.instanceOf(BeanstalkProducer.class));
        Assert.assertThat("Processor class", ((BeanstalkProducer) producer).getCommand(), CoreMatchers.instanceOf(ReleaseCommand.class));

        Mockito.when(client.release(jobId, priority, delay)).thenReturn(true);

        camelctx.start();
        try {
            ProducerTemplate template = camelctx.createProducerTemplate();
            final Exchange exchange = template.send(beanstalkEndpoint, ExchangePattern.InOnly, new Processor() {
                public void process(Exchange exchange) {
                    exchange.getIn().setHeader(Headers.JOB_ID, jobId);
                }
            });

            Assert.assertEquals("Op result", Boolean.TRUE, exchange.getIn().getHeader(Headers.RESULT, Boolean.class));
            Assert.assertEquals("Job ID in exchange", Long.valueOf(jobId), exchange.getIn().getHeader(Headers.JOB_ID, Long.class));
            Mockito.verify(client).release(jobId, priority, delay);
        } finally {
            camelctx.stop();
        }
    }

    @Test
    public void testTouch() throws Exception {
        final long jobId = 111;

        CamelContext camelctx = createCamelContext();
        BeanstalkEndpoint beanstalkEndpoint = camelctx.getEndpoint("beanstalk:tube", BeanstalkEndpoint.class);

        beanstalkEndpoint.setCommand(BeanstalkCommand.touch);
        Producer producer = beanstalkEndpoint.createProducer();
        Assert.assertNotNull("Producer", producer);
        Assert.assertThat("Producer class", producer, CoreMatchers.instanceOf(BeanstalkProducer.class));
        Assert.assertThat("Processor class", ((BeanstalkProducer) producer).getCommand(), CoreMatchers.instanceOf(TouchCommand.class));

        Mockito.when(client.touch(jobId)).thenReturn(true);

        camelctx.start();
        try {
            ProducerTemplate template = camelctx.createProducerTemplate();
            final Exchange exchange = template.send(beanstalkEndpoint, ExchangePattern.InOnly, new Processor() {
                public void process(Exchange exchange) {
                    exchange.getIn().setHeader(Headers.JOB_ID, jobId);
                }
            });

            Assert.assertEquals("Op result", Boolean.TRUE, exchange.getIn().getHeader(Headers.RESULT, Boolean.class));
            Assert.assertEquals("Job ID in exchange", Long.valueOf(jobId), exchange.getIn().getHeader(Headers.JOB_ID, Long.class));
            Mockito.verify(client).touch(jobId);
        } finally {
            camelctx.stop();
        }
    }

    private byte[] stringToBytes(final String s) throws IOException {
        final ByteArrayOutputStream byteOS = new ByteArrayOutputStream();
        final DataOutputStream dataStream = new DataOutputStream(byteOS);
        try {
            dataStream.writeBytes(s);
            dataStream.flush();
            return byteOS.toByteArray();
        } finally {
            dataStream.close();
            byteOS.close();
        }
    }

    private CamelContext createCamelContext() throws Exception {
        CamelContext camelctx = new DefaultCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() {
                from("direct:start")
                .to("beanstalk:tube?jobPriority=1020&jobDelay=50&jobTimeToRun=65")
                .to("mock:result");
            }
        });
        return camelctx;
    }

    static class MockConnectionSettings extends ConnectionSettings {
        final Client client;

        MockConnectionSettings(Client client) {
            super("tube");
            this.client = client;
        }

        @Override
        public Client newReadingClient(boolean useBlockIO) {
            return client;
        }

        @Override
        public Client newWritingClient() {
            return client;
        }
    }
}
