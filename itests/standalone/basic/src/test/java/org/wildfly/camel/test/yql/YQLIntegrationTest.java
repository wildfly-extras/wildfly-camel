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
package org.wildfly.camel.test.yql;

import static org.hamcrest.CoreMatchers.containsString;

import java.net.URLEncoder;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.http.HttpStatus;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.extension.camel.CamelAware;

@CamelAware
@RunWith(Arquillian.class)
public class YQLIntegrationTest {

    // CAMEL-12476: Make YQL header name constants public
    private static final String CAMEL_YQL_HTTP_STATUS = "CamelYqlHttpStatus";
    private static final String CAMEL_YQL_HTTP_REQUEST = "CamelYqlHttpRequest";

    private static final String BOOK_QUERY = "select * from google.books where q='barack obama' and maxResults=1";
    private static final String FINANCE_QUERY = "select symbol, Ask, Bid, from yahoo.finance.quotes where symbol in ('GOOG')";
    private static final String WEATHER_QUERY = "select wind, atmosphere from weather.forecast where woeid in (select woeid from geo.places(1) where text='chicago, il')";
    private static final String CALLBACK = "yqlCallback";
    private static final String ENV = "store://datatables.org/alltableswithkeys";

    @Deployment
    public static JavaArchive createDeployment() {
        return ShrinkWrap.create(JavaArchive.class, "camel-tests.jar");
    }

    @Test
    public void testFinanceQuote() throws Exception {
        CamelContext camelctx = new DefaultCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:start")
                .toF("yql://%s?format=json&callback=%s&https=false&env=%s", FINANCE_QUERY, CALLBACK, ENV)
                .to("mock:result");
            }
        });

        camelctx.start();
        try {
            MockEndpoint mockEndpoint = camelctx.getEndpoint("mock:result", MockEndpoint.class);

            ProducerTemplate template = camelctx.createProducerTemplate();
            template.sendBody("direct:start", null);

            final Exchange exchange = mockEndpoint.getReceivedExchanges().get(0);
            final String body = exchange.getIn().getBody(String.class);
            final Integer status = exchange.getIn().getHeader(CAMEL_YQL_HTTP_STATUS, Integer.class);
            final String httpRequest = exchange.getIn().getHeader(CAMEL_YQL_HTTP_REQUEST, String.class);
            Assert.assertThat(httpRequest, containsString("http"));
            Assert.assertThat(httpRequest, containsString("q=" + URLEncoder.encode(FINANCE_QUERY, "UTF-8")));
            Assert.assertThat(httpRequest, containsString("format=json"));
            Assert.assertThat(httpRequest, containsString("callback=" + CALLBACK));
            Assert.assertThat(httpRequest, containsString("diagnostics=false"));
            Assert.assertThat(httpRequest, containsString("debug=false"));
            Assert.assertThat(httpRequest, containsString("env=" + URLEncoder.encode(ENV, "UTF-8")));
            Assert.assertNotNull(body);
            Assert.assertThat(body, containsString(CALLBACK + "("));
            Assert.assertEquals(HttpStatus.SC_OK, status.intValue());
        } finally {
            camelctx.stop();
        }
    }

    @Test
    public void testWeather() throws Exception {
        CamelContext camelctx = new DefaultCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:start")
                .toF("yql://%s", WEATHER_QUERY)
                .to("mock:result");
            }
        });

        camelctx.start();
        try {
            MockEndpoint mockEndpoint = camelctx.getEndpoint("mock:result", MockEndpoint.class);

            ProducerTemplate template = camelctx.createProducerTemplate();
            template.sendBody("direct:start", null);

            final Exchange exchange = mockEndpoint.getReceivedExchanges().get(0);
            final String body = exchange.getIn().getBody(String.class);
            final Integer status = exchange.getIn().getHeader(CAMEL_YQL_HTTP_STATUS, Integer.class);
            final String httpRequest = exchange.getIn().getHeader(CAMEL_YQL_HTTP_REQUEST, String.class);
            Assert.assertThat(httpRequest, containsString("https"));
            Assert.assertThat(httpRequest, containsString("q=" + URLEncoder.encode(WEATHER_QUERY, "UTF-8")));
            Assert.assertThat(httpRequest, containsString("format=json"));
            Assert.assertThat(httpRequest, containsString("diagnostics=false"));
            Assert.assertThat(httpRequest, containsString("debug=false"));
            Assert.assertNotNull(body);
            Assert.assertEquals(HttpStatus.SC_OK, status.intValue());
        } finally {
            camelctx.stop();
        }
    }

    @Test
    public void testBook() throws Exception {
        CamelContext camelctx = new DefaultCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:start")
                .toF("yql://%s?format=xml&crossProduct=optimized&env=%s", BOOK_QUERY, ENV)
                .to("mock:result");
            }
        });

        camelctx.start();
        try {
            MockEndpoint mockEndpoint = camelctx.getEndpoint("mock:result", MockEndpoint.class);

            ProducerTemplate template = camelctx.createProducerTemplate();
            template.sendBody("direct:start", null);

            final Exchange exchange = mockEndpoint.getReceivedExchanges().get(0);
            final String body = exchange.getIn().getBody(String.class);
            final Integer status = exchange.getIn().getHeader(CAMEL_YQL_HTTP_STATUS, Integer.class);
            final String httpRequest = exchange.getIn().getHeader(CAMEL_YQL_HTTP_REQUEST, String.class);
            Assert.assertThat(httpRequest, containsString("https"));
            Assert.assertThat(httpRequest, containsString("q=" + URLEncoder.encode(BOOK_QUERY, "UTF-8")));
            Assert.assertThat(httpRequest, containsString("format=xml"));
            Assert.assertThat(httpRequest, containsString("diagnostics=false"));
            Assert.assertThat(httpRequest, containsString("debug=false"));
            Assert.assertThat(httpRequest, containsString("crossProduct=optimized"));
            Assert.assertNotNull(body);
            Assert.assertEquals(HttpStatus.SC_OK, status.intValue());
        } finally {
            camelctx.stop();
        }
    }
}
