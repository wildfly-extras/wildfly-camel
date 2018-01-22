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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.Producer;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.openstack.cinder.CinderEndpoint;
import org.apache.camel.component.openstack.cinder.producer.VolumeProducer;
import org.apache.camel.component.openstack.common.OpenstackConstants;
import org.apache.camel.component.openstack.glance.GlanceConstants;
import org.apache.camel.component.openstack.glance.GlanceEndpoint;
import org.apache.camel.component.openstack.glance.GlanceProducer;
import org.apache.camel.component.openstack.keystone.KeystoneConstants;
import org.apache.camel.component.openstack.keystone.KeystoneEndpoint;
import org.apache.camel.component.openstack.keystone.producer.ProjectProducer;
import org.apache.camel.component.openstack.neutron.NeutronConstants;
import org.apache.camel.component.openstack.neutron.NeutronEndpoint;
import org.apache.camel.component.openstack.neutron.producer.NetworkProducer;
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
import org.openstack4j.api.Builders;
import org.openstack4j.api.OSClient;
import org.openstack4j.api.compute.ComputeService;
import org.openstack4j.api.compute.FlavorService;
import org.openstack4j.api.compute.KeypairService;
import org.openstack4j.api.compute.ServerService;
import org.openstack4j.api.identity.v3.DomainService;
import org.openstack4j.api.identity.v3.GroupService;
import org.openstack4j.api.identity.v3.IdentityService;
import org.openstack4j.api.identity.v3.ProjectService;
import org.openstack4j.api.identity.v3.RegionService;
import org.openstack4j.api.identity.v3.UserService;
import org.openstack4j.api.image.ImageService;
import org.openstack4j.api.networking.NetworkService;
import org.openstack4j.api.networking.NetworkingService;
import org.openstack4j.api.networking.PortService;
import org.openstack4j.api.networking.RouterService;
import org.openstack4j.api.networking.SubnetService;
import org.openstack4j.api.storage.BlockStorageService;
import org.openstack4j.api.storage.BlockVolumeService;
import org.openstack4j.api.storage.BlockVolumeSnapshotService;
import org.openstack4j.api.storage.ObjectStorageContainerService;
import org.openstack4j.api.storage.ObjectStorageObjectService;
import org.openstack4j.api.storage.ObjectStorageService;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.common.Payload;
import org.openstack4j.model.compute.Keypair;
import org.openstack4j.model.identity.v3.Project;
import org.openstack4j.model.image.ContainerFormat;
import org.openstack4j.model.image.DiskFormat;
import org.openstack4j.model.image.Image;
import org.openstack4j.model.network.Network;
import org.openstack4j.model.network.NetworkType;
import org.openstack4j.model.storage.block.Volume;
import org.openstack4j.model.storage.block.builder.VolumeBuilder;
import org.openstack4j.model.storage.object.options.CreateUpdateContainerOptions;
import org.openstack4j.openstack.compute.domain.NovaKeypair;
import org.openstack4j.openstack.image.domain.GlanceImage;
import org.wildfly.extension.camel.CamelAware;

@CamelAware
@RunWith(Arquillian.class)
public class OpenstackIntegrationTest {

    private static final String CONTAINER_NAME = "containerName";
    private static final String KEYPAIR_NAME = "keypairName";

    // swift
    ObjectStorageService objectStorageService = Mockito.mock(ObjectStorageService.class);
    ObjectStorageContainerService containerService = Mockito.mock(ObjectStorageContainerService.class);
    ObjectStorageObjectService objectService = Mockito.mock(ObjectStorageObjectService.class);

    // nova
    ComputeService computeService = Mockito.mock(ComputeService.class);
    FlavorService flavorService = Mockito.mock(FlavorService.class);
    ServerService serverService = Mockito.mock(ServerService.class);
    KeypairService keypairService = Mockito.mock(KeypairService.class);
    Keypair osTestKeypair = Mockito.mock(Keypair.class);
    Keypair dummyKeypair = createKeypair();

