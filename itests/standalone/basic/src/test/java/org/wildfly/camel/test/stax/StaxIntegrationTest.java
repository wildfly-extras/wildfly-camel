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

package org.wildfly.camel.test.stax;

import java.io.InputStream;

import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.impl.DefaultCamelContext;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.camel.test.common.types.Record;
import org.wildfly.camel.test.common.types.Records;
import org.wildfly.camel.test.common.utils.ManifestBuilder;
import org.wildfly.camel.test.stax.subA.ElementCountingHandler;
import org.wildfly.extension.camel.CamelAware;

import static org.apache.camel.component.stax.StAXBuilder.stax;

@CamelAware
@RunWith(Arquillian.class)
public class StaxIntegrationTest {

    private static final String RECORDS_XML = "records.xml";

    @Deployment
    public static JavaArchive createDeployment() {
        return ShrinkWrap.create(JavaArchive.class, "camel-stax-tests.jar")
            .addClasses(ElementCountingHandler.class, Record.class, Records.class)
            .addAsResource("stax/" + RECORDS_XML, RECORDS_XML)
            .setManifest(() -> {
                ManifestBuilder builder = new ManifestBuilder();
                builder.addManifestHeader("Dependencies", "org.jdom2");
                return builder.openStream();
            });
    }

    @Test
    public void testStaxHandler() throws Exception {
        CamelContext camelctx = new DefaultCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:start")
                .toF("stax:%s", ElementCountingHandler.class.getName());
            }
        });

        camelctx.start();
        try (InputStream input = getClass().getResourceAsStream("/" + RECORDS_XML)) {
            ProducerTemplate template = camelctx.createProducerTemplate();
            ElementCountingHandler handler = template.requestBody("direct:start", input, ElementCountingHandler.class);
            Assert.assertEquals(6, handler.getCount());
        } finally {
            camelctx.stop();
        }
    }

    @Test
    public void testStaxJAXBSplit() throws Exception {
        CamelContext camelctx = new DefaultCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:start")
                .split(stax(Record.class)).streaming()
                .to("mock:result");
            }
        });

        MockEndpoint mockEndpoint = camelctx.getEndpoint("mock:result", MockEndpoint.class);
        mockEndpoint.expectedMessageCount(5);
        mockEndpoint.allMessages().body().isInstanceOf(Record.class);

        camelctx.start();
        try (InputStream input = getClass().getResourceAsStream("/" + RECORDS_XML)) {
            ProducerTemplate template = camelctx.createProducerTemplate();
            template.sendBody("direct:start", input);
            mockEndpoint.assertIsSatisfied();
        } finally {
            camelctx.stop();
        }
    }
}
