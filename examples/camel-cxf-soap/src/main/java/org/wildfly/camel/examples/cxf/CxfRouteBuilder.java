/*
 * #%L
 * Wildfly Camel :: Example :: Camel CDI
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
package org.wildfly.camel.examples.cxf;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.cdi.ContextName;

import javax.ejb.Startup;
import javax.enterprise.context.ApplicationScoped;

@Startup
@ApplicationScoped
@ContextName("cdi-context")
public class CxfRouteBuilder extends RouteBuilder {

    @Override
    public void configure() throws Exception {
        /*
         * Route to expose GreetingService as a JAX-WS web service.
         *
         * Requests get sent to http://localhost:8080/example-camel-cxf-soap/greeting
         *
         * The recipient list routes the request to a 'direct:' route where the name matches
         * the web service operation name header. Each route returns the output from
         * whatever method was invoked on the GreetingServiceImpl class.
         */
        from("cxf:/greeting?serviceClass=" + GreetingService.class.getName())
                .recipientList(simple("direct:${header.operationName}"));

        from("direct:sayHello")
                .transform(simple("${out.body}"));

        from("direct:sayGoodbye")
                .transform(simple("${out.body}"));
    }
}
