/*
 * #%L
 * Wildfly Camel :: Example :: Camel REST
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
package org.wildfly.camel.examples.rest;

import java.util.List;

import javax.ejb.Startup;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.bean.BeanInvocation;
import org.apache.camel.model.rest.RestBindingMode;
import org.wildfly.camel.examples.rest.data.CustomerRepository;
import org.wildfly.camel.examples.rest.model.Customer;
import org.wildfly.extension.camel.CamelAware;

@Startup
@CamelAware
@ApplicationScoped
public class RestConsumerRouteBuilder extends RouteBuilder {

    /**
     * Inject a service for interacting with the WildFly exampleDS in-memory database.
     */
    @Inject
    private CustomerRepository customerRepository;

    @Override
    public void configure() throws Exception {
        /**
         * Configure the Camel REST DSL to use the camel-servlet component for handling HTTP requests.
         *
         * Whenever a POST request is made to /customer it is accompanied with a JSON string representation
         * of a Customer object. Note that the binding mode is set to RestBindingMode.json. This will enable
         * Camel to unmarshal JSON to the desired object type.
         *
         * Note that the contextPath setting below has no effect on how the application server handles HTTP traffic.
         * The context root and required servlet mappings are configured in WEB-INF/jboss-web.xml and WEB-INF/web.xml.
         *
         */
        restConfiguration().component("servlet").contextPath("/camel-example-rest/camel").port(8080).bindingMode(RestBindingMode.json);

        /**
         * Handles requests to a base URL of /camel-example-rest/camel/customer
         */
        rest("/customer")
            /**
             * Handles GET requests to URLs such as /camel-example-rest/camel/customer/1
             */
            .get("/{id}")
                /**
                 * Marshalls the response to JSON
                 */
                .produces(MediaType.APPLICATION_JSON)
                .to("direct:readCustomer")
            /**
             * Handles POST requests to /camel-example-rest/camel/customer
             */
            .post()
                /**
                 * Unmarshalls the JSON data sent with the POST request to a Customer object.
                 */
                .type(Customer.class)
                .to("direct:createCustomer");

        /**
         * This route returns a JSON representation of any customers matching the id
         * that was sent with the GET request.
         *
         * If no customer was found, an HTTP 404 response code is returned to the calling client.
         */
        from("direct:readCustomer")
            .bean(customerRepository, "readCustomer(${header.id})")
            .choice()
                .when(simple("${body} == null"))
                    .setHeader(Exchange.HTTP_RESPONSE_CODE, constant(404));

        /**
         * This route handles persistence of new customers.
         */
        from("direct:createCustomer")
            .bean(customerRepository, "createCustomer");


        /**
         * This route handles REST requests that have been made to the RESTful services defined within
         * CustomerServiceImpl.
         *
         * These services are running under the WildFly RESTEasy JAX-RS subsystem. A CamelProxy proxies the direct:rest
         * route so that requests can be handled from within a Camel route.
         */
        from("direct:rest")
            .process(new Processor() {
                @Override
                public void process(Exchange exchange) throws Exception {
                    /**
                     * Retrieve the message payload. Since we are using camel-proxy to proxy the direct:rest
                     * endpoint the payload will be of type BeanInvocation.
                     */
                    BeanInvocation beanInvocation = exchange.getIn().getBody(BeanInvocation.class);

                    /**
                     * Get the invoked REST service method name and build a response to send
                     * back to the client.
                     */
                    String methodName = beanInvocation.getMethod().getName();

                    if (methodName.equals("getCustomers")) {
                        /**
                         * Retrieve all customers and send back a JSON response
                         */
                        List<Customer> customers = customerRepository.findAllCustomers();
                        exchange.getOut().setBody(Response.ok(customers).build());
                    } else if(methodName.equals("updateCustomer")) {
                        /**
                         * Get the customer that was sent on this method call
                         */
                        Customer updatedCustomer = (Customer) beanInvocation.getArgs()[0];
                        Customer existingCustomer = customerRepository.readCustomer(updatedCustomer.getId());

                        if(existingCustomer != null){
                            if(existingCustomer.equals(updatedCustomer)) {
                                /**
                                 * Nothing to be updated so return HTTP 304 - Not Modified.
                                 */
                                exchange.getOut().setBody(Response.notModified().build());
                            } else {
                                customerRepository.updateCustomer(updatedCustomer);
                                exchange.getOut().setBody(Response.ok().build());
                            }
                        } else {
                            /**
                             * No customer exists for the provided id, so return HTTP 404 - Not Found.
                             */
                            exchange.getOut().setBody(Response.status(Response.Status.NOT_FOUND).build());
                        }
                    } else if(methodName.equals("deleteCustomer")) {
                        Long customerId = (Long) beanInvocation.getArgs()[0];

                        Customer customer = customerRepository.readCustomer(customerId);
                        if(customer != null) {
                            customerRepository.deleteCustomer(customerId);
                            exchange.getOut().setBody(Response.ok().build());
                        } else {
                            /**
                             * No customer exists for the provided id, so return HTTP 404 - Not Found.
                             */
                            exchange.getOut().setBody(Response.status(Response.Status.NOT_FOUND).build());
                        }
                    } else if(methodName.equals("deleteCustomers")) {
                        customerRepository.deleteCustomers();

                        /**
                         * Return HTTP status OK.
                         */
                        exchange.getOut().setBody(Response.ok().build());
                    }
                }
            });
    }
}
