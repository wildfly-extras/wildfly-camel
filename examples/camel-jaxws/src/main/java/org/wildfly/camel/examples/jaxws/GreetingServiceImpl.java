/*
 * #%L
 * Wildfly Camel :: Example :: Camel JAX-WS
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
package org.wildfly.camel.examples.jaxws;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebService;

import org.apache.camel.CamelContext;
import org.apache.camel.builder.ProxyBuilder;

@WebService(serviceName="greeting", endpointInterface = "org.wildfly.camel.examples.jaxws.GreetingService")
public class GreetingServiceImpl {

    @Inject
    private CamelContext context;

    private GreetingService greetingService;

    /**
     * Configures a proxy for the direct:start endpoint
     */
    @PostConstruct
    public void initServiceProxy() throws Exception {
        greetingService = new ProxyBuilder(context).endpoint("direct:start").binding(false).build(GreetingService.class);
    }

    @WebMethod(operationName = "greet")
    public String greet(@WebParam(name = "name") String name) {
        /**
         * Invoke the proxied greet method and pass on the arguments we received
         */
        return greetingService.greet(name);
    }

    @WebMethod(operationName = "greetWithMessage")
    public String greetWithMessage(@WebParam(name = "message") String message, @WebParam(name = "name") String name) {
        /**
         * Invoke the proxied greetWithMessage method and pass on the arguments we received
         */
        return greetingService.greetWithMessage(message, name);
    }
}
