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

package org.wildfly.camel.test.netty;

import java.io.PrintWriter;
import java.net.Socket;

import org.apache.camel.CamelContext;
import org.apache.camel.PollingConsumer;
import org.apache.camel.ServiceStatus;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.impl.DefaultCamelContext;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.gravia.runtime.ServiceLocator;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.extension.camel.CamelContextRegistry;

@RunWith(Arquillian.class)
public class NettyIntegrationTest {

    private static final String SOCKET_HOST = "localhost";
    private static final int SOCKET_PORT = 7998;

    @Deployment
    public static WebArchive createDeployment() {
        final WebArchive archive = ShrinkWrap.create(WebArchive.class, "camel-netty-test.war");
        archive.addAsWebResource("netty/netty-camel-context.xml");
        return archive;
    }

    @Test
    public void testNettyTcpSocket() throws Exception {

        CamelContext camelctx = new DefaultCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("netty4:tcp://" + SOCKET_HOST + ":" + SOCKET_PORT + "?textline=true")
                .transform(simple("Hello ${body}"))
                .inOnly("seda:end");
            }
        });

        camelctx.start();
        try {
            PollingConsumer pollingConsumer = camelctx.getEndpoint("seda:end").createPollingConsumer();
            pollingConsumer.start();

            Socket socket = new Socket(SOCKET_HOST, SOCKET_PORT);
            socket.setKeepAlive(true);
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

            try {
                out.write("Kermit\n");
            } finally {
                out.close();
                socket.close();
            }

            String result = pollingConsumer.receive(3000).getIn().getBody(String.class);
            Assert.assertEquals("Hello Kermit", result);
        } finally {
            camelctx.stop();
        }
    }

    @Test
    public void testDeployedContext() throws Exception {

        CamelContextRegistry registry = ServiceLocator.getRequiredService(CamelContextRegistry.class);
        CamelContext camelctx = registry.getCamelContext("netty-context");
        Assert.assertNotNull("CamelContext not null", camelctx);
        Assert.assertEquals(ServiceStatus.Started, camelctx.getStatus());

        PollingConsumer pollingConsumer = camelctx.getEndpoint("seda:end").createPollingConsumer();
        pollingConsumer.start();

        Socket socket = new Socket(SOCKET_HOST, 7999);
        socket.setKeepAlive(true);
        PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

        try {
            out.write("Kermit\n");
        } finally {
            out.close();
            socket.close();
        }

        String result = pollingConsumer.receive().getIn().getBody(String.class);
        Assert.assertEquals("Hello Kermit", result);
    }
}
