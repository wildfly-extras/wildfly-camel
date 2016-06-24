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

import software.betamax.junit.Betamax;
import software.betamax.junit.RecorderRule;

import java.io.File;

import org.apache.camel.CamelContext;
import org.apache.camel.Endpoint;
import org.apache.camel.impl.DefaultCamelContext;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.extension.camel.CamelAware;

@CamelAware
@RunWith(Arquillian.class)
public class WeatherIntegrationTest {

    @Rule
    public RecorderRule recorder = new RecorderRule();

    @Deployment
    public static WebArchive createDeployment() throws Exception {
        File[] libraryDependencies = Maven.configureResolverViaPlugin().
            resolve("software.betamax:betamax-junit").
            withTransitivity().
            asFile();

        final WebArchive archive = ShrinkWrap.create(WebArchive.class, "camel-weather-tests.war");
        archive.addAsLibraries(libraryDependencies);
        archive.addAsResource("betamax.properties","betamax.properties");
        return archive;
    }

    @Test
    public void testComponentLoads() throws Exception {

        CamelContext camelctx = new DefaultCamelContext();
        Endpoint endpoint = camelctx.getEndpoint("weather:foo?location=Madrid,Spain&period=7 days");
        Assert.assertNotNull(endpoint);
        Assert.assertEquals(endpoint.getClass().getName(), "org.apache.camel.component.weather.WeatherEndpoint");
        camelctx.stop();
    }

    @Test
    @Betamax(tape="madrid-weather-report")
    public void testGetWeather() throws Exception {
        CamelContext camelctx = new DefaultCamelContext();
        String response = camelctx.createProducerTemplate().requestBody("weather:foo?location=Madrid,Spain&period=7 days", "").toString();
        Assert.assertNotNull(response);
        Assert.assertTrue("Contains ", response.contains(",\"name\":\"Madrid\","));
        camelctx.stop();
    }
}
