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
package org.wildfly.camel.test.openstack;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.Producer;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.openstack.common.OpenstackConstants;
import org.apache.camel.component.openstack.nova.NovaEndpoint;
import org.apache.camel.component.openstack.nova.producer.KeypairProducer;
import org.apache.camel.component.openstack.swift.SwiftConstants;
import org.apache.camel.component.openstack.swift.SwiftEndpoint;
import org.apache.camel.component.openstack.swift.producer.ContainerProducer;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.impl.DefaultHeadersMapFactory;
import org.apache.camel.impl.DefaultMessage;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Matchers;
import org.mockito.Mockito;
import org.objenesis.Objenesis;
import org.openstack4j.api.OSClient;
import org.openstack4j.api.compute.ComputeService;
import org.openstack4j.api.compute.FlavorService;
import org.openstack4j.api.compute.KeypairService;
import org.openstack4j.api.compute.ServerService;
import org.openstack4j.api.storage.ObjectStorageContainerService;
import org.openstack4j.api.storage.ObjectStorageObjectService;
import org.openstack4j.api.storage.ObjectStorageService;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.compute.Keypair;
import org.openstack4j.model.storage.object.options.CreateUpdateContainerOptions;
import org.openstack4j.openstack.compute.domain.NovaKeypair;
import org.wildfly.extension.camel.CamelAware;

@CamelAware
@RunWith(Arquillian.class)
public class OpenstackIntegrationTest {

    private static final String CONTAINER_NAME = "containerName";
    private static final String KEYPAIR_NAME = "keypairName";

    ObjectStorageService objectStorageService = Mockito.mock(ObjectStorageService.class);
    ObjectStorageContainerService containerService = Mockito.mock(ObjectStorageContainerService.class);
    ObjectStorageObjectService objectService = Mockito.mock(ObjectStorageObjectService.class);
    
    ComputeService computeService = Mockito.mock(ComputeService.class);
    FlavorService flavorService  = Mockito.mock(FlavorService.class);
    ServerService serverService = Mockito.mock(ServerService.class);
    KeypairService keypairService = Mockito.mock(KeypairService.class);
    Keypair osTestKeypair = Mockito.mock(Keypair.class);
    Keypair dummyKeypair = NovaKeypair.create(KEYPAIR_NAME, "string contains private key");

    OSClient.OSClientV3 client = Mockito.mock(OSClient.OSClientV3.class);

    @Deployment
    public static JavaArchive createDeployment() {
        JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "camel-openstack-tests.jar");
        archive.addPackages(true, Mockito.class.getPackage(), Objenesis.class.getPackage());
        return archive;
    }

    @Before
    public void before() throws IOException {
        when(client.objectStorage()).thenReturn(objectStorageService);
        when(client.compute()).thenReturn(computeService);
        
        when(objectStorageService.containers()).thenReturn(containerService);
        when(objectStorageService.objects()).thenReturn(objectService);

        when(computeService.flavors()).thenReturn(flavorService);
        when(computeService.servers()).thenReturn(serverService);
        when(computeService.keypairs()).thenReturn(keypairService);
        
        when(keypairService.get(Matchers.anyString())).thenReturn(osTestKeypair);
        when(keypairService.create(Matchers.anyString(), Matchers.anyString())).thenReturn(osTestKeypair);

        List<org.openstack4j.model.compute.Keypair> getAllList = new ArrayList<>();
        getAllList.add(osTestKeypair);
        getAllList.add(osTestKeypair);
        doReturn(getAllList).when(keypairService).list();

        when(osTestKeypair.getName()).thenReturn(dummyKeypair.getName());
        when(osTestKeypair.getPublicKey()).thenReturn(dummyKeypair.getPublicKey());
    }

    @Test
    public void createTestWithoutOptionsMock() throws Exception {

        CamelContext camelContext = Mockito.mock(CamelContext.class);
        when(camelContext.getHeadersMapFactory()).thenReturn(new DefaultHeadersMapFactory());

        Message msg = new DefaultMessage(camelContext);
        Exchange exchange = Mockito.mock(Exchange.class);
        when(exchange.getIn()).thenReturn(msg);

        when(containerService.create(anyString(), any(CreateUpdateContainerOptions.class))).thenReturn(ActionResponse.actionSuccess());

        SwiftEndpoint endpoint = Mockito.mock(SwiftEndpoint.class);
        Producer producer = new ContainerProducer(endpoint, client);
        msg.setHeader(OpenstackConstants.OPERATION, OpenstackConstants.CREATE);
        msg.setHeader(SwiftConstants.CONTAINER_NAME, CONTAINER_NAME);

        producer.process(exchange);

        ArgumentCaptor<String> containerNameCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<CreateUpdateContainerOptions> optionsCaptor = ArgumentCaptor.forClass(CreateUpdateContainerOptions.class);

        verify(containerService).create(containerNameCaptor.capture(), optionsCaptor.capture());
        assertEquals(CONTAINER_NAME, containerNameCaptor.getValue());
        assertNull(optionsCaptor.getValue());

        assertFalse(msg.isFault());
    }

    @Test
    public void testNovaKeypair() throws Exception {
        final String fingerPrint = "fp";
        final String privatecKey = "prk";
        when(osTestKeypair.getName()).thenReturn(KEYPAIR_NAME);
        when(osTestKeypair.getPublicKey()).thenReturn(dummyKeypair.getPublicKey());
        when(osTestKeypair.getFingerprint()).thenReturn(fingerPrint);
        when(osTestKeypair.getPrivateKey()).thenReturn(privatecKey);

        CamelContext camelContext = Mockito.mock(CamelContext.class);
        when(camelContext.getHeadersMapFactory()).thenReturn(new DefaultHeadersMapFactory());
        
        Message msg = new DefaultMessage(camelContext);
        msg.setHeader(OpenstackConstants.OPERATION, OpenstackConstants.CREATE);
        msg.setHeader(OpenstackConstants.NAME, KEYPAIR_NAME);

        Exchange exchange = Mockito.mock(Exchange.class);
        when(exchange.getIn()).thenReturn(msg);
        
        NovaEndpoint endpoint = Mockito.mock(NovaEndpoint.class);
        Producer producer = new KeypairProducer(endpoint, client);
        producer.process(exchange);

        ArgumentCaptor<String> nameCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> keypairCaptor = ArgumentCaptor.forClass(String.class);
        verify(keypairService).create(nameCaptor.capture(), keypairCaptor.capture());

        assertEquals(KEYPAIR_NAME, nameCaptor.getValue());
        assertNull(keypairCaptor.getValue());

        Keypair result = msg.getBody(Keypair.class);
        assertEquals(fingerPrint, result.getFingerprint());
        assertEquals(privatecKey, result.getPrivateKey());
        assertEquals(dummyKeypair.getName(), result.getName());
        assertEquals(dummyKeypair.getPublicKey(), result.getPublicKey());
    }

    @Test
    public void testEndpoints() throws Exception {

        CamelContext camelctx = new DefaultCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            public void configure() throws Exception {
                from("direct:start").to("openstack-swift:localhost");
                from("direct:start").to("openstack-nova:localhost");
            }
        });

        SwiftEndpoint swiftEndpoint = camelctx.getEndpoint("openstack-swift:localhost", SwiftEndpoint.class);
        Assert.assertNotNull("SwiftEndpoint not null", swiftEndpoint);

        NovaEndpoint novaEndpoint = camelctx.getEndpoint("openstack-nova:localhost", NovaEndpoint.class);
        Assert.assertNotNull("SwiftEndpoint not null", novaEndpoint);
    }
}
