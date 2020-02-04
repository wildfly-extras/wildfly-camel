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
package org.wildfly.camel.test.ipfs;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.ipfs.IPFSEndpoint;
import org.apache.camel.impl.DefaultCamelContext;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.extension.camel.CamelAware;

import io.nessus.ipfs.client.IPFSClient;
import io.nessus.utils.StreamUtils;

/*

    > ipfs config Addresses.Gateway /ip4/tcp/8088

    > ipfs daemon

    Initializing daemon...
    go-ipfs version: 0.4.22-
    Repo version: 7
    System version: amd64/darwin
    Golang version: go1.12.7
    Swarm listening on /ip4/tcp/4001
    Swarm listening on /ip4/192.168.178.30/tcp/4001
    Swarm listening on /ip6/::1/tcp/4001
    Swarm listening on /ip6/fd00::14f1:aeeb:14b5:1dc/tcp/4001
    Swarm listening on /ip6/fd00::98b3:371b:e305:f4e2/tcp/4001
    Swarm listening on /ip6/fd00::b926:d23b:c07d:5e80/tcp/4001
    Swarm listening on /p2p-circuit
    Swarm announcing /ip4/tcp/4001
    Swarm announcing /ip4/192.168.178.30/tcp/4001
    Swarm announcing /ip4/2.201.86.143/tcp/61426
    Swarm announcing /ip6/::1/tcp/4001
    Swarm announcing /ip6/fd00::14f1:aeeb:14b5:1dc/tcp/4001
    Swarm announcing /ip6/fd00::98b3:371b:e305:f4e2/tcp/4001
    Swarm announcing /ip6/fd00::b926:d23b:c07d:5e80/tcp/4001
    API server listening on /ip4/tcp/5001
    WebUI: http://127.0.0.1:5001/webui
    Gateway (readonly) server listening on /ip4/tcp/8088
    Daemon is ready

 */

@CamelAware
@RunWith(Arquillian.class)
public class IPFSIntegrationTest {

    private static final String SINGLE_HASH = "QmUD7uG5prAMHbcCfp4x1G1mMSpywcSMHTGpq62sbpDAg6";
    private static final String RECURSIVE_HASH = "QmdcE2PmF5SBGCs1EVtznNTFPu4GoJztgJmAvdq66XxM3h";

    @Deployment
    public static JavaArchive createDeployment() {
        return ShrinkWrap.create(JavaArchive.class, "camel-ipfs-tests");
    }

    @Test
    public void ipfsVersion() throws Exception {

        CamelContext camelctx = new DefaultCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:startA").to("ipfs:version");
            }
        });

        camelctx.start();
        try {
            assumeIPFSAvailable(camelctx);

            ProducerTemplate producer = camelctx.createProducerTemplate();
            String resA = producer.requestBody("direct:startA", null, String.class);
            Assert.assertTrue("Expecting 0.4 in: " + resA, resA.startsWith("0.4"));
        } finally {
            camelctx.close();
        }
    }

    @Test
    public void ipfsAddSingle() throws Exception {

        CamelContext camelctx = new DefaultCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:start").to("ipfs:add");
            }
        });


        camelctx.start();
        try {
            assumeIPFSAvailable(camelctx);

            Path path = Paths.get("src/test/resources/ipfs/etc/userfile.txt");
            ProducerTemplate producer = camelctx.createProducerTemplate();
            String res = producer.requestBody("direct:start", path, String.class);
            Assert.assertEquals(SINGLE_HASH, res);
        } finally {
            camelctx.close();
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    public void ipfsAddRecursive() throws Exception {

        CamelContext camelctx = new DefaultCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:start").to("ipfs:add");
            }
        });

        Path path = Paths.get("src/test/resources/ipfs");

        camelctx.start();
        try {
            assumeIPFSAvailable(camelctx);

            ProducerTemplate producer = camelctx.createProducerTemplate();
            List<String> res = producer.requestBody("direct:start", path, List.class);
            Assert.assertEquals(10, res.size());
            Assert.assertEquals(RECURSIVE_HASH, res.get(9));
        } finally {
            camelctx.close();
        }
    }

    @Test
    public void ipfsCat() throws Exception {

        CamelContext camelctx = new DefaultCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:start").to("ipfs:cat");
            }
        });

        camelctx.start();
        try {
            assumeIPFSAvailable(camelctx);

            ProducerTemplate producer = camelctx.createProducerTemplate();
            InputStream res = producer.requestBody("direct:start", SINGLE_HASH, InputStream.class);
            verifyFileContent(res);
        } finally {
            camelctx.close();
        }
    }

    @Test
    public void ipfsGetSingle() throws Exception {

        CamelContext camelctx = new DefaultCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:start").to("ipfs:get?outdir=target");
            }
        });

        camelctx.start();
        try {
            assumeIPFSAvailable(camelctx);

            ProducerTemplate producer = camelctx.createProducerTemplate();
            Path res = producer.requestBody("direct:start", SINGLE_HASH, Path.class);
            Assert.assertEquals(Paths.get("target", SINGLE_HASH), res);
            verifyFileContent(new FileInputStream(res.toFile()));
        } finally {
            camelctx.close();
        }
    }

    @Test
    public void ipfsGetRecursive() throws Exception {

        CamelContext camelctx = new DefaultCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:start").to("ipfs:get?outdir=target");
            }
        });

        camelctx.start();
        try {
            assumeIPFSAvailable(camelctx);

            ProducerTemplate producer = camelctx.createProducerTemplate();
            Path res = producer.requestBody("direct:start", RECURSIVE_HASH, Path.class);
            Assert.assertEquals(Paths.get("target", RECURSIVE_HASH), res);
            Assert.assertTrue(res.toFile().isDirectory());
            Assert.assertTrue(res.resolve("index.html").toFile().exists());
        } finally {
            camelctx.close();
        }
    }

    private void verifyFileContent(InputStream ins) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        StreamUtils.copyStream(ins, baos);
        Assert.assertEquals("The quick brown fox jumps over the lazy dog.", new String(baos.toByteArray()));
    }

    private void assumeIPFSAvailable(CamelContext camelctx) {
        IPFSEndpoint ipfsEp = camelctx.getEndpoints().stream()
                .filter(ep -> ep instanceof IPFSEndpoint)
                .map(ep -> (IPFSEndpoint)ep)
                .findFirst().get();
        IPFSClient ipfsClient = ipfsEp.getIPFSClient();
        Assume.assumeTrue(ipfsClient.hasConnection());
    }
}
