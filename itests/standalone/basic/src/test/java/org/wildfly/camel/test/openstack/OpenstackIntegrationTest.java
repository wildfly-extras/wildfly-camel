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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.Producer;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.openstack.common.OpenstackConstants;
import org.apache.camel.component.openstack.swift.SwiftComponent;
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
import org.mockito.Mockito;
import org.objenesis.Objenesis;
import org.openstack4j.api.OSClient;
import org.openstack4j.api.storage.ObjectStorageContainerService;
import org.openstack4j.api.storage.ObjectStorageObjectService;
import org.openstack4j.api.storage.ObjectStorageService;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.storage.object.options.CreateUpdateContainerOptions;
import org.wildfly.extension.camel.CamelAware;

@CamelAware
@RunWith(Arquillian.class)
public class OpenstackIntegrationTest {

    private static final String CONTAINER_NAME = "containerName";

    private ObjectStorageService objectStorageService = Mockito.mock(ObjectStorageService.class);
    private ObjectStorageContainerService containerService = Mockito.mock(ObjectStorageContainerService.class);
    private ObjectStorageObjectService objectService = Mockito.mock(ObjectStorageObjectService.class);
    private OSClient.OSClientV3 client = Mockito.mock(OSClient.OSClientV3.class);

    @Deployment
    public static JavaArchive createDeployment() {
        JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "camel-openstack-tests.jar");
        archive.addPackages(true, Mockito.class.getPackage(), Objenesis.class.getPackage());
        return archive;
    }

    @Before
    public void before() throws IOException {
        when(client.objectStorage()).thenReturn(objectStorageService);
        when(objectStorageService.containers()).thenReturn(containerService);
        when(objectStorageService.objects()).thenReturn(objectService);
    }

    @Test
    public void createTestWithoutOptionsMock() throws Exception {

        SwiftEndpoint endpoint = Mockito.mock(SwiftEndpoint.class);
        Exchange exchange = Mockito.mock(Exchange.class);
        CamelContext camelContext = Mockito.mock(CamelContext.class);

        Message msg = new DefaultMessage(camelContext);
        when(exchange.getIn()).thenReturn(msg);
        when(camelContext.getHeadersMapFactory()).thenReturn(new DefaultHeadersMapFactory());

        when(containerService.create(anyString(), any(CreateUpdateContainerOptions.class))).thenReturn(ActionResponse.actionSuccess());

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
    public void createTestWithoutOptions() throws Exception {

        CamelContext camelctx = new DefaultCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            public void configure() throws Exception {
                from("direct:start").to("openstack-swift:localhost");
            }
        });

        camelctx.getEndpoint("openstack-swift:localhost");
        SwiftComponent component = camelctx.getComponent("openstack-swift", SwiftComponent.class);
        Assert.assertNotNull("SwiftComponent not null", component);
    }
}
