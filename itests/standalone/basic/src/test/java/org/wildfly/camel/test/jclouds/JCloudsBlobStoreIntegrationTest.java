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
package org.wildfly.camel.test.jclouds;

import java.util.ArrayList;
import java.util.List;

import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.jclouds.JcloudsBlobStoreHelper;
import org.apache.camel.component.jclouds.JcloudsComponent;
import org.apache.camel.component.jclouds.JcloudsConstants;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.impl.DefaultCamelContext;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jclouds.ContextBuilder;
import org.jclouds.blobstore.BlobStore;
import org.jclouds.blobstore.BlobStoreContext;
import org.jclouds.io.payloads.StringPayload;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.extension.camel.CamelAware;

@CamelAware
@RunWith(Arquillian.class)
public class JCloudsBlobStoreIntegrationTest {

    private static String BLOB_NAME = "wfc-blob";
    private static String BLOB_NAME_WITH_DIR = "dir/blob";
    private static String CONTAINER_NAME = "wfc-container";
    private static String CONTAINER_NAME_WITH_DIR = CONTAINER_NAME + "-with-dir";

    @Deployment
    public static JavaArchive createDeployment() {
        return ShrinkWrap.create(JavaArchive.class, "camel-jclouds-blob-tests.jar");
    }

    @Test
    public void testBlobStoreConsumer() throws Exception {
        BlobStore blobStore = getBlobStore();

        CamelContext camelctx = new DefaultCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                fromF("jclouds:blobstore:transient?container=%s", CONTAINER_NAME)
                .convertBodyTo(String.class)
                .to("mock:result");
            }
        });

        List<BlobStore> blobStores = new ArrayList<>();
        blobStores.add(blobStore);

        JcloudsComponent jclouds = camelctx.getComponent("jclouds", JcloudsComponent.class);
        jclouds.setBlobStores(blobStores);

        MockEndpoint mockEndpoint = camelctx.getEndpoint("mock:result", MockEndpoint.class);
        mockEndpoint.expectedBodiesReceived("Hello Kermit");

        camelctx.start();
        try {
            JcloudsBlobStoreHelper.writeBlob(blobStore, CONTAINER_NAME, BLOB_NAME, new StringPayload("Hello Kermit"));

            mockEndpoint.assertIsSatisfied();
        } finally {
            camelctx.close();
        }
    }

    @Test
    public void testBlobStoreConsumerWithDirectory() throws Exception {
        BlobStore blobStore = getBlobStore();

        CamelContext camelctx = new DefaultCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                fromF("jclouds:blobstore:transient?container=%s&directory=dir", CONTAINER_NAME_WITH_DIR)
                .convertBodyTo(String.class)
                .to("mock:result");
            }
        });

        List<BlobStore> blobStores = new ArrayList<>();
        blobStores.add(blobStore);

        JcloudsComponent jclouds = camelctx.getComponent("jclouds", JcloudsComponent.class);
        jclouds.setBlobStores(blobStores);

        MockEndpoint mockEndpoint = camelctx.getEndpoint("mock:result", MockEndpoint.class);
        mockEndpoint.expectedBodiesReceived("Hello Kermit");

        camelctx.start();
        try {
            JcloudsBlobStoreHelper.writeBlob(blobStore, CONTAINER_NAME_WITH_DIR, BLOB_NAME_WITH_DIR, new StringPayload("Hello Kermit"));

            mockEndpoint.assertIsSatisfied();
        } finally {
            camelctx.close();
        }
    }

    @Test
    public void testBlobStoreProducer() throws Exception {
        BlobStore blobStore = getBlobStore();

        CamelContext camelctx = new DefaultCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:start")
                .setHeader(JcloudsConstants.BLOB_NAME, constant(BLOB_NAME_WITH_DIR))
                .setHeader(JcloudsConstants.CONTAINER_NAME, constant(CONTAINER_NAME_WITH_DIR))
                .to("jclouds:blobstore:transient");
            }
        });

        List<BlobStore> blobStores = new ArrayList<>();
        blobStores.add(blobStore);

        JcloudsComponent jclouds = camelctx.getComponent("jclouds", JcloudsComponent.class);
        jclouds.setBlobStores(blobStores);

        camelctx.start();
        try {
            ProducerTemplate template = camelctx.createProducerTemplate();
            String result = template.requestBody("direct:start", "Hello Kermit", String.class);
            Assert.assertEquals("Hello Kermit", result);
        } finally {
            camelctx.close();
        }

    }

    private BlobStore getBlobStore() {
        BlobStore blobStore = ContextBuilder.newBuilder("transient")
            .credentials("id", "credential")
            .buildView(BlobStoreContext.class)
            .getBlobStore();
        blobStore.createContainerInLocation(null, CONTAINER_NAME);
        blobStore.createContainerInLocation(null, CONTAINER_NAME_WITH_DIR);
        return blobStore;
    }
}
