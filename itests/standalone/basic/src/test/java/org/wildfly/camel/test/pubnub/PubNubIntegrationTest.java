/*
 * #%L
 * Wildfly Camel :: Testsuite
 * %%
 * Copyright (C) 2013 - 2017 RedHat
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package org.wildfly.camel.test.pubnub;

import javax.naming.InitialContext;
import javax.naming.NamingException;

import com.pubnub.api.PNConfiguration;
import com.pubnub.api.PubNub;
import com.pubnub.api.enums.PNLogVerbosity;

import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.support.jndi.JndiBeanRepository;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.camel.test.pubnub.subA.FakePubNubPublishAPIServlet;
import org.wildfly.extension.camel.CamelAware;
import static com.pubnub.api.enums.PNHeartbeatNotificationOptions.NONE;
import static org.apache.camel.component.pubnub.PubNubConstants.CHANNEL;
import static org.apache.camel.component.pubnub.PubNubConstants.TIMETOKEN;

@CamelAware
@RunWith(Arquillian.class)
public class PubNubIntegrationTest {

    @ArquillianResource
    private InitialContext context;
    private PubNub pubNub;

    @Deployment
    public static WebArchive createDeployment() {
        return ShrinkWrap.create(WebArchive.class, "camel-pubnub-tests.war")
            .addPackage(FakePubNubPublishAPIServlet.class.getPackage());
    }

    @Before
    public void setUp() throws Exception {
        pubNub = createMockedPubNub();
        context.bind("pubnub", pubNub);
    }

    @After
    public void tearDown() {
        try {
            context.unbind("pubnub");
        } catch (NamingException e) {
            // Ignore
        } finally {
            if (pubNub != null) {
                pubNub.destroy();
            }
        }
    }

    @Test
    public void testPubNubPublish() throws Exception {
        CamelContext camelctx = new DefaultCamelContext(new JndiBeanRepository());
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:start")
                .to("pubnub:publishChannel?pubnub=#pubnub")
                .to("mock:resultA");
            }
        });

        MockEndpoint mockEndpoint = camelctx.getEndpoint("mock:resultA", MockEndpoint.class);
        mockEndpoint.expectedMessageCount(1);
        mockEndpoint.expectedHeaderReceived(TIMETOKEN, "14598111595318003");

        camelctx.start();
        try {
            ProducerTemplate template = camelctx.createProducerTemplate();
            template.sendBody("direct:start", "Hello Kermit");

            mockEndpoint.assertIsSatisfied();
        } finally {
            camelctx.stop();
        }
    }

    @Test
    public void testPubNubSubscriber() throws Exception {
        CamelContext camelctx = new DefaultCamelContext(new JndiBeanRepository());
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("pubnub:subscriberChannel?pubnub=#pubnub").id("subscriber").autoStartup(false)
                .to("mock:resultB");
            }
        });

        camelctx.start();
        try {
            MockEndpoint mockEndpoint = camelctx.getEndpoint("mock:resultB", MockEndpoint.class);

            camelctx.getRouteController().startRoute("subscriber");
            mockEndpoint.expectedMinimumMessageCount(1);
            mockEndpoint.expectedHeaderReceived(CHANNEL, "subscriberChannel");
            mockEndpoint.assertIsSatisfied();
        } finally {
            camelctx.stop();
        }
    }

    private PubNub createMockedPubNub() {

        PNConfiguration pnConfiguration = new PNConfiguration();
        pnConfiguration.setOrigin("localhost:8080/camel-pubnub-tests/");
        pnConfiguration.setSecure(false);
        pnConfiguration.setSubscribeKey("wfcSubscribeKey");
        pnConfiguration.setPublishKey("wfcPublishKey");
        pnConfiguration.setUuid("wfcUUID");
        pnConfiguration.setLogVerbosity(PNLogVerbosity.NONE);
        pnConfiguration.setHeartbeatNotificationOptions(NONE);

        class MockedPubNub extends PubNub {

            MockedPubNub(PNConfiguration initialConfig) {
                super(initialConfig);
            }

            @Override
            public int getTimestamp() {
                return 12345;
            }

            @Override
            public String getVersion() {
                return "WFC Test 1.0";
            }

            @Override
            public String getInstanceId() {
                return "PubNubInstanceId";
            }

            @Override
            public String getRequestId() {
                return "PubNubRequestId";
            }
        }

        return new MockedPubNub(pnConfiguration);
    }
}
