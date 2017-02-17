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
package org.wildfly.camel.test.schematron;

import java.io.ByteArrayOutputStream;

import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.impl.DefaultCamelContext;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.gravia.utils.IOUtils;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.camel.test.common.utils.EnvironmentUtils;
import org.wildfly.extension.camel.CamelAware;

@CamelAware
@RunWith(Arquillian.class)
public class SchematronIntegrationTest {

    @Deployment
    public static JavaArchive createDeployment() {
        return ShrinkWrap.create(JavaArchive.class, "camel-schematron-tests.jar")
            .addAsResource("schematron/person.sch", "schematron/person.sch")
            .addAsResource("schematron/person.xsd", "schematron/person.xsd")
            .addAsResource("schematron/person-invalid.xml", "schematron/person-invalid.xml")
            .addAsResource("schematron/person-valid.xml", "schematron/person-valid.xml")
            .addClasses(EnvironmentUtils.class);
    }

    @Test
    public void testSchematronValidation() throws Exception {

        Assume.assumeFalse("[#1629] SchematronIntegrationTest fails on Windows", EnvironmentUtils.isWindows());
        
        CamelContext camelctx = new DefaultCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:start")
                .to("schematron://schematron/person.sch")
                .choice()
                    .when(simple("${in.header.CamelSchematronValidationStatus} == 'SUCCESS'"))
                        .setBody(constant("PASS"))
                    .otherwise()
                        .setBody(constant("FAIL"));
            }
        });

        camelctx.start();
        try {
            ProducerTemplate template = camelctx.createProducerTemplate();
            String valid = template.requestBody("direct:start", readResource("person-valid.xml"), String.class);
            String invalid = template.requestBody("direct:start", readResource("person-invalid.xml"), String.class);

            Assert.assertEquals(valid, "PASS");
            Assert.assertEquals(invalid, "FAIL");
        } finally {
            camelctx.stop();
        }
    }

    private String readResource(String resource) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        IOUtils.copyStream(getClass().getResourceAsStream("/schematron/" + resource), out);
        return new String(out.toByteArray());
    }
}