    // neutron
    NetworkingService networkingService = Mockito.mock(NetworkingService.class);
    PortService portService = Mockito.mock(PortService.class);
    RouterService routerService = Mockito.mock(RouterService.class);
    SubnetService subnetService = Mockito.mock(SubnetService.class);
    NetworkService networkService = Mockito.mock(NetworkService.class);
    Network testOSnetwork = Mockito.mock(Network.class);
    Network dummyNetwork = createNetwork();

    // keystone
    IdentityService identityService = Mockito.mock(IdentityService.class);
    DomainService domainService = Mockito.mock(DomainService.class);
    GroupService groupService = Mockito.mock(GroupService.class);
    ProjectService projectService = Mockito.mock(ProjectService.class);
    RegionService regionService = Mockito.mock(RegionService.class);
    UserService userService = Mockito.mock(UserService.class);
    Project testOSproject = Mockito.mock(Project.class);
    Project dummyProject = createProject();

    // glance
    ImageService imageService = Mockito.mock(ImageService.class);
    Image dummyImage = createImage();
    Image osImage = spy(Builders.image().build());

    //cinder
    BlockStorageService blockStorageService = Mockito.mock(BlockStorageService.class);
    BlockVolumeService volumeService = Mockito.mock(BlockVolumeService.class);
    BlockVolumeSnapshotService snapshotService = Mockito.mock(BlockVolumeSnapshotService.class);
    Volume testOSVolume = Mockito.mock(Volume.class);
    Volume dummyVolume = createTestVolume();

    OSClient.OSClientV3 client = Mockito.mock(OSClient.OSClientV3.class);

