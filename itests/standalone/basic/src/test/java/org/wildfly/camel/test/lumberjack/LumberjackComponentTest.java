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
package org.wildfly.camel.test.lumberjack;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLEngine;

import org.apache.camel.CamelContext;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.util.jsse.SSLContextParameters;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.gravia.resource.ManifestBuilder;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.camel.test.common.utils.AvailablePortFinder;
import org.wildfly.extension.camel.CamelAware;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.ssl.SslHandler;

@CamelAware
@RunWith(Arquillian.class)
public class LumberjackComponentTest {

    private int port;

    @Deployment
    public static JavaArchive deployment() {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "camel-lumberjack-tests");
        archive.addClasses(AvailablePortFinder.class);
        archive.addAsResource("lumberjack/window10");
        archive.addAsResource("lumberjack/window15");
        archive.setManifest(() -> {
            ManifestBuilder builder = new ManifestBuilder();
            builder.addManifestHeader("Dependencies", "io.netty");
            return builder.openStream();
        });
        return archive;
    }

    @Before
    public void before() {
        port = AvailablePortFinder.getNextAvailable();
    }

    @Test
    public void shouldListenToMessages() throws Exception {

        CamelContext camelctx = new DefaultCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            public void configure() {
                from("lumberjack:0.0.0.0:" + port).to("mock:output");
            }
        });

        // We're expecting 25 messages with Maps
        MockEndpoint mock = camelctx.getEndpoint("mock:output", MockEndpoint.class);
        mock.expectedMessageCount(25);
        mock.allMessages().body().isInstanceOf(Map.class);

        camelctx.start();
        try {
            sendMessages(port, null);

            // Then we should have the messages we're expecting
            mock.assertIsSatisfied();

            // And the first map should contains valid data (we're assuming it's also valid for the other ones)
            Map<?, ?> first = mock.getExchanges().get(0).getIn().getBody(Map.class);
            Assert.assertEquals("log", first.get("input_type"));
            Assert.assertEquals("/home/qatest/collectNetwork/log/data-integration/00000000-f000-0000-1541-8da26f200001/absorption.log", first.get("source"));
        } finally {
            camelctx.stop();
        }
    }

    private List<Integer> sendMessages(int port, SSLContextParameters sslContextParameters) throws InterruptedException {
        NioEventLoopGroup eventLoopGroup = new NioEventLoopGroup();
        try {
            // This list will hold the acknowledgment response sequence numbers
            List<Integer> responses = new ArrayList<>();

            // This initializer configures the SSL and an acknowledgment recorder
            ChannelInitializer<Channel> initializer = new ChannelInitializer<Channel>() {
                @Override
                protected void initChannel(Channel ch) throws Exception {
                    ChannelPipeline pipeline = ch.pipeline();
                    if (sslContextParameters != null) {
                        SSLEngine sslEngine = sslContextParameters.createSSLContext(null).createSSLEngine();
                        sslEngine.setUseClientMode(true);
                        pipeline.addLast(new SslHandler(sslEngine));
                    }

                    // Add the response recorder
                    pipeline.addLast(new SimpleChannelInboundHandler<ByteBuf>() {
                        @Override
                        protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
                            Assert.assertEquals(msg.readUnsignedByte(), (short) '2');
                            Assert.assertEquals(msg.readUnsignedByte(), (short) 'A');
                            synchronized (responses) {
                                responses.add(msg.readInt());
                            }
                        }
                    });
                }
            };

            // Connect to the server
            Channel channel = new Bootstrap()
                    .group(eventLoopGroup)
                    .channel(NioSocketChannel.class)
                    .handler(initializer)
                    .connect("127.0.0.1", port).sync().channel();

            // Send the 2 window frames
            TimeUnit.MILLISECONDS.sleep(100);
            channel.writeAndFlush(readSample("lumberjack/window10"));
            TimeUnit.MILLISECONDS.sleep(100);
            channel.writeAndFlush(readSample("lumberjack/window15"));
            TimeUnit.MILLISECONDS.sleep(100);

            channel.close();

            synchronized (responses) {
                return responses;
            }
        } finally {
            eventLoopGroup.shutdownGracefully();
        }
    }

    private ByteBuf readSample(String resource) {
        try (InputStream stream = getClass().getClassLoader().getResourceAsStream(resource)) {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            int input;
            while ((input = stream.read()) != -1) {
                output.write(input);
            }
            return Unpooled.wrappedBuffer(output.toByteArray());
        } catch (IOException e) {
            throw new RuntimeException("Cannot read sample data", e);
        }
    }
}
