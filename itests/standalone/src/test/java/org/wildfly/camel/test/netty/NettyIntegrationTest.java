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
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.impl.DefaultCamelContext;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(Arquillian.class)
public class NettyIntegrationTest {

    private static final String SOCKET_HOST = "localhost";
    private static final int SOCKET_PORT = 7999;

    @Deployment
    public static WebArchive createdeployment() {
        final WebArchive archive = ShrinkWrap.create(WebArchive.class, "camel-test.war");
        return archive;
    }

    @Test
    public void testNettyTcpSocket() throws Exception {
        CamelContext camelContext = new DefaultCamelContext();

        camelContext.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("netty4:tcp://" + SOCKET_HOST + ":" + SOCKET_PORT + "?textline=true")
                        .transform(simple("Hello ${body}"))
                        .to("direct:end");
            }
        });

        camelContext.start();

        PollingConsumer pollingConsumer = camelContext.getEndpoint("direct:end").createPollingConsumer();
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

        String result = pollingConsumer.receive().getIn().getBody(String.class);

        Assert.assertEquals("Hello Kermit", result);

        camelContext.stop();
    }
}
