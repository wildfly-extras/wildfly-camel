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
package org.wildfly.camel.test.azure;

import java.util.Iterator;

import javax.naming.Context;

import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.extension.camel.CamelAware;
import org.wildfly.extension.camel.WildFlyCamelContext;

import com.microsoft.azure.storage.StorageCredentials;
import com.microsoft.azure.storage.StorageCredentialsAccountAndKey;
import com.microsoft.azure.storage.blob.CloudAppendBlob;
import com.microsoft.azure.storage.blob.CloudBlob;

@CamelAware
@RunWith(Arquillian.class)
public class AzureIntegrationTest {

    private static final String AZURE_STORAGE_BLOB = "AZURE_STORAGE_BLOB";

    @Deployment
    public static JavaArchive createDeployment() {
        return ShrinkWrap.create(JavaArchive.class, "camel-azure-tests.jar");
    }

    @Test
    public void testAppendBlob() throws Exception {

        StorageCredentials creds = getStorageCredentials("camelblob", System.getenv(AZURE_STORAGE_BLOB));
        Assume.assumeNotNull("Credentials not null", creds);
        
        CamelContext camelctx = createCamelContext(creds);
        camelctx.addRoutes(new RouteBuilder() {
            public void configure() throws Exception {
                from("direct:start")
                .to("azure-blob://camelblob/container1/blobAppend?credentials=#creds&operation=updateAppendBlob");
                
                from("azure-blob://camelblob/container1/blobAppend?credentials=#creds&blobType=appendblob")
                .to("mock:read");
                
                from("direct:list")
                .to("azure-blob://camelblob/container1?credentials=#creds&operation=listBlobs");
            }
        });

        camelctx.start();
        try {
            MockEndpoint mockRead = camelctx.getEndpoint("mock:read", MockEndpoint.class);
            mockRead.expectedBodiesReceived("Append Blob");
            mockRead.expectedMessageCount(1);
            
            ProducerTemplate producer = camelctx.createProducerTemplate();
            
            Iterator<?> it = producer.requestBody("direct:list", null, Iterable.class).iterator();
            Assert.assertFalse("No Blob exists", it.hasNext());

            // append to blob
            producer.sendBody("direct:start", "Append Blob");
            mockRead.assertIsSatisfied();

            it = producer.requestBody("direct:list", null, Iterable.class).iterator();
            Assert.assertTrue("Blob exists", it.hasNext());
            CloudBlob blob = (CloudAppendBlob) it.next();
            blob.delete();
            
            it = producer.requestBody("direct:list", null, Iterable.class).iterator();
            Assert.assertFalse("No Blob exists", it.hasNext());

        } finally {
            camelctx.stop();
        }
    }

    private StorageCredentials getStorageCredentials(String account, String key) {
        return key != null ? new StorageCredentialsAccountAndKey(account, key) : null;
    }

    private CamelContext createCamelContext(StorageCredentials creds) throws Exception {
        WildFlyCamelContext camelctx = new WildFlyCamelContext();
        Context jndictx = camelctx.getNamingContext();
        jndictx.rebind("creds", creds);
        return camelctx;
    }
}