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
package org.wildfly.camel.test.apns;

import com.notnoop.apns.APNS;
import com.notnoop.apns.ApnsService;
import com.notnoop.apns.EnhancedApnsNotification;
import com.notnoop.apns.utils.ApnsServerStub;

import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.apns.ApnsComponent;
import org.apache.camel.component.apns.factory.ApnsServiceFactory;
import org.apache.camel.component.apns.model.InactiveDevice;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.impl.DefaultCamelContext;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.camel.test.apns.subA.ApnsUtils;
import org.wildfly.camel.test.common.utils.AvailablePortFinder;
import org.wildfly.extension.camel.CamelAware;
import static org.apache.camel.component.apns.model.ApnsConstants.HEADER_TOKENS;

@CamelAware
@RunWith(Arquillian.class)
public class ApnsIntegrationTest {

    private static final String FAKE_TOKEN = "19308314834701ACD8313AEBD92AEFDE192120371FE13982392831701318B943";
    private ApnsServerStub server;

    @Deployment
    public static JavaArchive createDeployment() {
        return ShrinkWrap.create(JavaArchive.class, "camel-apns-tests.jar")
            .addClasses(AvailablePortFinder.class, ApnsUtils.class)
            // Add required apns test-jar packages for ApnsServerStub
            .addPackages(true, "com.notnoop.apns.internal", "com.notnoop.apns.utils", "com.notnoop.exceptions")
            .addAsResource("clientStore.p12", "clientStore.p12")
            .addAsResource("serverStore.p12", "serverStore.p12");
    }

    @Before
    public void setUp() {
        int feedbackPort = AvailablePortFinder.getNextAvailable();
        int gatewayPort = AvailablePortFinder.getNextAvailable();

        AvailablePortFinder.storeServerData("apns-feedback-port.txt", feedbackPort);
        AvailablePortFinder.storeServerData("apns-gateway-port.txt", gatewayPort);

        server = ApnsUtils.createServer(gatewayPort, feedbackPort);
        server.start();
    }

    @After
    public void tearDown() {
        if (server != null) {
            server.stop();
        }
    }

    @Test
    public void testApnsConsumer() throws Exception {
        CamelContext camelctx = new DefaultCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("apns:consumer?initialDelay=500&delay=500&timeUnit=MILLISECONDS")
                .to("mock:result");
            }
        });

        int gatewayPort = getPortNumber("apns-feedback-port.txt");
        int feedbackPort = getPortNumber("apns-gateway-port.txt");

        ApnsServiceFactory apnsServiceFactory = ApnsUtils.createDefaultTestConfiguration(camelctx);
        apnsServiceFactory.setFeedbackHost("localhost");
        apnsServiceFactory.setFeedbackPort(gatewayPort);
        apnsServiceFactory.setGatewayHost("localhost");
        apnsServiceFactory.setGatewayPort(feedbackPort);

        ApnsService apnsService = apnsServiceFactory.getApnsService();
        ApnsComponent apnsComponent = new ApnsComponent(apnsService);
        camelctx.addComponent("apns", apnsComponent);

        camelctx.start();
        try {
            byte[] deviceTokenBytes = ApnsUtils.createRandomDeviceTokenBytes();
            String deviceToken = ApnsUtils.encodeHexToken(deviceTokenBytes);

            MockEndpoint mockEndpoint = camelctx.getEndpoint("mock:result", MockEndpoint.class);
            mockEndpoint.expectedMessageCount(1);

            byte[] feedBackBytes = ApnsUtils.generateFeedbackBytes(deviceTokenBytes);
            server.getToSend().write(feedBackBytes);

            mockEndpoint.assertIsSatisfied(5000);

            InactiveDevice inactiveDevice = (InactiveDevice)mockEndpoint.getExchanges().get(0).getIn().getBody();
            Assert.assertNotNull(inactiveDevice);
            Assert.assertNotNull(inactiveDevice.getDate());
            Assert.assertNotNull(inactiveDevice.getDeviceToken());
            Assert.assertEquals(deviceToken, inactiveDevice.getDeviceToken());
        } finally {
            camelctx.stop();
        }
    }

    @Test
    public void testApnsProducer() throws Exception {
        CamelContext camelctx = new DefaultCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:start")
                .setHeader(HEADER_TOKENS, constant(FAKE_TOKEN))
                .to("apns:notify");
            }
        });

        int gatewayPort = getPortNumber("apns-feedback-port.txt");
        int feedbackPort = getPortNumber("apns-gateway-port.txt");

        ApnsServiceFactory apnsServiceFactory = ApnsUtils.createDefaultTestConfiguration(camelctx);
        apnsServiceFactory.setFeedbackHost("localhost");
        apnsServiceFactory.setFeedbackPort(gatewayPort);
        apnsServiceFactory.setGatewayHost("localhost");
        apnsServiceFactory.setGatewayPort(feedbackPort);

        ApnsService apnsService = apnsServiceFactory.getApnsService();
        ApnsComponent apnsComponent = new ApnsComponent(apnsService);
        camelctx.addComponent("apns", apnsComponent);

        camelctx.start();
        try {
            String message = "Hello World";
            String messagePayload = APNS.newPayload().alertBody(message).build();

            EnhancedApnsNotification apnsNotification = new EnhancedApnsNotification(1, EnhancedApnsNotification.MAXIMUM_EXPIRY, FAKE_TOKEN, messagePayload);
            server.stopAt(apnsNotification.length());

            ProducerTemplate template = camelctx.createProducerTemplate();
            template.sendBody("direct:start", message);

            server.getMessages().acquire();
            Assert.assertArrayEquals(apnsNotification.marshall(), server.getReceived().toByteArray());
        } finally {
            camelctx.stop();
        }
    }

    private Integer getPortNumber(String portFile) {
        return Integer.parseInt(AvailablePortFinder.readServerData(portFile));
    }
}
