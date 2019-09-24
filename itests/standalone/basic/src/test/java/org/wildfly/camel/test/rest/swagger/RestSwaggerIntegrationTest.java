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
package org.wildfly.camel.test.rest.swagger;

import java.net.URI;

import javax.ws.rs.core.MediaType;
import javax.xml.bind.JAXBContext;

import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.jackson.JacksonDataFormat;
import org.apache.camel.component.rest.swagger.RestSwaggerComponent;
import org.apache.camel.converter.jaxb.JaxbDataFormat;
import org.apache.camel.impl.DefaultCamelContext;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.camel.test.rest.swagger.subA.Customer;
import org.wildfly.camel.test.rest.swagger.subA.RestRouteBuilder;
import org.wildfly.extension.camel.CamelAware;

@CamelAware
@RunWith(Arquillian.class)
public class RestSwaggerIntegrationTest {

    @Deployment
    public static WebArchive createDeployment() {
        return ShrinkWrap.create(WebArchive.class, "RestSwaggerIntegrationTest.war")
            .addClass(Customer.class);
    }

    @Deployment(testable = false, name = "RestSwaggerIntegrationTest-endpoints.war")
    public static WebArchive restServiceDeployment() {
        return ShrinkWrap.create(WebArchive.class, "RestSwaggerIntegrationTest-endpoints.war")
            .addPackage(RestRouteBuilder.class.getPackage())
            .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
    }

    @Test
    public void testRestSwaggerXML() throws Exception {
        JAXBContext jaxbContext = JAXBContext.newInstance(Customer.class);
        JaxbDataFormat jaxb = new JaxbDataFormat(jaxbContext);

        CamelContext camelctx = new DefaultCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:getCustomerById")
                .to("customer:getCustomerById")
                .unmarshal(jaxb);
            }
        });

        RestSwaggerComponent restSwaggerComponent = new RestSwaggerComponent();
        restSwaggerComponent.setSpecificationUri(new URI("http://localhost:8080/api/swagger"));
        restSwaggerComponent.setComponentName("undertow");
        restSwaggerComponent.setConsumes(MediaType.APPLICATION_XML);
        restSwaggerComponent.setProduces(MediaType.APPLICATION_XML);

        camelctx.addComponent("customer", restSwaggerComponent);

        camelctx.start();
        try {
            ProducerTemplate template = camelctx.createProducerTemplate();
            Customer customer = template.requestBodyAndHeader("direct:getCustomerById", null, "id", 1, Customer.class);
            Assert.assertNotNull(customer);
            Assert.assertEquals(1, customer.getId());
        } finally {
            camelctx.close();
        }
    }

    @Test
    public void testRestSwaggerJSON() throws Exception {
        JacksonDataFormat jacksonDataFormat = new JacksonDataFormat();
        jacksonDataFormat.setUnmarshalType(Customer.class);

        CamelContext camelctx = new DefaultCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:getCustomerById")
                .to("customer:getCustomerById")
                .convertBodyTo(String.class)
                .unmarshal(jacksonDataFormat);
            }
        });

        RestSwaggerComponent restSwaggerComponent = new RestSwaggerComponent();
        restSwaggerComponent.setSpecificationUri(new URI("http://localhost:8080/api/swagger"));
        restSwaggerComponent.setComponentName("undertow");
        restSwaggerComponent.setConsumes(MediaType.APPLICATION_JSON);
        restSwaggerComponent.setProduces(MediaType.APPLICATION_JSON);

        camelctx.addComponent("customer", restSwaggerComponent);

        camelctx.start();
        try {
            ProducerTemplate template = camelctx.createProducerTemplate();
            Customer customer = template.requestBodyAndHeader("direct:getCustomerById", null, "id", 1, Customer.class);
            Assert.assertNotNull(customer);
            Assert.assertEquals(1, customer.getId());
        } finally {
            camelctx.close();
        }
    }
}
