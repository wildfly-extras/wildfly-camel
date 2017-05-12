/*
 * #%L
 * Wildfly Camel :: Example :: Camel Rest Swagger
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
package org.wildfly.camel.examples.rest;

import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.core.MediaType;

import com.fasterxml.jackson.core.JsonParseException;

import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.cdi.ContextName;
import org.apache.camel.model.rest.RestBindingMode;
import org.apache.camel.model.rest.RestParamType;
import org.wildfly.camel.examples.rest.model.Customer;
import org.wildfly.camel.examples.rest.service.CustomerService;

@ApplicationScoped
@ContextName("camel-rest-context")
public class RestRouteBuilder extends RouteBuilder {

    public void configure() throws Exception {

        /**
         * Configure an error handler to trap instances where the data posted to
         * the REST API is invalid
         */
        onException(JsonParseException.class)
            .handled(true)
            .setHeader(Exchange.HTTP_RESPONSE_CODE, constant(400))
            .setHeader(Exchange.CONTENT_TYPE, constant(MediaType.TEXT_PLAIN))
            .setBody().constant("Invalid json data");

        /**
         * Configure Camel REST to use the camel-undertow component
         */
        restConfiguration()
            .bindingMode(RestBindingMode.json)
            .component("undertow")
            .contextPath("rest/api")
            .host("localhost")
            .port(8080)
            .enableCORS(true)
            .apiProperty("api.title", "WildFly Camel REST API")
            .apiProperty("api.version", "1.0")
            .apiContextPath("swagger");

        /**
         * Configure REST API with a base path of /customers
         */
        rest("/customers").description("Customers REST service")
            .get()
                .description("Retrieves all customers")
                .produces(MediaType.APPLICATION_JSON)
                .route()
                    .bean(CustomerService.class, "findAll")
                .endRest()

            .get("/{id}")
                .description("Retrieves a customer for the specified id")
                .param()
                    .name("id")
                    .description("Customer ID")
                    .type(RestParamType.path)
                    .dataType("int")
                .endParam()
                .produces(MediaType.APPLICATION_JSON)
                .route()
                    .bean(CustomerService.class, "findById")
                .endRest()

            .post()
                .description("Creates a new customer")
                .consumes(MediaType.APPLICATION_JSON)
                .produces(MediaType.APPLICATION_JSON)
                .type(Customer.class)
                .route()
                    .bean(CustomerService.class, "create")
                .endRest()

            .put("/{id}")
                .description("Updates the customer relating to the specified id")
                .param()
                    .name("id")
                    .description("Customer ID")
                    .type(RestParamType.path)
                    .dataType("int")
                .endParam()
                .consumes(MediaType.APPLICATION_JSON)
                .type(Customer.class)
                .route()
                    .bean(CustomerService.class, "update")
                .endRest()

            .delete("/{id}")
                .description("Deletes the customer relating to the specified id")
                .param()
                    .name("id")
                    .description("Customer ID")
                    .type(RestParamType.path)
                    .dataType("int")
                .endParam()
                .route()
                    .bean(CustomerService.class, "delete")
                .endRest();
    }
}
