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
package org.wildfly.camel.test.ganglia.subA;

import static info.ganglia.gmetric4j.xdr.v31x.Ganglia_msg_formats.gmetadata_full;
import static info.ganglia.gmetric4j.xdr.v31x.Ganglia_msg_formats.gmetric_string;

import java.io.IOException;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Named;

import org.acplt.oncrpc.OncRpcException;
import org.acplt.oncrpc.XdrAble;
import org.acplt.oncrpc.XdrBufferDecodingStream;
import org.apache.camel.builder.RouteBuilder;
import org.wildfly.camel.test.common.utils.AvailablePortFinder;

import info.ganglia.gmetric4j.xdr.v31x.Ganglia_metadata_msg;
import info.ganglia.gmetric4j.xdr.v31x.Ganglia_value_msg;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.socket.DatagramPacket;
import io.netty.handler.codec.MessageToMessageDecoder;

@ApplicationScoped
public class FakeGangliaAgent extends RouteBuilder {

    private static final int PORT = AvailablePortFinder.getNextAvailable();

    @Override
    public void configure() throws Exception {
        fromF("netty:udp://localhost:%d/?decoders=protocolV31Decoder", PORT)
        .to("mock:result");

        from("direct:getPort")
        .setBody(constant(PORT));
    }

    @Produces
    @Named
    ProtocolV31Decoder protocolV31Decoder() {
        return new ProtocolV31Decoder();
    }

    @ChannelHandler.Sharable
    class ProtocolV31Decoder extends MessageToMessageDecoder<DatagramPacket> {
        @Override
        protected void decode(ChannelHandlerContext ctx, DatagramPacket packet, List<Object> out) throws OncRpcException, IOException {
            byte[] bytes = new byte[packet.content().readableBytes()];
            packet.content().readBytes(bytes);

            XdrBufferDecodingStream xbds = new XdrBufferDecodingStream(bytes);
            xbds.beginDecoding();
            int id = xbds.xdrDecodeInt() & 0xbf;
            xbds.endDecoding();
            XdrAble outMsg;
            if (id == gmetadata_full) {
                outMsg = new Ganglia_metadata_msg();
            } else if (id == gmetric_string) {
                outMsg = new Ganglia_value_msg();
            } else {
                throw new IllegalStateException("Unexpected id");
            }

            xbds = new XdrBufferDecodingStream(bytes);
            xbds.beginDecoding();
            outMsg.xdrDecode(xbds);
            xbds.endDecoding();
            out.add(outMsg);
        }
    }
}
