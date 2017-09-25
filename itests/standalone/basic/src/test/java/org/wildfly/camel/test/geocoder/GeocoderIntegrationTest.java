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

import static com.google.code.geocoder.model.GeocoderStatus.OK;
import static com.google.code.geocoder.model.GeocoderStatus.OVER_QUERY_LIMIT;

import java.util.List;

import javax.naming.InitialContext;

import com.google.code.geocoder.model.GeocodeResponse;
import com.google.code.geocoder.model.GeocoderResult;
import com.google.code.geocoder.model.GeocoderStatus;
import com.google.code.geocoder.model.LatLng;

import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.geocoder.http.HttpClientConfigurer;
import org.apache.camel.impl.DefaultCamelContext;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.camel.test.geocoder.subA.GeocoderHttpClientConfigurer;
import org.wildfly.extension.camel.CamelAware;

@CamelAware
@RunWith(Arquillian.class)
public class GeocoderIntegrationTest {

    @ArquillianResource
    private InitialContext initialContext;

    @Deployment
    public static JavaArchive createDeployment() {
        return ShrinkWrap.create(JavaArchive.class, "camel-geocoder-tests.jar")
            .addClass(GeocoderHttpClientConfigurer.class);
    }

    @Test
    public void testGeocoderComponent() throws Exception {
        HttpClientConfigurer configurer = new GeocoderHttpClientConfigurer();
        initialContext.bind("httpClientConfigurer", configurer);

        CamelContext camelctx = new DefaultCamelContext();
        camelctx.addRoutes(new RouteBuilder() {

            @Override
            public void configure() throws Exception {
                from("direct:start")
                .to("geocoder:address:London, England?httpClientConfigurer=#httpClientConfigurer");
            }
        });

        camelctx.start();
        try {
            // Geocoder API is sometimes flaky so retry the request if necessary
            GeocodeResponse result = null;
            int count = 0;
            while (count < 5) {
                ProducerTemplate template = camelctx.createProducerTemplate();
                result = template.requestBody("direct:start", null, GeocodeResponse.class);
                Assert.assertNotNull("Geocoder response is null", result);

                // Skip further testing if we exceeded the API rate limit
                GeocoderStatus status = result.getStatus();
                Assume.assumeFalse("Geocoder API rate limit exceeded", status.equals(OVER_QUERY_LIMIT));

                if (status.equals(OK)) {
                    break;
                }

                Thread.sleep(1000);
                count++;
            }

            List<GeocoderResult> results = result.getResults();
            Assert.assertNotNull("Geocoder results is null", result);
            Assert.assertEquals("Expected 1 GeocoderResult to be returned", 1, results.size());

            LatLng location = results.get(0).getGeometry().getLocation();
            Assert.assertEquals("51.5073509", location.getLat().toPlainString());
            Assert.assertEquals("-0.1277583", location.getLng().toPlainString());
        } finally {
            camelctx.stop();
            initialContext.unbind("httpClientConfigurer");
        }
    }
}
