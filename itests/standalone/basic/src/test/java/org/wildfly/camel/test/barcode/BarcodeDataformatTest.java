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
package org.wildfly.camel.test.barcode;

import java.io.BufferedInputStream;
import java.io.File;

import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.dataformat.barcode.BarcodeDataFormat;
import org.apache.camel.impl.DefaultCamelContext;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.extension.camel.CamelAware;

@CamelAware
@RunWith(Arquillian.class)
public class BarcodeDataformatTest {

    @Deployment
    public static JavaArchive createDeployment() {
        return ShrinkWrap.create(JavaArchive.class, "camel-barcode-tests.jar")
            .addAsResource("barcode/barcode.png", "barcode.png");
    }

    @Test
    public void testBarcodeMarshal() throws Exception {
        CamelContext camelctx = new DefaultCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:start")
                .marshal(new BarcodeDataFormat())
                .to("file://target?fileName=barcode.png");
            }
        });

        camelctx.start();
        try {
            ProducerTemplate template = camelctx.createProducerTemplate();
            template.requestBody("direct:start", "Barcode Test Content");

            File file = new File("target/barcode.png");
            Assert.assertTrue(file.exists());
            Assert.assertTrue(file.length() > 0);
        } finally {
            camelctx.stop();
        }
    }

    @Test
    public void testBarcodeUnmarshal() throws Exception {
        CamelContext camelctx = new DefaultCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:start")
                .unmarshal(new BarcodeDataFormat());
            }
        });

        camelctx.start();
        try (BufferedInputStream bis = new BufferedInputStream(getClass().getClassLoader().getResourceAsStream("barcode.png"))) {

            ProducerTemplate template = camelctx.createProducerTemplate();
            String result = template.requestBody("direct:start", bis, String.class);

            Assert.assertEquals("Barcode Test Content", result);
        } finally {
            camelctx.stop();
        }
    }


}
