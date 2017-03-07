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
package org.wildfly.camel.test.nats;

import java.util.Properties;

import io.nats.client.Connection;
import io.nats.client.ConnectionFactory;
import org.apache.camel.CamelContext;
import org.apache.camel.Endpoint;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.component.nats.NatsConsumer;
import org.apache.camel.impl.DefaultCamelContext;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.camel.test.common.rule.ExecutableResource;
import org.wildfly.camel.test.common.utils.EnvironmentUtils;
import org.wildfly.extension.camel.CamelAware;

@CamelAware
@RunWith(Arquillian.class)
public class NatsIntegrationTest {

    private static final String NATS_DOWNLOAD_BASE_URL = "https://github.com/nats-io/gnatsd/releases/download";
    private static final String NATS_VERSION = "v0.9.4";

    @Rule
    public ExecutableResource nats = new ExecutableResource().builder()
        .downloadFrom(resolveDownloadURL())
        .executable(String.format("%s/gnatsd", resolveExecutableName()))
        .args("-DV -a 127.0.0.1")
        .waitForPort(4222)
        .managed(false)
        .build();

    @Deployment
    public static JavaArchive createDeployment() {
        return ShrinkWrap.create(JavaArchive.class, "camel-nats-tests.jar")
            .addClasses(EnvironmentUtils.class, ExecutableResource.class);
    }

    @Test
    public void testNatsComponent() throws Exception {
        CamelContext camelctx = new DefaultCamelContext();
        Endpoint endpoint = camelctx.getEndpoint("nats://localhost:4222?topic=test");
        Assert.assertNotNull(endpoint);
        Assert.assertEquals(endpoint.getClass().getName(), "org.apache.camel.component.nats.NatsEndpoint");
    }

    @Test
    public void testNatsRoutes() throws Exception {
        // There is no gnatsd binary for AIX
        Assume.assumeFalse("[#1684] NatsIntegrationTest fails on AIX", EnvironmentUtils.isAIX());

        CamelContext camelctx = new DefaultCamelContext();
        try {
            nats.startProcess();

            camelctx.addRoutes(new RouteBuilder() {
                @Override
                public void configure() throws Exception {
                    from("nats://localhost:4222?topic=test").id("nats-route")
                    .to("mock:result");
                }
            });

            MockEndpoint to = camelctx.getEndpoint("mock:result", MockEndpoint.class);
            to.expectedBodiesReceived("{Subject=test;Reply=null;Payload=<test-message>}");
            to.expectedMessageCount(1);

            camelctx.start();

            // Make sure the consumer has subscribed to the topic before sending messages
            NatsConsumer consumer = (NatsConsumer) camelctx.getRoute("nats-route").getConsumer();
            int count = 0;
            while(!consumer.isSubscribed()) {
                Thread.sleep(500);
                count += 1;
                if (count > 10) {
                    throw new IllegalStateException("Gave up waiting for nats consumer to subscribe to topic");
                }
            }

            Properties opts = new Properties();
            opts.put("servers", "nats://localhost:4222");

            ConnectionFactory factory = new ConnectionFactory(opts);
            Connection connection = factory.createConnection();
            connection.publish("test", "test-message".getBytes());
            
            to.assertIsSatisfied(5000);

        } finally {
            camelctx.stop();
            nats.stopProcess();
        }
    }

    private static String resolveDownloadURL() {
        return String.format("%s/%s/%s.zip", NATS_DOWNLOAD_BASE_URL, NATS_VERSION, resolveExecutableName());
    }

    private static String resolveExecutableName() {
        String osName = "linux";
        if (EnvironmentUtils.isMac()) {
            osName = "darwin";
        } else if (EnvironmentUtils.isWindows()) {
            osName = "windows";
        }
        return String.format("gnatsd-%s-%s-amd64", NATS_VERSION, osName);
    }
}
