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

import javax.ejb.Startup;
import javax.enterprise.context.ApplicationScoped;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.bean.BeanInvocation;
import org.wildfly.extension.camel.CamelAware;

@Startup
@CamelAware
@ApplicationScoped
public class JaxwsRouteBuilder extends RouteBuilder {

    @Override
    public void configure() throws Exception {
        /**
         * This route uses the camel-proxy to mimic a JAX-WS consumer endpoint.
         *
         * The direct:start endpoint is proxied in {@link GreetingServiceImpl} whenever
         * any of the web service methods are invoked, it triggers the direct:start route to
         * run.
         *
         * A simple processor implements the logic of each WebMethod and returns a response to the
         * calling client.
         */
        from("direct:start")
        .process(new Processor() {
            @Override
            public void process(Exchange exchange) throws Exception {
                /**
                 * Retrieve the message payload. Since we are using camel-proxy to proxy the direct:start
                 * endpoint the payload will be of type BeanInvocation.
                 */
                BeanInvocation beanInvocation = exchange.getIn().getBody(BeanInvocation.class);

                /**
                 * Get the invoked web service method name and build a response to send
                 * back to the client.
                 */
                String methodName = beanInvocation.getMethod().getName();
                if(methodName.equals("greet")) {
                    /**
                     * When a method such as greet has a single argument, Camel regards this value as the message
                     * payload. Therefore, we do not need to use beanInvocation.getArgs like in the example below. We can
                     * simply retrieve the message body from the exchange.
                     */
                    String name = exchange.getIn().getBody(String.class);

                    /**
                     * Return a result back to the client.
                     */
                    exchange.getOut().setBody("Hello " + name);

                } else if(methodName.equals("greetWithMessage")) {
                    /**
                     * Retrieve the web service method argument values from the bean invocation.
                     */
                    String message = (String) beanInvocation.getArgs()[0];
                    String name = (String) beanInvocation.getArgs()[1];

                    /**
                     * Return a result back to the client.
                     */
                    exchange.getOut().setBody(message + " " + name);
                } else {
                    throw new IllegalStateException("Unknown method invocation " + methodName);
                }
            }
        });
    }
}
