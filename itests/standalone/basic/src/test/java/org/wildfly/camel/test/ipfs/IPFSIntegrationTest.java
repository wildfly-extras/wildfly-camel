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
import java.util.Arrays;
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

import io.nessus.utils.StreamUtils;

@CamelAware
@RunWith(Arquillian.class)
public class IPFSIntegrationTest {

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
                from("direct:startB").to("ipfs:127.0.0.1/version");
                from("direct:startC").to("ipfs:127.0.0.1:5001/version");
            }
        });

        camelctx.start();
        try {
            assumeIPFSAvailable(camelctx);

            ProducerTemplate producer = camelctx.createProducerTemplate();
            String resA = producer.requestBody("direct:startA", null, String.class);
            String resB = producer.requestBody("direct:startB", null, String.class);
            String resC = producer.requestBody("direct:startC", null, String.class);
            Arrays.asList(resA, resB, resC).forEach(res -> {
                Assert.assertTrue("Expecting 0.4 in: " + resA, resA.startsWith("0.4"));
            });
        } finally {
            camelctx.close();
        }
    }

    @Test
    public void ipfsAddSingle() throws Exception {

        String hash = "QmYgjSRbXFPdPYKqQSnUjmXLYLudVahEJQotMaAJKt6Lbd";

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

            Path path = Paths.get("src/test/resources/ipfs/index.html");
            ProducerTemplate producer = camelctx.createProducerTemplate();
            String res = producer.requestBody("direct:start", path, String.class);
            Assert.assertEquals(hash, res);
        } finally {
            camelctx.close();
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    public void ipfsAddRecursive() throws Exception {

        String hash = "QmdqofqAqRxeMWziEHmvUJroAKvmApofXpH4RTyKs4ojEw";

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
            Assert.assertEquals(hash, res.get(9));
        } finally {
            camelctx.close();
        }
    }

    @Test
    public void ipfsCat() throws Exception {

        String hash = "QmUD7uG5prAMHbcCfp4x1G1mMSpywcSMHTGpq62sbpDAg6";

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
            InputStream res = producer.requestBody("direct:start", hash, InputStream.class);
            verifyFileContent(res);
        } finally {
            camelctx.close();
        }
    }

    @Test
    public void ipfsGetSingle() throws Exception {

        String hash = "QmUD7uG5prAMHbcCfp4x1G1mMSpywcSMHTGpq62sbpDAg6";

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
            Path res = producer.requestBody("direct:start", hash, Path.class);
            Assert.assertEquals(Paths.get("target", hash), res);
            verifyFileContent(new FileInputStream(res.toFile()));
        } finally {
            camelctx.close();
        }
    }

    @Test
    public void ipfsGetRecursive() throws Exception {

        String hash = "Qme6hd6tYXTFb7bb7L3JZ5U6ygktpAHKxbaeffYyQN85mW";

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
            Path res = producer.requestBody("direct:start", hash, Path.class);
            Assert.assertEquals(Paths.get("target", hash), res);
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
        Assume.assumeTrue(ipfsEp.getIPFSClient().hasConnection());
    }
}
