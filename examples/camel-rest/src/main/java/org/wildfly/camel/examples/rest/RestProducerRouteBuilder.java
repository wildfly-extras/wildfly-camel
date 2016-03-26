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

import javax.ejb.Startup;
import javax.enterprise.context.ApplicationScoped;

import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.wildfly.extension.camel.CamelAware;

@Startup
@CamelAware
@ApplicationScoped
public class RestProducerRouteBuilder extends RouteBuilder {

    @Override
    public void configure() throws Exception {
        /**
         * This route demonstrates a JAX-RS producer using the camel-restlet component.
         *
         * Every 30 seconds, a call is made to the REST API for retrieving all customers at
         * the URL http://localhost:8080/example-camel-rest/rest/customer.
         *
         * The results of the REST service call are written to a file at:
         *
         * JBOSS_HOME/standalone/data/customer-records/customers.json
         */
        from("timer://outputCustomers?period=30000")
        .to("restlet://http://localhost:8080/example-camel-rest/rest/customer")
        .choice()
            .when(simple("${header.CamelHttpResponseCode} == 200"))
                .log("Updating customers.json")
                .setHeader(Exchange.FILE_NAME, constant("customers.json"))
                .to("file:{{jboss.server.data.dir}}/customer-records/")
            .otherwise()
                .log("REST request failed. HTTP status ${header.CamelHttpResponseCode}");
    }
}
