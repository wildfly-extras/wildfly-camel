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

import java.io.File;
import java.io.IOException;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.freeside.betamax.Betamax;
import co.freeside.betamax.Recorder;

@RunWith(Arquillian.class)
public class WeatherTest {

    protected Logger log = LoggerFactory.getLogger(WeatherTest.class);

    @Rule
    public Recorder recorder = new Recorder();

    @Deployment
    public static WebArchive createDeployment() throws IOException {
        File[] betamaxDependencies = Maven.configureResolverViaPlugin().
                resolve("co.freeside:betamax", "org.codehaus.groovy:groovy-all").
                withTransitivity().
                asFile();

        final WebArchive archive = ShrinkWrap.create(WebArchive.class, "camel-weather-tests.war");
        archive.addAsLibraries(betamaxDependencies);
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
        // response should look somethign like: {"cod":"200","message":0.1646,"city":{"id":3117735,"name":"Madrid","coord":{"lon":-3.70256,"lat":40.4165},"country":"ES","population":0,"sys":{"population":0}},"cnt":7,"list":[{"dt":1418040000,"temp":{"day":274.72,"min":268.97,"max":274.72,"night":268.97,"eve":274.72,"morn":274.72},"pressure":967.72,"humidity":78,"weather":[{"id":800,"main":"Clear","description":"sky is clear","icon":"01n"}],"speed":3.01,"deg":314,"clouds":0},{"dt":1418126400,"temp":{"day":280.79,"min":269.82,"max":280.79,"night":270.42,"eve":276.82,"morn":269.82},"pressure":970.31,"humidity":67,"weather":[{"id":801,"main":"Clouds","description":"few clouds","icon":"02d"}],"speed":5.36,"deg":354,"clouds":12},{"dt":1418212800,"temp":{"day":280.7,"min":268.53,"max":282.41,"night":269.17,"eve":275.56,"morn":268.53},"pressure":970.35,"humidity":81,"weather":[{"id":800,"main":"Clear","description":"sky is clear","icon":"01d"}],"speed":1.86,"deg":240,"clouds":0},{"dt":1418299200,"temp":{"day":276.78,"min":270,"max":278.96,"night":270.83,"eve":273.24,"morn":271.64},"pressure":965.53,"humidity":93,"weather":[{"id":803,"main":"Clouds","description":"broken clouds","icon":"04d"}],"speed":1.61,"deg":138,"clouds":76},{"dt":1418385600,"temp":{"day":277.49,"min":272.5,"max":280.18,"night":280.18,"eve":277.86,"morn":272.5},"pressure":958.92,"humidity":100,"weather":[{"id":500,"main":"Rain","description":"light rain","icon":"10d"}],"speed":3.11,"deg":230,"clouds":80,"rain":2},{"dt":1418472000,"temp":{"day":280.86,"min":269.62,"max":280.86,"night":269.62,"eve":274.97,"morn":278.33},"pressure":946.08,"humidity":0,"weather":[{"id":500,"main":"Rain","description":"light rain","icon":"10d"}],"speed":2.04,"deg":356,"clouds":30,"rain":1.76},{"dt":1418558400,"temp":{"day":279.82,"min":268.72,"max":279.82,"night":272.39,"eve":277.54,"morn":268.72},"pressure":955.3,"humidity":0,"weather":[{"id":800,"main":"Clear","description":"sky is clear","icon":"01d"}],"speed":4.46,"deg":13,"clouds":0}]}
        Assert.assertTrue("Contains ", response.contains(",\"name\":\"Madrid\","));
        camelctx.stop();
    }
}
