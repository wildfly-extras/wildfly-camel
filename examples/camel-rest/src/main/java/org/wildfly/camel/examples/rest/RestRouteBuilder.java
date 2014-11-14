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

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.cdi.ContextName;
import org.apache.camel.component.bean.BeanInvocation;

import javax.ejb.Startup;
import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.core.Response;

@Startup
@ApplicationScoped
@ContextName("rest-context")
public class RestRouteBuilder extends RouteBuilder {

    @Override
    public void configure() throws Exception {
        from("direct:start")
            .to("log:input?showAll=true")
            .process(new Processor() {
                @Override
                public void process(Exchange exchange) throws Exception {
                    BeanInvocation beanInvocation = exchange.getIn().getBody(BeanInvocation.class);
                    String name = (String) beanInvocation.getArgs()[0];
                    exchange.getOut().setBody(Response.ok().entity("Hello " + name).build());
                }
            });
    }
}
