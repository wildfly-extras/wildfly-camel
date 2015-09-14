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

package org.wildfly.camel.test.jasypt;

import java.io.IOException;

import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.jasypt.JasyptPropertiesParser;
import org.apache.camel.component.properties.PropertiesComponent;
import org.apache.camel.impl.DefaultCamelContext;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(Arquillian.class)
public class JasyptIntegrationTest {

    @Deployment
    public static JavaArchive createdeployment() throws IOException {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "camel-jasypt-tests");
        archive.addAsResource("jasypt/password.properties", "password.properties");
        return archive;
    }

    @Test
    public void testPasswordDecryption() throws Exception {

        // create the jasypt properties parser
        JasyptPropertiesParser jasypt = new JasyptPropertiesParser();
        // and set the master password
        jasypt.setPassword("secret");

        // create the properties component
        PropertiesComponent pc = new PropertiesComponent();
        pc.setLocation("classpath:password.properties");
        // and use the jasypt properties parser so we can decrypt values
        pc.setPropertiesParser(jasypt);

        CamelContext camelctx = new DefaultCamelContext();
        camelctx.addComponent("properties", pc);
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:start").transform().simple("Hi ${body} the decrypted password is: ${properties:cool.password}");
            }
        });

        camelctx.start();
        try {
            ProducerTemplate producer = camelctx.createProducerTemplate();
            String result = producer.requestBody("direct:start", "John", String.class);
            Assert.assertEquals("Hi John the decrypted password is: tiger", result.trim());
        } finally {
            camelctx.stop();
        }
    }
}
