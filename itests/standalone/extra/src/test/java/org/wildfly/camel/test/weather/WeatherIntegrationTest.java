/*
 * #%L
 * Wildfly Camel :: Testsuite
 * %%
 * Copyright (C) 2013 - 2014 RedHat
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

package org.wildfly.camel.test.weather;

import java.util.concurrent.TimeUnit;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.impl.DefaultCamelContext;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.extension.camel.CamelAware;

@CamelAware
@RunWith(Arquillian.class)
public class WeatherIntegrationTest {

    private static final String OPENWEATHER_APP_ID = System.getenv("OPENWEATHER_APP_ID");
    private static final String GEOLOCATION_ACCESS_KEY = System.getenv("GEOLOCATION_ACCESS_KEY");

    @Deployment
    public static JavaArchive createDeployment() {
        return ShrinkWrap.create(JavaArchive.class, "camel-weather-tests.jar");
    }

    @Test
    public void testGetWeatherWithDefinedLocation() throws Exception {
    	
        Assume.assumeNotNull(OPENWEATHER_APP_ID);

        try (CamelContext camelctx = new DefaultCamelContext()) {
        	
            camelctx.addRoutes(new RouteBuilder() {
                @Override
                public void configure() throws Exception {
                    from("weather:foo?location=Madrid,Spain&appid=" + OPENWEATHER_APP_ID)
                    .to("mock:result");
                }
            });
            
            camelctx.start();

            MockEndpoint mock = camelctx.getEndpoint("mock:result", MockEndpoint.class);
            mock.expectedMessageCount(1);

            MockEndpoint.assertIsSatisfied(20, TimeUnit.SECONDS, mock);

            Exchange exchange = mock.getExchanges().get(0);
            String body = exchange.getMessage().getBody(String.class);
            System.out.println(body);

            Assert.assertTrue("Contains ", body.contains(",\"name\":\"Madrid\","));
        }
    }

    @Test
    @Ignore("[CAMEL-15276] GeoLocationProvider may not get initialized properly [Target 3.4.1]")
    public void testGetWeatherFromGeoIpLocation() throws Exception {
    	
        Assume.assumeNotNull(OPENWEATHER_APP_ID, GEOLOCATION_ACCESS_KEY);

        try (CamelContext camelctx = new DefaultCamelContext()) {
        	
            camelctx.addRoutes(new RouteBuilder() {
                @Override
                public void configure() throws Exception {
                    from("weather:foo?geolocationRequestHostIP=redhat.com&geolocationAccessKey=" + GEOLOCATION_ACCESS_KEY + "&appid=" + OPENWEATHER_APP_ID)
                    .to("mock:result");
                }
            });
            
            camelctx.start();

            MockEndpoint mock = camelctx.getEndpoint("mock:result", MockEndpoint.class);
            mock.expectedMessageCount(1);

            MockEndpoint.assertIsSatisfied(20, TimeUnit.SECONDS, mock);

            Exchange exchange = mock.getExchanges().get(0);
            String body = exchange.getMessage().getBody(String.class);
            System.out.println(body);

            //Assert.assertTrue("Contains ", body.contains(",\"name\":\"Madrid\","));
        }
    }
}
