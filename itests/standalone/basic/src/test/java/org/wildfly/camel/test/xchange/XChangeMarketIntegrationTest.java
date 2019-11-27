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
package org.wildfly.camel.test.xchange;

import static org.apache.camel.component.xchange.XChangeConfiguration.HEADER_CURRENCY_PAIR;

import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.impl.DefaultCamelContext;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.knowm.xchange.currency.CurrencyPair;
import org.knowm.xchange.dto.marketdata.Ticker;
import org.wildfly.extension.camel.CamelAware;

@CamelAware
@RunWith(Arquillian.class)
public class XChangeMarketIntegrationTest extends AbstractXChangeIntegrationTest {

    @Deployment
    public static JavaArchive createDeployment() {
        JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "camel-xchange-market-tests");
        archive.addClasses(AbstractXChangeIntegrationTest.class);
        return archive;
    }

    @Test
    public void testTicker() throws Exception {

        try (CamelContext camelctx = new DefaultCamelContext()) {

            Assume.assumeTrue(checkAPIConnection());

            camelctx.addRoutes(createRouteBuilder());
            camelctx.start();

            ProducerTemplate template = camelctx.createProducerTemplate();
            Ticker ticker = template.requestBody("direct:ticker", CurrencyPair.EOS_ETH, Ticker.class);
            Assert.assertNotNull("Ticker not null", ticker);
            System.out.println(ticker);

            ticker = template.requestBodyAndHeader("direct:ticker", null, HEADER_CURRENCY_PAIR, CurrencyPair.EOS_ETH, Ticker.class);
            Assert.assertNotNull("Ticker not null", ticker);
            System.out.println(ticker);
        }
    }

    @Test
    public void testTickerBTCUSDT() throws Exception {

        try (CamelContext camelctx = new DefaultCamelContext()) {

            Assume.assumeTrue(checkAPIConnection());

            camelctx.addRoutes(createRouteBuilder());
            camelctx.start();

            ProducerTemplate template = camelctx.createProducerTemplate();
            Ticker ticker = template.requestBody("direct:tickerBTCUSDT", null, Ticker.class);
            Assert.assertNotNull("Ticker not null", ticker);
            System.out.println(ticker);
        }
    }

    private RouteBuilder createRouteBuilder() throws Exception {
        return new RouteBuilder() {
            @Override
            public void configure() throws Exception {

                from("direct:ticker")
                .to("xchange:binance?service=marketdata&method=ticker");

                from("direct:tickerBTCUSDT")
                    .to("xchange:binance?service=marketdata&method=ticker&currencyPair=BTC/USDT");
            }
        };
    }
}
