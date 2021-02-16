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
package org.wildfly.camel.test.hipchat;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.hipchat.HipchatConstants;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.http.HttpEntity;
import org.apache.http.ProtocolVersion;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.message.BasicStatusLine;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.objenesis.Objenesis;
import org.wildfly.camel.test.common.utils.ManifestBuilder;
import org.wildfly.extension.camel.CamelAware;

import net.bytebuddy.ByteBuddy;

/**
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 */
@CamelAware
@RunWith(Arquillian.class)
public class HipchatConsumerIntegrationTest {

    private CloseableHttpResponse closeableHttpResponse = mock(CloseableHttpResponse.class);

    @Deployment
    public static JavaArchive createDeployment() {
        JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "camel-hipchat-tests.jar");
        archive.addClasses(HipchatComponentSupport.class, HipchatEndpointSupport.class);
        archive.setManifest(new Asset() {
            @Override
            public InputStream openStream() {
                ManifestBuilder builder = new ManifestBuilder();
                builder.addManifestHeader("Dependencies", "org.mockito");
                return builder.openStream();
            }
        });
        return archive;
    }

    private CamelContext createCamelContext() throws Exception {
        CamelContext context = new DefaultCamelContext();
        context.addComponent("hipchat", new HipchatComponentSupport(context, null, closeableHttpResponse));
        context.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("hipchat://?authToken=anything&consumeUsers=@AUser").to("mock:result");
            }
        });
        return context;
    }

    @Test
    public void sendInOnly() throws Exception {
        CamelContext camelctx = createCamelContext();

        MockEndpoint result = camelctx.getEndpoint("mock:result", MockEndpoint.class);
        result.expectedMessageCount(1);

        camelctx.start();
        try {
            String expectedResponse = "{\n" + "  \"items\" : [\n" + "    {\n" //
                    + "      \"date\" : \"2015-01-19T22:07:11.030740+00:00\",\n" //
                    + "      \"from\" : {\n" //
                    + "        \"id\" : 1647095,\n" //
                    + "        \"links\" : {\n" //
                    + "          \"self\" : \"https://api.hipchat.com/v2/user/1647095\"\n" //
                    + "        },\n" //
                    + "        \"mention_name\" : \"notifier\",\n" //
                    + "        \"name\" : \"Message Notifier\"\n" //
                    + "      },\n" //
                    + "      \"id\" : \"6567c6f7-7c1b-43cf-bed0-792b1d092919\",\n" //
                    + "      \"mentions\" : [ ],\n" //
                    + "      \"message\" : \"Unit test Alert\",\n" //
                    + "      \"type\" : \"message\"\n" //
                    + "    }\n" //
                    + "  ],\n" //
                    + "  \"links\" : {\n" //
                    + "    \"self\" : \"https://api.hipchat.com/v2/user/%40ShreyasPurohit/history/latest\"\n" //
                    + "  },\n" //
                    + "  \"maxResults\" : 1,\n" //
                    + "  \"startIndex\" : 0\n" //
                    + "}";
            HttpEntity mockHttpEntity = mock(HttpEntity.class);
            when(mockHttpEntity.getContent())
                    .thenReturn(new ByteArrayInputStream(expectedResponse.getBytes(StandardCharsets.UTF_8)));
            when(closeableHttpResponse.getEntity()).thenReturn(mockHttpEntity);
            when(closeableHttpResponse.getStatusLine())
                    .thenReturn(new BasicStatusLine(new ProtocolVersion("HTTP", 1, 1), 200, ""));

            result.assertIsSatisfied();

            assertCommonResultExchange(result.getExchanges().get(0));
        } finally {
            camelctx.close();
        }
    }

    private void assertCommonResultExchange(Exchange resultExchange) {
        HipchatEndpointSupport.assertIsInstanceOf(String.class, resultExchange.getIn().getBody());
        Assert.assertEquals("Unit test Alert", resultExchange.getIn().getBody(String.class));
        Assert.assertEquals("@AUser", resultExchange.getIn().getHeader(HipchatConstants.FROM_USER));
        Assert.assertEquals("2015-01-19T22:07:11.030740+00:00",
                resultExchange.getIn().getHeader(HipchatConstants.MESSAGE_DATE));
        Assert.assertNotNull(resultExchange.getIn().getHeader(HipchatConstants.FROM_USER_RESPONSE_STATUS));
    }

    @Test
    public void sendInOnlyMultipleUsers() throws Exception {
        CamelContext camelctx = createCamelContext();

        MockEndpoint result = camelctx.getEndpoint("mock:result", MockEndpoint.class);
        result.expectedMessageCount(1);

        camelctx.start();
        try {
            String expectedResponse = "{\n" //
                    + "  \"items\" : [\n" //
                    + "    {\n" //
                    + "      \"date\" : \"2015-01-19T22:07:11.030740+00:00\",\n" //
                    + "      \"from\" : {\n" //
                    + "        \"id\" : 1647095,\n" //
                    + "        \"links\" : {\n" //
                    + "          \"self\" : \"https://api.hipchat.com/v2/user/1647095\"\n" //
                    + "        },\n" //
                    + "        \"mention_name\" : \"notifier\",\n" //
                    + "        \"name\" : \"Message Notifier\"\n" //
                    + "      },\n" //
                    + "      \"id\" : \"6567c6f7-7c1b-43cf-bed0-792b1d092919\",\n" //
                    + "      \"mentions\" : [ ],\n" //
                    + "      \"message\" : \"Unit test Alert\",\n" //
                    + "      \"type\" : \"message\"\n" //
                    + "    }\n" //
                    + "  ],\n" //
                    + "  \"links\" : {\n" //
                    + "    \"self\" : \"https://api.hipchat.com/v2/user/%40ShreyasPurohit/history/latest\"\n" //
                    + "  },\n" //
                    + "  \"maxResults\" : 1,\n" //
                    + "  \"startIndex\" : 0\n" //
                    + "}";
            HttpEntity mockHttpEntity = mock(HttpEntity.class);
            when(mockHttpEntity.getContent())
                    .thenReturn(new ByteArrayInputStream(expectedResponse.getBytes(StandardCharsets.UTF_8)));
            when(closeableHttpResponse.getEntity()).thenReturn(mockHttpEntity);
            when(closeableHttpResponse.getStatusLine())
                    .thenReturn(new BasicStatusLine(new ProtocolVersion("HTTP", 1, 1), 200, ""));

            result.assertIsSatisfied();

            assertCommonResultExchange(result.getExchanges().get(0));
        } finally {
            camelctx.close();
        }
    }

    @Test
    public void sendInOnlyNoResponse() throws Exception {
        CamelContext camelctx = createCamelContext();

        MockEndpoint result = camelctx.getEndpoint("mock:result", MockEndpoint.class);
        result.expectedMessageCount(0);

        camelctx.start();
        try {
            ProducerTemplate template = camelctx.createProducerTemplate();
            HttpEntity mockHttpEntity = mock(HttpEntity.class);
            when(mockHttpEntity.getContent()).thenReturn(null);
            when(closeableHttpResponse.getEntity()).thenReturn(mockHttpEntity);
            when(closeableHttpResponse.getStatusLine())
                    .thenReturn(new BasicStatusLine(new ProtocolVersion("HTTP", 1, 1), 200, ""));

            result.assertIsSatisfied();
        } finally {
            camelctx.close();
        }
    }

}