    @Deployment
    public static JavaArchive createDeployment() {
        JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "camel-openstack-tests.jar");
        archive.addPackages(true, Mockito.class.getPackage(), Objenesis.class.getPackage());
        return archive;
    }

    @Before
    public void before() throws IOException {

        // swift
        when(objectStorageService.containers()).thenReturn(containerService);
        when(objectStorageService.objects()).thenReturn(objectService);
        when(client.objectStorage()).thenReturn(objectStorageService);

        // nova
        when(computeService.flavors()).thenReturn(flavorService);
        when(computeService.servers()).thenReturn(serverService);
        when(computeService.keypairs()).thenReturn(keypairService);
        when(client.compute()).thenReturn(computeService);

        when(keypairService.get(Matchers.anyString())).thenReturn(osTestKeypair);
        when(keypairService.create(Matchers.anyString(), Matchers.anyString())).thenReturn(osTestKeypair);

        List<Keypair> keypairList = new ArrayList<>();
        keypairList.add(osTestKeypair);
        keypairList.add(osTestKeypair);
        doReturn(keypairList).when(keypairService).list();

        when(osTestKeypair.getName()).thenReturn(dummyKeypair.getName());
        when(osTestKeypair.getPublicKey()).thenReturn(dummyKeypair.getPublicKey());

        // neutron
        when(networkingService.port()).thenReturn(portService);
        when(networkingService.router()).thenReturn(routerService);
        when(networkingService.subnet()).thenReturn(subnetService);
        when(networkingService.network()).thenReturn(networkService);
        when(client.networking()).thenReturn(networkingService);

        when(networkService.create(any(Network.class))).thenReturn(testOSnetwork);
        when(networkService.get(anyString())).thenReturn(testOSnetwork);

        List<Network> networkList = new ArrayList<>();
        networkList.add(testOSnetwork);
        networkList.add(testOSnetwork);
        doReturn(networkList).when(networkService).list();

        when(testOSnetwork.getName()).thenReturn(dummyNetwork.getName());
        when(testOSnetwork.getTenantId()).thenReturn(dummyNetwork.getTenantId());
        when(testOSnetwork.getNetworkType()).thenReturn(dummyNetwork.getNetworkType());
        when(testOSnetwork.getId()).thenReturn(UUID.randomUUID().toString());

        // keystone
        when(identityService.domains()).thenReturn(domainService);
        when(identityService.groups()).thenReturn(groupService);
        when(identityService.projects()).thenReturn(projectService);
        when(identityService.regions()).thenReturn(regionService);
        when(identityService.users()).thenReturn(userService);
        when(client.identity()).thenReturn(identityService);

        when(projectService.create(any(Project.class))).thenReturn(testOSproject);
        when(projectService.get(anyString())).thenReturn(testOSproject);

        List<Project> projectList = new ArrayList<>();
        projectList.add(testOSproject);
        projectList.add(testOSproject);
        doReturn(projectList).when(projectService).list();

        when(testOSproject.getName()).thenReturn(dummyProject.getName());
        when(testOSproject.getDescription()).thenReturn(dummyProject.getDescription());

        // glance
        when(imageService.get(anyString())).thenReturn(osImage);
        when(imageService.create(any(org.openstack4j.model.image.Image.class), any(Payload.class))).thenReturn(osImage);
        when(imageService.reserve(any(org.openstack4j.model.image.Image.class))).thenReturn(osImage);
        when(imageService.upload(anyString(), any(Payload.class), any(GlanceImage.class))).thenReturn(osImage);
        when(client.images()).thenReturn(imageService);

        when(osImage.getContainerFormat()).thenReturn(ContainerFormat.BARE);
        when(osImage.getDiskFormat()).thenReturn(DiskFormat.ISO);
        when(osImage.getName()).thenReturn(dummyImage.getName());
        when(osImage.getChecksum()).thenReturn(dummyImage.getChecksum());
        when(osImage.getMinDisk()).thenReturn(dummyImage.getMinDisk());
        when(osImage.getMinRam()).thenReturn(dummyImage.getMinRam());
        when(osImage.getOwner()).thenReturn(dummyImage.getOwner());
        when(osImage.getId()).thenReturn(UUID.randomUUID().toString());

        // cinder
        when(blockStorageService.volumes()).thenReturn(volumeService);
        when(blockStorageService.snapshots()).thenReturn(snapshotService);
        when(client.blockStorage()).thenReturn(blockStorageService);

        when(volumeService.create(Matchers.any(org.openstack4j.model.storage.block.Volume.class))).thenReturn(testOSVolume);
        when(volumeService.get(Matchers.anyString())).thenReturn(testOSVolume);

        when(testOSVolume.getId()).thenReturn(UUID.randomUUID().toString());
        when(testOSVolume.getName()).thenReturn(dummyVolume.getName());
        when(testOSVolume.getDescription()).thenReturn(dummyVolume.getDescription());
        when(testOSVolume.getImageRef()).thenReturn(dummyVolume.getImageRef());
        when(testOSVolume.getSize()).thenReturn(dummyVolume.getSize());
        when(testOSVolume.getVolumeType()).thenReturn(dummyVolume.getVolumeType());
    }

    @Test
    public void createSwiftContainer() throws Exception {

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
        when(osTestKeypair.getName()).thenReturn(KEYPAIR_NAME);
        when(osTestKeypair.getPublicKey()).thenReturn(dummyKeypair.getPublicKey());
        when(osTestKeypair.getFingerprint()).thenReturn("fp");
        when(osTestKeypair.getPrivateKey()).thenReturn("prk");

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
        assertEquals("fp", result.getFingerprint());
        assertEquals("prk", result.getPrivateKey());
        assertEquals(dummyKeypair.getName(), result.getName());
        assertEquals(dummyKeypair.getPublicKey(), result.getPublicKey());
    }

    @Test
    public void createNeutronNetwork() throws Exception {
        CamelContext camelContext = Mockito.mock(CamelContext.class);
        when(camelContext.getHeadersMapFactory()).thenReturn(new DefaultHeadersMapFactory());

        Message msg = new DefaultMessage(camelContext);
        Exchange exchange = Mockito.mock(Exchange.class);
        when(exchange.getIn()).thenReturn(msg);

        msg.setHeader(OpenstackConstants.OPERATION, OpenstackConstants.CREATE);
        msg.setHeader(OpenstackConstants.NAME, dummyNetwork.getName());
        msg.setHeader(NeutronConstants.NETWORK_TYPE, dummyNetwork.getNetworkType());
        msg.setHeader(NeutronConstants.TENANT_ID, dummyNetwork.getTenantId());

        NeutronEndpoint endpoint = Mockito.mock(NeutronEndpoint.class);
        Producer producer = new NetworkProducer(endpoint, client);
        producer.process(exchange);

        ArgumentCaptor<Network> captor = ArgumentCaptor.forClass(Network.class);
        verify(networkService).create(captor.capture());

        assertEqualsNetwork(dummyNetwork, captor.getValue());
        assertNotNull(msg.getBody(Network.class).getId());
    }

    @Test
    public void createKeystoneProject() throws Exception {
        CamelContext camelContext = Mockito.mock(CamelContext.class);
        when(camelContext.getHeadersMapFactory()).thenReturn(new DefaultHeadersMapFactory());

        Message msg = new DefaultMessage(camelContext);
        Exchange exchange = Mockito.mock(Exchange.class);
        when(exchange.getIn()).thenReturn(msg);

        msg.setHeader(OpenstackConstants.OPERATION, OpenstackConstants.CREATE);
        msg.setHeader(OpenstackConstants.NAME, dummyProject.getName());
        msg.setHeader(KeystoneConstants.DESCRIPTION, dummyProject.getDescription());
        msg.setHeader(KeystoneConstants.DOMAIN_ID, dummyProject.getDomainId());
        msg.setHeader(KeystoneConstants.PARENT_ID, dummyProject.getParentId());

        KeystoneEndpoint endpoint = Mockito.mock(KeystoneEndpoint.class);
        Producer producer = new ProjectProducer(endpoint, client);
        producer.process(exchange);

        ArgumentCaptor<Project> captor = ArgumentCaptor.forClass(Project.class);
        verify(projectService).create(captor.capture());

        assertEqualsProject(dummyProject, captor.getValue());
    }

    @Test
    public void reserveGlanceImage() throws Exception {
        CamelContext camelContext = Mockito.mock(CamelContext.class);
        when(camelContext.getHeadersMapFactory()).thenReturn(new DefaultHeadersMapFactory());

        GlanceEndpoint endpoint = Mockito.mock(GlanceEndpoint.class);
        when(endpoint.getOperation()).thenReturn(GlanceConstants.RESERVE);

        Message msg = new DefaultMessage(camelContext);
        msg.setBody(dummyImage);

        Exchange exchange = Mockito.mock(Exchange.class);
        when(exchange.getIn()).thenReturn(msg);

        Producer producer = new GlanceProducer(endpoint, client);
        producer.process(exchange);
        ArgumentCaptor<Image> captor = ArgumentCaptor.forClass(Image.class);
        verify(imageService).reserve(captor.capture());
        assertEquals(dummyImage, captor.getValue());

        Image result = msg.getBody(Image.class);
        assertNotNull(result.getId());
        assertEqualsImages(dummyImage, result);
    }

    @Test
    public void createCinderVolume() throws Exception {
        CamelContext camelContext = Mockito.mock(CamelContext.class);
        when(camelContext.getHeadersMapFactory()).thenReturn(new DefaultHeadersMapFactory());

        Message msg = new DefaultMessage(camelContext);
        Exchange exchange = Mockito.mock(Exchange.class);
        when(exchange.getIn()).thenReturn(msg);

        CinderEndpoint endpoint = Mockito.mock(CinderEndpoint.class);
        when(endpoint.getOperation()).thenReturn(OpenstackConstants.CREATE);
        msg.setBody(dummyVolume);

        Producer producer = new VolumeProducer(endpoint, client);
        producer.process(exchange);
        assertEqualVolumes(dummyVolume, msg.getBody(Volume.class));
    }

    @Test
    public void testEndpoints() throws Exception {

        CamelContext camelctx = new DefaultCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            public void configure() throws Exception {
                from("direct:start").to("openstack-swift:localhost");
                from("direct:start").to("openstack-nova:localhost");
                from("direct:start").to("openstack-neutron:localhost");
                from("direct:start").to("openstack-keystone:localhost");
                from("direct:start").to("openstack-glance:localhost");
                from("direct:start").to("openstack-cinder:localhost");
            }
        });

        SwiftEndpoint swiftEndpoint = camelctx.getEndpoint("openstack-swift:localhost", SwiftEndpoint.class);
        Assert.assertNotNull("SwiftEndpoint not null", swiftEndpoint);

        NovaEndpoint novaEndpoint = camelctx.getEndpoint("openstack-nova:localhost", NovaEndpoint.class);
        Assert.assertNotNull("NovaEndpoint not null", novaEndpoint);

        NeutronEndpoint neutronEndpoint = camelctx.getEndpoint("openstack-neutron:localhost", NeutronEndpoint.class);
        Assert.assertNotNull("NeutronEndpoint not null", neutronEndpoint);

        KeystoneEndpoint keystoneEndpoint = camelctx.getEndpoint("openstack-keystone:localhost", KeystoneEndpoint.class);
        Assert.assertNotNull("KeystoneEndpoint not null", keystoneEndpoint);

        GlanceEndpoint glanceEndpoint = camelctx.getEndpoint("openstack-glance:localhost", GlanceEndpoint.class);
        Assert.assertNotNull("GlanceEndpoint not null", glanceEndpoint);

        CinderEndpoint cinderEndpoint = camelctx.getEndpoint("openstack-cinder:localhost", CinderEndpoint.class);
        Assert.assertNotNull("cinderEndpoint not null", cinderEndpoint);
    }

    private NovaKeypair createKeypair() {
        return NovaKeypair.create(KEYPAIR_NAME, "string contains private key");
    }

    private Network createNetwork() {
        return Builders.network().name("name").tenantId("tenantID").networkType(NetworkType.LOCAL).build();
    }

    private Project createProject() {
        return Builders.project().domainId("domain").description("desc").name("project Name").parentId("parent").build();
    }

    private Image createImage() {
        return Builders.image().name("Image Name").diskFormat(DiskFormat.ISO).containerFormat(ContainerFormat.BARE).checksum("checksum").minDisk(10L).minRam(5L)
                .owner("owner").build();
    }

    private Volume createTestVolume() {
        VolumeBuilder builder = Builders.volume().name("name").description("description").imageRef("ref").size(20).volumeType("type");
        return builder.build();
    }

    private void assertEqualsNetwork(Network old, Network newNetwork) {
        assertEquals(old.getName(), newNetwork.getName());
        assertEquals(old.getTenantId(), newNetwork.getTenantId());
        assertEquals(old.getNetworkType(), newNetwork.getNetworkType());
    }

    private void assertEqualsProject(Project old, Project newProject) {
        assertEquals(old.getName(), newProject.getName());
        assertEquals(old.getDescription(), newProject.getDescription());
        assertEquals(old.getDomainId(), newProject.getDomainId());
    }

    private void assertEqualsImages(Image original, Image newImage) {
        assertEquals(original.getContainerFormat(), newImage.getContainerFormat());
        assertEquals(original.getDiskFormat(), newImage.getDiskFormat());
        assertEquals(original.getChecksum(), newImage.getChecksum());
        assertEquals(original.getMinDisk(), newImage.getMinDisk());
        assertEquals(original.getMinRam(), newImage.getMinRam());
        assertEquals(original.getOwner(), newImage.getOwner());
        assertEquals(original.getName(), newImage.getName());
    }

    private void assertEqualVolumes(Volume old, Volume newVolume) {
        assertEquals(old.getName(), newVolume.getName());
        assertEquals(old.getDescription(), newVolume.getDescription());
        assertEquals(old.getImageRef(), newVolume.getImageRef());
        assertEquals(old.getSize(), newVolume.getSize());
        assertEquals(old.getVolumeType(), newVolume.getVolumeType());
        assertNotNull(newVolume.getId());
    }
}
