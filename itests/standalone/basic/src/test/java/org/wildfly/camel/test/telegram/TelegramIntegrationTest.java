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
package org.wildfly.camel.test.telegram;


import java.io.IOException;
import java.io.InputStream;

import org.apache.camel.CamelContext;
import org.apache.camel.RoutesBuilder;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.component.telegram.TelegramService;
import org.apache.camel.component.telegram.TelegramServiceProvider;
import org.apache.camel.component.telegram.model.UpdateResult;
import org.apache.camel.impl.DefaultCamelContext;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.objenesis.Objenesis;
import org.wildfly.extension.camel.CamelAware;

import com.fasterxml.jackson.databind.ObjectMapper;

import net.bytebuddy.ByteBuddy;

@CamelAware
@RunWith(Arquillian.class)
public class TelegramIntegrationTest {

    @Deployment
    public static JavaArchive createDeployment() {
        JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "camel-telegram-tests");
        archive.addPackages(true, Mockito.class.getPackage(), Objenesis.class.getPackage(), ByteBuddy.class.getPackage());
        archive.addAsResource("telegram/updates-single.json");
        archive.addAsResource("telegram/updates-empty.json");
        return archive;
    }

    @Before
    public void mockAPIs() throws IOException {
        TelegramService api = mockTelegramService();

        UpdateResult res1 = getJSONResource("telegram/updates-single.json", UpdateResult.class);
        res1.getUpdates().get(0).getMessage().setText("message1");

        UpdateResult res2 = getJSONResource("telegram/updates-single.json", UpdateResult.class);
        res2.getUpdates().get(0).getMessage().setText("message2");

        UpdateResult defaultRes = getJSONResource("telegram/updates-empty.json", UpdateResult.class);
        Mockito.when(api.getUpdates(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
            .thenReturn(res1).thenReturn(res2).thenAnswer((i) -> defaultRes);
    }

    @Test
    public void testReceptionOfTwoMessages() throws Exception {
        
        CamelContext camelctx = new DefaultCamelContext();
        camelctx.addRoutes(createRouteBuilder());

        MockEndpoint endpoint = camelctx.getEndpoint("mock:telegram", MockEndpoint.class);
        camelctx.start();
        try {
            endpoint.expectedMinimumMessageCount(2);
            endpoint.expectedBodiesReceived("message1", "message2");

            endpoint.assertIsSatisfied();
        } finally {
            camelctx.stop();
        }
    }

    private RoutesBuilder createRouteBuilder() throws Exception {
        return new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("telegram:bots/mock-token")
                        .convertBodyTo(String.class)
                        .to("mock:telegram");
            }
        };
    }

    private TelegramService mockTelegramService() {
        TelegramService mockService = Mockito.mock(TelegramService.class);
        TelegramServiceProvider.get().setAlternativeService(mockService);
        return mockService;
    }

    private <T> T getJSONResource(String resName, Class<T> clazz) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        try (InputStream stream = getClass().getClassLoader().getResourceAsStream(resName)) {
            T value = mapper.readValue(stream, clazz);
            return value;
        }
    }
}
