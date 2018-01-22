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
package org.wildfly.camel.test.spring.ws;

import javax.xml.transform.Source;

import org.apache.camel.CamelContext;
import org.apache.camel.ExchangePattern;
import org.apache.camel.Message;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.StringSource;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.component.spring.ws.SpringWebserviceConstants;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.extension.camel.CamelContextRegistry;

import net.javacrumbs.springws.test.helper.InMemoryWebServiceMessageSender;

@RunWith(Arquillian.class)
public class SpringWsIntegrationTest {

    @ArquillianResource
    CamelContextRegistry contextRegistry;

    private final String stockQuoteWebserviceUri = "http://localhost/";
    private final String xmlRequestForGoogleStockQuote = "<GetQuote xmlns=\"http://www.webserviceX.NET/\"><symbol>GOOG</symbol></GetQuote>";

    private ProducerTemplate template;
    private MockEndpoint resultEndpoint;
    private MockEndpoint inOnlyEndpoint;

    @Deployment
    public static JavaArchive createDeployment() {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "spring-ws-tests");
        archive.addAsResource("spring/ws/springws-camel-context.xml");
        archive.addAsResource("spring/ws/stockquote-response.xml");
        archive.addPackage(InMemoryWebServiceMessageSender.class.getPackage());
        archive.addClasses(StockQuoteResponseProcessor.class);
        return archive;
    }

    @Before
    public void before() throws Exception {
        CamelContext camelctx = contextRegistry.getCamelContext("springwsContext");
        resultEndpoint = camelctx.getEndpoint("mock:result", MockEndpoint.class);
        inOnlyEndpoint = camelctx.getEndpoint("mock:inOnly", MockEndpoint.class);
        template = camelctx.createProducerTemplate();
    }

    @Test
    public void consumeStockQuoteWebserviceWithDefaultTemplate() throws Exception {
        Object result = template.requestBody("direct:stockQuoteWebserviceWithDefaultTemplate", xmlRequestForGoogleStockQuote);

        Assert.assertNotNull(result);
        Assert.assertTrue(result instanceof Source);
    }

    @Test
    public void consumeStockQuoteWebserviceAndPreserveHeaders() throws Exception {
        resultEndpoint.expectedHeaderReceived("helloHeader", "hello world!");

        Object result = template.requestBodyAndHeader("direct:stockQuoteWebserviceMock", xmlRequestForGoogleStockQuote, "helloHeader", "hello world!");

        Assert.assertNotNull(result);
        resultEndpoint.assertIsSatisfied();
    }

    @Test
    public void consumeStockQuoteWebservice() throws Exception {
        Object result = template.requestBody("direct:stockQuoteWebservice", xmlRequestForGoogleStockQuote);

        Assert.assertNotNull(result);
        Assert.assertTrue(result instanceof Source);
    }

    @Test
    public void consumeStockQuoteWebserviceWithCamelStringSourceInput() throws Exception {
        Object result = template.requestBody("direct:stockQuoteWebservice", new StringSource(xmlRequestForGoogleStockQuote));

        Assert.assertNotNull(result);
        Assert.assertTrue(result instanceof Source);
    }

    @Test
    public void consumeStockQuoteWebserviceWithNonDefaultMessageFactory() throws Exception {
        Object result = template.requestBody("direct:stockQuoteWebserviceWithNonDefaultMessageFactory", xmlRequestForGoogleStockQuote);

        Assert.assertNotNull(result);
        Assert.assertTrue(result instanceof Source);
    }

    @Test
    public void consumeStockQuoteWebserviceAndConvertResult() throws Exception {
        Object result = template.requestBody("direct:stockQuoteWebserviceAsString", xmlRequestForGoogleStockQuote);

        Assert.assertNotNull(result);
        Assert.assertTrue(result instanceof String);
        String resultMessage = (String) result;
        Assert.assertTrue(resultMessage.contains("Google Inc."));
    }

    @Test
    public void consumeStockQuoteWebserviceAndProvideEndpointUriByHeader() throws Exception {
        Object result = template.requestBodyAndHeader("direct:stockQuoteWebserviceWithoutDefaultUri", xmlRequestForGoogleStockQuote,
                SpringWebserviceConstants.SPRING_WS_ENDPOINT_URI, stockQuoteWebserviceUri);

        Assert.assertNotNull(result);
        Assert.assertTrue(result instanceof String);
        String resultMessage = (String) result;
        Assert.assertTrue(resultMessage.contains("Google Inc."));
    }

    @Test
    public void consumeStockQuoteWebserviceInOnly() throws Exception {
        inOnlyEndpoint.expectedExchangePattern(ExchangePattern.InOnly);
        inOnlyEndpoint.expectedMessageCount(1);

        template.sendBodyAndHeader("direct:stockQuoteWebserviceInOnly", xmlRequestForGoogleStockQuote, "foo", "bar");

        inOnlyEndpoint.assertIsSatisfied();

        Message in = inOnlyEndpoint.getReceivedExchanges().get(0).getIn();

        Object result = in.getBody();
        Assert.assertNotNull(result);
        Assert.assertTrue(result instanceof String);
        String resultMessage = (String) result;
        Assert.assertTrue(resultMessage.contains("Google Inc."));

        Object bar = in.getHeader("foo");
        Assert.assertEquals("The header value should have been preserved", "bar", bar);
    }
}
