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

import java.util.Hashtable;
import java.util.Iterator;
import java.util.Properties;

import javax.naming.Context;
import javax.naming.InitialContext;

import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.impl.JndiRegistry;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.extension.camel.CamelAware;

import com.microsoft.azure.storage.StorageCredentials;
import com.microsoft.azure.storage.StorageCredentialsAccountAndKey;
import com.microsoft.azure.storage.blob.CloudAppendBlob;
import com.microsoft.azure.storage.blob.CloudBlob;

@CamelAware
@RunWith(Arquillian.class)
public class AzureBlobIntegrationTest {

    private static final String AZURE_STORAGE_KEY = "AZURE_STORAGE_KEY";

    @Deployment
    public static JavaArchive createDeployment() {
        return ShrinkWrap.create(JavaArchive.class, "camel-azure-tests.jar");
    }

    @Test
    public void testGetAppendBlob() throws Exception {

        StorageCredentials credentials = getStorageCredentials();
        Assume.assumeNotNull("Credentials not null", credentials);
        
        CamelContext camelctx = createCamelContext(credentials);
        camelctx.addRoutes(new RouteBuilder() {
            public void configure() throws Exception {
                from("direct:start")
                .to("azure-blob://camelazure/container1/blobAppend?credentials=#creds&operation=updateAppendBlob");
                
                from("azure-blob://camelazure/container1/blobAppend?credentials=#creds&blobType=appendblob")
                .to("mock:read");
                
                from("direct:list")
                .to("azure-blob://camelazure/container1?credentials=#creds&operation=listBlobs");
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

    private StorageCredentials getStorageCredentials() {
        String key = System.getenv(AZURE_STORAGE_KEY);
        return key != null ? new StorageCredentialsAccountAndKey("camelazure", key) : null;
    }

    private CamelContext createCamelContext(StorageCredentials credentials) throws Exception {
        JndiRegistry registry = new JndiRegistry(createJndiContext());
        registry.bind("creds", credentials);
        return new DefaultCamelContext(registry);
    }

    private Context createJndiContext() throws Exception {
        Properties properties = new Properties();
        properties.put("java.naming.factory.initial", "org.apache.camel.util.jndi.CamelInitialContextFactory");
        return new InitialContext(new Hashtable<Object, Object>(properties));
    }
}