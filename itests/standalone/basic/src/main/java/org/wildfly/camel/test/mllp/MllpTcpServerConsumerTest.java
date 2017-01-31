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
package org.wildfly.camel.test.mllp;

import java.util.concurrent.TimeUnit;

import org.apache.camel.CamelContext;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.impl.DefaultCamelContext;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.camel.test.common.utils.AvailablePortFinder;
import org.wildfly.camel.test.mllp.subA.Hl7MessageGenerator;
import org.wildfly.camel.test.mllp.subA.MllpClientResource;
import org.wildfly.extension.camel.CamelAware;

@CamelAware
@RunWith(Arquillian.class)
public class MllpTcpServerConsumerTest {
    
    @Rule
    public MllpClientResource mllpClient = new MllpClientResource();
    
    @Deployment
    public static JavaArchive createdeployment() {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "camel-mllp-tests");
        archive.addPackage(MllpClientResource.class.getPackage());
        archive.addClasses(AvailablePortFinder.class);
        return archive;
    }
    
    @Test
    public void testReceiveSingleMessage() throws Exception {
        
        mllpClient.setMllpHost("localhost");
        mllpClient.setMllpPort(AvailablePortFinder.getNextAvailable());

        CamelContext camelctx = new DefaultCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            int connectTimeout = 500;
            int responseTimeout = 5000;

            @Override
            public void configure() throws Exception {
                String routeId = "mllp-test-receiver-route";

                fromF("mllp://%s:%d?autoAck=true&connectTimeout=%d&receiveTimeout=%d&reuseAddress=true",
                        mllpClient.getMllpHost(), mllpClient.getMllpPort(), connectTimeout, responseTimeout)
                        .routeId(routeId)
                        .log(LoggingLevel.INFO, routeId, "Test route received message")
                        .to("mock:result");

            }
        });

        MockEndpoint mock = camelctx.getEndpoint("mock:result", MockEndpoint.class);
        mock.expectedMessageCount(1);
        
        camelctx.start();
        try {
            mllpClient.connect();
            mllpClient.sendMessageAndWaitForAcknowledgement(Hl7MessageGenerator.generateMessage(), 10000);
            MockEndpoint.assertIsSatisfied(10, TimeUnit.SECONDS, mock);
        } finally {
            mllpClient.disconnect();
            camelctx.stop();
        }
    }
}

