/*
 * #%L
 * Wildfly Camel :: Testsuite
 * %%
 * Copyright (C) 2013 - 2016 RedHat
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
package org.wildfly.camel.test.sap.netweaver;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.sap.netweaver.NetWeaverConstants;
import org.apache.camel.impl.DefaultCamelContext;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.camel.test.common.utils.TestUtils;
import org.wildfly.extension.camel.CamelAware;

@CamelAware
@RunWith(Arquillian.class)
public class SAPNetweaverIntegrationTest {

    private static final String SAP_COMMAND = "FlightCollection(carrid='AA',connid='0017',fldate=datetime'2017-10-05T00%3A00%3A00')";

    @Deployment
    public static WebArchive createDeployment() {
        return ShrinkWrap.create(WebArchive.class, "SAPNetweaverIntegrationTest.war")
            .addClass(TestUtils.class)
            .addAsResource("sap/netweaver/flight-data.json", "flight-data.json")
            .addAsResource("sap/netweaver/flight-data.xml", "flight-data.xml");
    }

    @Test
    public void testSAPNetweaverEndpointJsonResponse() throws Exception {
        CamelContext camelctx = new DefaultCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:start")
                .to("sap-netweaver:http4://localhost:8080/sap/api");

                from("undertow:http://localhost:8080/sap/api?matchOnUriPrefix=true")
                .process(new Processor() {
                    @Override
                    public void process(Exchange exchange) throws Exception {
                        String data = TestUtils.getResourceValue(SAPNetweaverIntegrationTest.class, "/flight-data.json");
                        exchange.getOut().setBody(data);
                    }
                });
            }
        });

        camelctx.start();
        try {
            ProducerTemplate producer = camelctx.createProducerTemplate();
            String result = producer.requestBodyAndHeader("direct:start", null, NetWeaverConstants.COMMAND, SAP_COMMAND, String.class);
            Assert.assertTrue(result.contains("PRICE=422.94, CURRENCY=USD"));
        } finally {
            camelctx.stop();
        }
    }

    @Test
    public void testSAPNetweaverEndpointXmlResponse() throws Exception {
        CamelContext camelctx = new DefaultCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:start")
                    .to("sap-netweaver:http4://localhost:8080/sap/api?json=false");

                from("undertow:http://localhost:8080/sap/api?matchOnUriPrefix=true")
                    .process(new Processor() {
                        @Override
                        public void process(Exchange exchange) throws Exception {
                            String data = TestUtils.getResourceValue(SAPNetweaverIntegrationTest.class, "/flight-data.xml");
                            exchange.getOut().setBody(data);
                        }
                    });
            }
        });

        camelctx.start();
        try {
            ProducerTemplate producer = camelctx.createProducerTemplate();
            String result = producer.requestBodyAndHeader("direct:start", null, NetWeaverConstants.COMMAND, SAP_COMMAND, String.class);
            Assert.assertTrue(result.contains("<d:PRICE>422.94</d:PRICE>"));
            Assert.assertTrue(result.contains("<d:CURRENCY>USD</d:CURRENCY>"));
        } finally {
            camelctx.stop();
        }
    }
}
