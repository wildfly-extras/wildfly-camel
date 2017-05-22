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

import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.jackson.JacksonDataFormat;
import org.apache.camel.component.rest.swagger.RestSwaggerComponent;
import org.apache.camel.converter.jaxb.JaxbDataFormat;
import org.apache.camel.impl.DefaultCamelContext;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.camel.test.rest.swagger.subA.Pet;
import org.wildfly.extension.camel.CamelAware;

@CamelAware
@RunWith(Arquillian.class)
public class RestSwaggerIntegrationTest {

    @Deployment
    public static JavaArchive createDeployment() {
        return ShrinkWrap.create(JavaArchive.class, "camel-rest-swagger-tests.jar")
            .addPackage(Pet.class.getPackage());
    }

    @Test
    public void testRestSwaggerXMLMarshal() throws Exception {
        JAXBContext jaxbContext = JAXBContext.newInstance(Pet.class);
        JaxbDataFormat jaxb = new JaxbDataFormat(jaxbContext);

        DefaultCamelContext camelctx = new DefaultCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:getPetById")
                .to("petstore:getPetById")
                .unmarshal(jaxb);
            }
        });

        RestSwaggerComponent petStoreComponent = new RestSwaggerComponent();
        petStoreComponent.setSpecificationUri(new URI("http://petstore.swagger.io/v2/swagger.json"));
        petStoreComponent.setComponentName("undertow");
        petStoreComponent.setConsumes(MediaType.APPLICATION_XML);
        petStoreComponent.setProduces(MediaType.APPLICATION_XML);

        camelctx.addComponent("petstore", petStoreComponent);

        camelctx.start();
        try {
            ProducerTemplate template = camelctx.createProducerTemplate();
            Pet pet = template.requestBodyAndHeader("direct:getPetById", null, "petId", "1", Pet.class);
            Assert.assertNotNull(pet);
            Assert.assertEquals(1, pet.getId().intValue());
        } finally {
            camelctx.stop();
        }
    }

    @Test
    public void testRestSwaggerJSONMarshal() throws Exception {
        JacksonDataFormat jacksonDataFormat = new JacksonDataFormat();
        jacksonDataFormat.setUnmarshalType(Pet.class);

        DefaultCamelContext camelctx = new DefaultCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:getPetById")
                .to("petstore:getPetById")
                .convertBodyTo(String.class)
                .unmarshal(jacksonDataFormat);
            }
        });

        RestSwaggerComponent petStoreComponent = new RestSwaggerComponent();
        petStoreComponent.setSpecificationUri(new URI("http://petstore.swagger.io/v2/swagger.json"));
        petStoreComponent.setComponentName("undertow");
        petStoreComponent.setConsumes(MediaType.APPLICATION_JSON);
        petStoreComponent.setProduces(MediaType.APPLICATION_JSON);

        camelctx.addComponent("petstore", petStoreComponent);

        camelctx.start();
        try {
            ProducerTemplate template = camelctx.createProducerTemplate();
            Pet pet = template.requestBodyAndHeader("direct:getPetById", null, "petId", "1", Pet.class);
            Assert.assertNotNull(pet);
            Assert.assertEquals(1, pet.getId().intValue());
        } finally {
            camelctx.stop();
        }
    }
}
