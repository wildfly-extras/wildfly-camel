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
package org.wildfly.camel.test.milo;

import static java.util.Collections.singletonList;
import static org.apache.camel.component.milo.NodeIds.nodeValue;
import static org.eclipse.milo.opcua.sdk.server.api.config.OpcUaServerConfig.USER_TOKEN_POLICY_ANONYMOUS;

import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.milo.server.MiloServerComponent;
import org.apache.camel.component.mock.AssertionClause;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.impl.DefaultCamelContext;
import org.eclipse.milo.opcua.sdk.server.api.config.OpcUaServerConfig;
import org.eclipse.milo.opcua.sdk.server.identity.AnonymousIdentityValidator;
import org.eclipse.milo.opcua.stack.core.UaException;
import org.eclipse.milo.opcua.stack.core.application.CertificateValidator;
import org.eclipse.milo.opcua.stack.core.application.DefaultCertificateManager;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.camel.test.common.utils.AvailablePortFinder;
import org.wildfly.extension.camel.CamelAware;

/**
 * Unit tests for writing from the client side
 */
@CamelAware
@RunWith(Arquillian.class)
public class MiloIntegrationTest {

    private static final String DIRECT_START_1 = "direct:start1";
    private static final String DIRECT_START_2 = "direct:start2";
    private static final String DIRECT_START_3 = "direct:start3";
    private static final String DIRECT_START_4 = "direct:start4";

    private static final String MILO_SERVER_ITEM_1 = "milo-server:myitem1";
    private static final String MILO_SERVER_ITEM_2 = "milo-server:myitem2";

    private static final String MILO_CLIENT_BASE_C1 = "milo-client:tcp://foo:bar@localhost:@@port@@";
    private static final String MILO_CLIENT_BASE_C2 = "milo-client:tcp://foo2:bar2@localhost:@@port@@";

    private static final String MILO_CLIENT_ITEM_C1_1 = MILO_CLIENT_BASE_C1 + "?allowedSecurityPolicies=None&node=" + nodeValue(MiloServerComponent.DEFAULT_NAMESPACE_URI, "items-myitem1");
    private static final String MILO_CLIENT_ITEM_C1_2 = MILO_CLIENT_BASE_C1 + "?allowedSecurityPolicies=None&node=" + nodeValue(MiloServerComponent.DEFAULT_NAMESPACE_URI, "items-myitem2");

    private static final String MILO_CLIENT_ITEM_C2_1 = MILO_CLIENT_BASE_C2 + "?allowedSecurityPolicies=None&node=" + nodeValue(MiloServerComponent.DEFAULT_NAMESPACE_URI, "items-myitem1");
    private static final String MILO_CLIENT_ITEM_C2_2 = MILO_CLIENT_BASE_C2 + "?allowedSecurityPolicies=None&node=" + nodeValue(MiloServerComponent.DEFAULT_NAMESPACE_URI, "items-myitem2");

    private static final String MOCK_TEST_1 = "mock:test1";
    private static final String MOCK_TEST_2 = "mock:test2";

    private int serverPort = AvailablePortFinder.getNextAvailable();

    @Deployment
    public static JavaArchive createDeployment() {
        JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "milo-client-tests.jar");
        archive.addClasses(AvailablePortFinder.class);
        return archive;
    }

    @Test
    public void testClientWrite() throws Exception {

        CamelContext camelctx = new DefaultCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            public void configure() throws Exception {

                from(MILO_SERVER_ITEM_1).to(MOCK_TEST_1);
                from(MILO_SERVER_ITEM_2).to(MOCK_TEST_2);

                from(DIRECT_START_1).to(resolve(MILO_CLIENT_ITEM_C1_1));
                from(DIRECT_START_2).to(resolve(MILO_CLIENT_ITEM_C1_2));

                from(DIRECT_START_3).to(resolve(MILO_CLIENT_ITEM_C2_1));
                from(DIRECT_START_4).to(resolve(MILO_CLIENT_ITEM_C2_2));
            }
        });

        OpcUaServerConfig serverConfig = OpcUaServerConfig.builder()
            .setEndpointAddresses(Arrays.asList(new String[] {"localhost"}))
            .setCertificateValidator(new CertificateValidator() {
                @Override
                public void validate(X509Certificate certificate) throws UaException {
                }

                @Override
                public void verifyTrustChain(List<X509Certificate> certificateChain) throws UaException {
                }
            })
            .setCertificateManager(new DefaultCertificateManager())
            .setUserTokenPolicies(singletonList(USER_TOKEN_POLICY_ANONYMOUS))
            .setIdentityValidator(AnonymousIdentityValidator.INSTANCE)
            .build();

        MiloServerComponent server = new MiloServerComponent(serverConfig);
        server.setBindAddresses("localhost");
        server.setBindPort(serverPort);
        server.setEnableAnonymousAuthentication(true);
        camelctx.addComponent("milo-server", server);

        camelctx.start();
        try {
            MockEndpoint test1Endpoint = camelctx.getEndpoint(MOCK_TEST_1, MockEndpoint.class);
            MockEndpoint test2Endpoint = camelctx.getEndpoint(MOCK_TEST_2, MockEndpoint.class);

            // item 1
            test1Endpoint.setExpectedCount(2);
            testBody(test1Endpoint.message(0), assertGoodValue("Foo1"));
            testBody(test1Endpoint.message(1), assertGoodValue("Foo3"));

            // item 1
            test2Endpoint.setExpectedCount(2);
            testBody(test2Endpoint.message(0), assertGoodValue("Foo2"));
            testBody(test2Endpoint.message(1), assertGoodValue("Foo4"));

            ProducerTemplate producer1 = camelctx.createProducerTemplate();
            producer1.setDefaultEndpointUri(DIRECT_START_1);

            ProducerTemplate producer2 = camelctx.createProducerTemplate();
            producer2.setDefaultEndpointUri(DIRECT_START_2);

            ProducerTemplate producer3 = camelctx.createProducerTemplate();
            producer3.setDefaultEndpointUri(DIRECT_START_3);

            ProducerTemplate producer4 = camelctx.createProducerTemplate();
            producer4.setDefaultEndpointUri(DIRECT_START_4);

            // send
            sendValue(producer1, new Variant("Foo1"));
            sendValue(producer2, new Variant("Foo2"));
            sendValue(producer3, new Variant("Foo3"));
            sendValue(producer4, new Variant("Foo4"));

            MockEndpoint.assertIsSatisfied(camelctx);

        } finally {
            camelctx.close();
        }
    }

    private static void sendValue(final ProducerTemplate producerTemplate, final Variant variant) {
        // we always write synchronously since we do need the message order
        producerTemplate.sendBodyAndHeader(variant, "await", true);
    }

    private static void testBody(final AssertionClause clause, final Consumer<DataValue> valueConsumer) {
        testBody(clause, DataValue.class, valueConsumer);
    }

    private static <T> void testBody(final AssertionClause clause, final Class<T> bodyClass, final Consumer<T> valueConsumer) {
        clause.predicate(exchange -> {
            final T body = exchange.getIn().getBody(bodyClass);
            valueConsumer.accept(body);
            return true;
        });
    }

    private static Consumer<DataValue> assertGoodValue(final Object expectedValue) {
        return value -> {
            Assert.assertNotNull(value);
            Assert.assertEquals(expectedValue, value.getValue().getValue());
            Assert.assertTrue(value.getStatusCode().isGood());
            Assert.assertFalse(value.getStatusCode().isBad());
        };
    }

    private String resolve(String uri) {
        return uri != null ? uri.replace("@@port@@", Integer.toString(serverPort)) : null;
    }
}
