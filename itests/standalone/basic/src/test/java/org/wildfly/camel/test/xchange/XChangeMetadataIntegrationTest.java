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

import static org.apache.camel.component.xchange.XChangeConfiguration.HEADER_CURRENCY;
import static org.apache.camel.component.xchange.XChangeConfiguration.HEADER_CURRENCY_PAIR;

import java.util.List;

import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.impl.DefaultCamelContext;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.knowm.xchange.currency.Currency;
import org.knowm.xchange.currency.CurrencyPair;
import org.knowm.xchange.dto.meta.CurrencyMetaData;
import org.knowm.xchange.dto.meta.CurrencyPairMetaData;
import org.wildfly.extension.camel.CamelAware;

@CamelAware
@RunWith(Arquillian.class)
public class XChangeMetadataIntegrationTest {

    @Deployment
    public static JavaArchive createDeployment() {
        return ShrinkWrap.create(JavaArchive.class, "camel-xchange-metadata-tests");
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testCurrencies() throws Exception {

        CamelContext camelctx = new DefaultCamelContext();
        camelctx.addRoutes(createRouteBuilder());

        camelctx.start();
        try {
            ProducerTemplate template = camelctx.createProducerTemplate();
            List<Currency> currencies = template.requestBody("direct:currencies", null, List.class);
            Assert.assertNotNull("Currencies not null", currencies);
            Assert.assertTrue("Contains ETH", currencies.contains(Currency.ETH));
        } finally {
            camelctx.stop();
        }
    }

    @Test
    public void testCurrencyMetaData() throws Exception {

        CamelContext camelctx = new DefaultCamelContext();
        camelctx.addRoutes(createRouteBuilder());

        camelctx.start();
        try {
            ProducerTemplate template = camelctx.createProducerTemplate();
            CurrencyMetaData metadata = template.requestBody("direct:currencyMetaData", Currency.ETH, CurrencyMetaData.class);
            Assert.assertNotNull("CurrencyMetaData not null", metadata);

            metadata = template.requestBodyAndHeader("direct:currencyMetaData", null, HEADER_CURRENCY, Currency.ETH, CurrencyMetaData.class);
            Assert.assertNotNull("CurrencyMetaData not null", metadata);
        } finally {
            camelctx.stop();
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testCurrencyPairs() throws Exception {

        CamelContext camelctx = new DefaultCamelContext();
        camelctx.addRoutes(createRouteBuilder());

        camelctx.start();
        try {
            ProducerTemplate template = camelctx.createProducerTemplate();
            List<CurrencyPair> pairs = template.requestBody("direct:currencyPairs", null, List.class);
            Assert.assertNotNull("Pairs not null", pairs);
            Assert.assertTrue("Contains EOS/ETH", pairs.contains(CurrencyPair.EOS_ETH));
        } finally {
            camelctx.stop();
        }
    }

    @Test
    public void testCurrencyPairMetaData() throws Exception {

        CamelContext camelctx = new DefaultCamelContext();
        camelctx.addRoutes(createRouteBuilder());

        camelctx.start();
        try {
            ProducerTemplate template = camelctx.createProducerTemplate();
            CurrencyPairMetaData metadata = template.requestBody("direct:currencyPairMetaData", CurrencyPair.EOS_ETH, CurrencyPairMetaData.class);
            Assert.assertNotNull("CurrencyPairMetaData not null", metadata);

            metadata = template.requestBodyAndHeader("direct:currencyPairMetaData", null, HEADER_CURRENCY_PAIR, CurrencyPair.EOS_ETH, CurrencyPairMetaData.class);
            Assert.assertNotNull("CurrencyPairMetaData not null", metadata);
        } finally {
            camelctx.stop();
        }
    }

    private RouteBuilder createRouteBuilder() throws Exception {
        return new RouteBuilder() {
            @Override
            public void configure() throws Exception {

                from("direct:currencies")
                    .to("xchange:binance?service=metadata&method=currencies");

                from("direct:currencyMetaData")
                    .to("xchange:binance?service=metadata&method=currencyMetaData");

                from("direct:currencyPairs")
                    .to("xchange:binance?service=metadata&method=currencyPairs");

                from("direct:currencyPairMetaData")
                    .to("xchange:binance?service=metadata&method=currencyPairMetaData");
            }
        };
    }
}
