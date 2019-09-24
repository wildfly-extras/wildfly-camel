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

package org.wildfly.camel.test.olingo2;

import java.util.List;

import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.olingo2.Olingo2Component;
import org.apache.camel.component.olingo2.Olingo2Configuration;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.olingo.odata2.api.ep.entry.ODataEntry;
import org.apache.olingo.odata2.api.ep.feed.ODataFeed;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.camel.test.common.utils.ManifestBuilder;
import org.wildfly.camel.test.olingo2.subA.Olingo2TestServlet;
import org.wildfly.extension.camel.CamelAware;

@CamelAware
@RunWith(Arquillian.class)
public class Olingo2IntegrationTest {

    private static final String OLINGO_SERVICE_URI = "http://localhost:8080/olingo-server/MyODataSample.svc/";

    @Deployment(testable = false, order = 1, name = "olingo-server.war")
    public static WebArchive createOlingoServerDeployment() {
        final WebArchive archive = ShrinkWrap.create(WebArchive.class, "olingo-server.war");
        archive.addPackage(Olingo2TestServlet.class.getPackage());
        archive.setManifest(() -> {
            ManifestBuilder builder = new ManifestBuilder();
            builder.addManifestHeader("Dependencies", "org.apache.cxf.ext,org.apache.olingo2");
            return builder.openStream();
        });
        return archive;
    }

    @Deployment(order = 2)
    public static JavaArchive createDeployment() {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "camel-olingo2-tests");
        return archive;
    }

    @Test
    public void testOlingo2Read() throws Exception {
        CamelContext camelctx = new DefaultCamelContext();

        Olingo2Configuration configuration = new Olingo2Configuration();
        configuration.setServiceUri(OLINGO_SERVICE_URI);

        Olingo2Component component = new Olingo2Component();
        component.setConfiguration(configuration);

        camelctx.addComponent("olingo2", component);
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:start")
                .to("olingo2://read/Cars");
            }
        });

        camelctx.start();
        try {
            ProducerTemplate template = camelctx.createProducerTemplate();
            ODataFeed carEntries = template.requestBody("direct:start", null, ODataFeed.class);

            Assert.assertNotNull(carEntries);

            List<ODataEntry> cars = carEntries.getEntries();
            Assert.assertFalse(cars.isEmpty());
        } finally {
            camelctx.close();
        }
    }

}
