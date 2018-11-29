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
package org.wildfly.camel.test.geocoder;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.geocoder.GeoCoderConstants;
import org.apache.camel.component.geocoder.GeocoderStatus;
import org.apache.camel.impl.DefaultCamelContext;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.extension.camel.CamelAware;

@CamelAware
@RunWith(Arquillian.class)
public class GeocoderIntegrationTest {

    private static final String GEOCODER_API_KEY = System.getenv("GEOCODER_API_KEY");

    @Deployment
    public static JavaArchive createDeployment() {
        return ShrinkWrap.create(JavaArchive.class, "camel-geocoder-tests.jar");
    }

    @Before
    public void setUp() {
        Assume.assumeNotNull("GEOCODER_API_KEY is NULL", GEOCODER_API_KEY);
    }

    @Test
    public void testGeocoderComponent() throws Exception {
        CamelContext camelctx = new DefaultCamelContext();
        camelctx.addRoutes(new RouteBuilder() {

            @Override
            public void configure() throws Exception {
                from("direct:start")
                .to("geocoder:address:London, England?apiKey=" + GEOCODER_API_KEY);
            }
        });

        camelctx.start();
        try {
            // Geocoder API is sometimes flaky so retry the request if necessary
            Exchange exchange = null;
            int count = 0;
            while (count < 5) {
                ProducerTemplate template = camelctx.createProducerTemplate();
                exchange = template.request("direct:start", null);
                Assert.assertNotNull("Exchange is null", exchange);

                GeocoderStatus status = exchange.getIn().getHeader(GeoCoderConstants.STATUS, GeocoderStatus.class);
                Assert.assertNotNull("Geocoder status is null", status);

                if (status.equals(GeocoderStatus.OK)) {
                    break;
                }

                Thread.sleep(1000);
                count++;
            }
            Assert.assertNotNull("Gave up attempting to get successful Geocoder API response", exchange);

            String latitude = exchange.getIn().getHeader(GeoCoderConstants.LAT, String.class);
            Assert.assertNotNull("Geocoder " + GeoCoderConstants.LAT + " header is null", latitude);

            String longitude = exchange.getIn().getHeader(GeoCoderConstants.LNG, String.class);
            Assert.assertNotNull("Geocoder " + GeoCoderConstants.LNG + " header is null", latitude);

            Assert.assertEquals("51.50735090", latitude);
            Assert.assertEquals("-0.12775830", longitude);
        } finally {
            camelctx.stop();
        }
    }
}
