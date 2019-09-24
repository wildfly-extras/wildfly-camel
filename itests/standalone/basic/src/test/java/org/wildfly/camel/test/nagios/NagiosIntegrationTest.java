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
package org.wildfly.camel.test.nagios;

import java.util.HashMap;
import java.util.Map;

import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.component.nagios.NagiosComponent;
import org.apache.camel.component.nagios.NagiosConstants;
import org.apache.camel.component.nagios.NagiosEndpoint;
import org.apache.camel.impl.DefaultCamelContext;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.objenesis.Objenesis;
import org.wildfly.extension.camel.CamelAware;

import com.googlecode.jsendnsca.Level;
import com.googlecode.jsendnsca.MessagePayload;
import com.googlecode.jsendnsca.NagiosPassiveCheckSender;
import com.googlecode.jsendnsca.PassiveCheckSender;

import net.bytebuddy.ByteBuddy;

@CamelAware
@RunWith(Arquillian.class)
public class NagiosIntegrationTest {

    private PassiveCheckSender nagiosPassiveCheckSender;

    @Deployment
    public static JavaArchive createDeployment() {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "camel-nagios-test");
        archive.addPackages(true, Mockito.class.getPackage(), Objenesis.class.getPackage(), ByteBuddy.class.getPackage());
        return archive;
    }

    @Before
    public void before() {
        nagiosPassiveCheckSender =  Mockito.mock(NagiosPassiveCheckSender.class);
    }

    @Test
    public void testSendToNagios() throws Exception {

        CamelContext camelctx = new DefaultCamelContext();
        camelctx.addRoutes(createRouteBuilder());

        MessagePayload expectedPayload = new MessagePayload("localhost", Level.OK, camelctx.getName(),  "Hello Nagios");

        MockEndpoint mock = camelctx.getEndpoint("mock:result", MockEndpoint.class);
        mock.expectedMessageCount(1);
        mock.allMessages().body().isInstanceOf(String.class);
        mock.expectedBodiesReceived("Hello Nagios");

        camelctx.start();
        try {
            ProducerTemplate template = camelctx.createProducerTemplate();
            template.sendBody("direct:start", "Hello Nagios");

            mock.assertIsSatisfied();

            Mockito.verify(nagiosPassiveCheckSender, Mockito.times(1)).send(expectedPayload);
        } finally {
            camelctx.close();
        }
    }

    @Test
    public void testSendTwoToNagios() throws Exception {

        CamelContext camelctx = new DefaultCamelContext();
        camelctx.addRoutes(createRouteBuilder());

        MessagePayload expectedPayload1 = new MessagePayload("localhost", Level.OK, camelctx.getName(),  "Hello Nagios");
        MessagePayload expectedPayload2 = new MessagePayload("localhost", Level.OK, camelctx.getName(),  "Bye Nagios");

        MockEndpoint mock = camelctx.getEndpoint("mock:result", MockEndpoint.class);
        mock.expectedMessageCount(2);
        mock.allMessages().body().isInstanceOf(String.class);
        mock.expectedBodiesReceived("Hello Nagios", "Bye Nagios");

        camelctx.start();
        try {
            ProducerTemplate template = camelctx.createProducerTemplate();
            template.sendBody("direct:start", "Hello Nagios");
            template.sendBody("direct:start", "Bye Nagios");

            mock.assertIsSatisfied();

            Mockito.verify(nagiosPassiveCheckSender).send(expectedPayload1);
            Mockito.verify(nagiosPassiveCheckSender).send(expectedPayload2);
        } finally {
            camelctx.close();
        }
    }

    @Test
    public void testSendToNagiosWarn() throws Exception {

        CamelContext camelctx = new DefaultCamelContext();
        camelctx.addRoutes(createRouteBuilder());

        MessagePayload expectedPayload1 = new MessagePayload("localhost", Level.WARNING, camelctx.getName(),  "Hello Nagios");

        MockEndpoint mock = camelctx.getEndpoint("mock:result", MockEndpoint.class);
        mock.expectedMessageCount(1);
        mock.expectedBodiesReceived("Hello Nagios");

        camelctx.start();
        try {
            ProducerTemplate template = camelctx.createProducerTemplate();
            template.sendBodyAndHeader("direct:start", "Hello Nagios", NagiosConstants.LEVEL, Level.WARNING);

            mock.assertIsSatisfied();
            Mockito.verify(nagiosPassiveCheckSender).send(expectedPayload1);
        } finally {
            camelctx.close();
        }
    }

    @Test
    public void testSendToNagiosWarnAsText() throws Exception {

        CamelContext camelctx = new DefaultCamelContext();
        camelctx.addRoutes(createRouteBuilder());

        MessagePayload expectedPayload1 = new MessagePayload("localhost", Level.WARNING, camelctx.getName(),  "Hello Nagios");

        MockEndpoint mock = camelctx.getEndpoint("mock:result", MockEndpoint.class);
        mock.expectedMessageCount(1);
        mock.expectedBodiesReceived("Hello Nagios");

        camelctx.start();
        try {
            ProducerTemplate template = camelctx.createProducerTemplate();
            template.sendBodyAndHeader("direct:start", "Hello Nagios", NagiosConstants.LEVEL, "WARNING");

            mock.assertIsSatisfied();

            Mockito.verify(nagiosPassiveCheckSender).send(expectedPayload1);
        } finally {
            camelctx.close();
        }
    }

    @Test
    public void testSendToNagiosMultiHeaders() throws Exception {

        CamelContext camelctx = new DefaultCamelContext();
        camelctx.addRoutes(createRouteBuilder());

        MessagePayload expectedPayload1 = new MessagePayload("myHost", Level.CRITICAL, "myService",  "Hello Nagios");

        MockEndpoint mock = camelctx.getEndpoint("mock:result", MockEndpoint.class);
        mock.expectedMessageCount(1);
        mock.expectedBodiesReceived("Hello Nagios");

        Map<String, Object> headers = new HashMap<String, Object>();
        headers.put(NagiosConstants.LEVEL, "CRITICAL");
        headers.put(NagiosConstants.HOST_NAME, "myHost");
        headers.put(NagiosConstants.SERVICE_NAME, "myService");

        camelctx.start();
        try {
            ProducerTemplate template = camelctx.createProducerTemplate();
            template.sendBodyAndHeaders("direct:start", "Hello Nagios", headers);

            mock.assertIsSatisfied();
            Mockito.verify(nagiosPassiveCheckSender).send(expectedPayload1);
        } finally {
            camelctx.close();
        }
    }

    private RouteBuilder createRouteBuilder() throws Exception {
        return new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                String uri = "nagios:127.0.0.1:25664?password=secret";

                NagiosComponent nagiosComponent = new NagiosComponent();
                nagiosComponent.setCamelContext(getContext());
                NagiosEndpoint nagiousEndpoint = (NagiosEndpoint) nagiosComponent.createEndpoint(uri);
                nagiousEndpoint.setSender(nagiosPassiveCheckSender);

                from("direct:start")
                        .to(nagiousEndpoint)
                        .to("mock:result");
            }
        };
    }
}
