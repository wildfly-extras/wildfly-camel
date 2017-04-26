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
package org.wildfly.camel.test.json;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.jackson.JacksonDataFormat;
import org.apache.camel.impl.DefaultCamelContext;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.camel.test.json.subA.Employee;
import org.wildfly.camel.test.json.subA.Organization;
import org.wildfly.extension.camel.CamelAware;

@CamelAware
@RunWith(Arquillian.class)
public class JSONJacksonAnnotationsTest {

    @Deployment
    public static JavaArchive createDeployment() {
        JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "json-jackson-annotations-test.jar");
        archive.addPackage(Employee.class.getPackage());
        return archive;
    }

    @Test
    public void testJsonIgnore() throws Exception {
        DefaultCamelContext camelctx = new DefaultCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                JacksonDataFormat jacksonDataFormat = new JacksonDataFormat();
                jacksonDataFormat.setPrettyPrint(false);

                from("direct:start")
                .process(new Processor() {
                    @Override
                    public void process(Exchange exchange) throws Exception {
                        Organization organization = new Organization();
                        organization.setName("The Organization");

                        Employee employee = new Employee();
                        employee.setName("The Manager");
                        employee.setEmployeeNumber(12345);
                        employee.setOrganization(organization);
                        organization.setManager(employee);

                        exchange.getIn().setBody(employee);
                    }
                })
                .marshal(jacksonDataFormat);
            }
        });

        camelctx.start();
        try {
            ProducerTemplate template = camelctx.createProducerTemplate();
            String result = template.requestBody("direct:start", null, String.class);

            Assert.assertEquals("{\"name\":\"The Manager\"}", result);
        } finally {
            camelctx.stop();
        }
    }
}
