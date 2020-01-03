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

import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.impl.DefaultCamelContext;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Assume;
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
    public void testGetWeatherWithDefinedLocation() {
        Assume.assumeNotNull(OPENWEATHER_APP_ID);

        CamelContext camelctx = new DefaultCamelContext();
        try {
            camelctx.start();

            ProducerTemplate template = camelctx.createProducerTemplate();
            String response = template.requestBody("weather:foo?location=Madrid,Spain&period=7 days&appid=" + OPENWEATHER_APP_ID, null, String.class);
            Assert.assertNotNull(response);
            Assert.assertTrue("Contains ", response.contains(",\"name\":\"Madrid\","));
        } finally {
            camelctx.stop();
        }
    }

    @Test
    public void testGetWeatherFromGeoIpLocation() {
        Assume.assumeNotNull(OPENWEATHER_APP_ID, GEOLOCATION_ACCESS_KEY);

        CamelContext camelctx = new DefaultCamelContext();
        try {
            camelctx.start();

            ProducerTemplate template = camelctx.createProducerTemplate();
            String response = template.requestBody("weather:foo?geolocationRequestHostIP=redhat.com&geolocationAccessKey=" + GEOLOCATION_ACCESS_KEY + "&appid=" + OPENWEATHER_APP_ID, null, String.class);
            Assert.assertNotNull(response);
            Assert.assertTrue("Contains ", response.contains(",\"weather\":"));
        } finally {
            camelctx.stop();
        }
    }
}
