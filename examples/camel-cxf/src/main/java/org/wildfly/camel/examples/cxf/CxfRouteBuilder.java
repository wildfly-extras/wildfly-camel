/*
 * #%L
 * Wildfly Camel :: Example :: Camel CXF
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

import javax.ejb.Startup;
import javax.enterprise.context.ApplicationScoped;

import org.apache.camel.builder.RouteBuilder;
import org.wildfly.extension.camel.CamelAware;

@Startup
@CamelAware
@ApplicationScoped
public class CxfRouteBuilder extends RouteBuilder {

    @Override
    public void configure() throws Exception {
        /**
         * This route defines a CXF producer endpoint which invokes a greeting web service.
         *
         * The {@link CamelCxfServlet} sends the required message and name parameters to the
         * service by populating them within the message exchange body.
         */
        from("direct:start")
            .to("cxf://http://localhost:8080/example-camel-cxf/greeting?serviceClass=" +
                    GreetingService.class.getName());
    }
}
