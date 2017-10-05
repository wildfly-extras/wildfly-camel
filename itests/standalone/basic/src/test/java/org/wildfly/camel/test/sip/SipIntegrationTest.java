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
package org.wildfly.camel.test.sip;

import javax.sip.message.Request;

import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.impl.DefaultCamelContext;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.camel.test.common.utils.AvailablePortFinder;
import org.wildfly.extension.camel.CamelAware;

@CamelAware
@RunWith(Arquillian.class)
public class SipIntegrationTest {

    private int port1;
    private int port2;
    private int port3;

    @Deployment
    public static JavaArchive createdeployment() {
        JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "camel-sip-tests");
        archive.addClasses(AvailablePortFinder.class);
        return archive;
    }

    @Before
    public void setUp() throws Exception {
        port1 = AvailablePortFinder.getNextAvailable(17189);
        port2 = AvailablePortFinder.getNextAvailable(port1 + 1);
        port3 = AvailablePortFinder.getNextAvailable(port2 + 1);
    }

    @Test
    public void testPresenceAgentBasedPubSub() throws Exception {
        
        CamelContext camelctx = new DefaultCamelContext();
        camelctx.addRoutes(createRouteBuilder());

        MockEndpoint mockNeverland = camelctx.getEndpoint("mock:neverland", MockEndpoint.class);
        mockNeverland.expectedMessageCount(0);

        MockEndpoint mockNotification = camelctx.getEndpoint("mock:notification", MockEndpoint.class);
        mockNotification.expectedMinimumMessageCount(1);

        camelctx.start();
        try {
            ProducerTemplate template = camelctx.createProducerTemplate();
            template.sendBodyAndHeader(
                    "sip://agent@localhost:" + port1 + "?stackName=client&eventHeaderName=evtHdrName&eventId=evtid&fromUser=user2&fromHost=localhost&fromPort=" + port3,
                    "EVENT_A",
                    "REQUEST_METHOD", Request.PUBLISH);         

            mockNeverland.assertIsSatisfied();
            mockNotification.assertIsSatisfied();
        } finally {
            camelctx.stop();
        }
    }
    
    private RouteBuilder createRouteBuilder() throws Exception {
        return new RouteBuilder() {
            @Override
            public void configure() throws Exception {  
                // Create PresenceAgent
                fromF("sip://agent@localhost:%s?stackName=PresenceAgent&presenceAgent=true&eventHeaderName=evtHdrName&eventId=evtid", port1)
                    .to("log:neverland")
                    .to("mock:neverland");
                
                fromF("sip://johndoe@localhost:%s?stackName=Subscriber&toUser=agent&toHost=localhost&toPort=%s&eventHeaderName=evtHdrName&eventId=evtid", port2, port1)
                    .to("log:notification")
                    .to("mock:notification");
            }
        };
    }

} 
