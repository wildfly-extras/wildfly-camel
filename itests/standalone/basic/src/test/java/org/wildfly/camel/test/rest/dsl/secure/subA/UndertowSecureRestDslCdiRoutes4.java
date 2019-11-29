/*
 * #%L
 * Wildfly Camel :: Example :: Camel CXF JAX-WS CDI Secure
 * %%
 * Copyright (C) 2013 - 2018 RedHat
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
package org.wildfly.camel.test.rest.dsl.secure.subA;

import javax.enterprise.context.ApplicationScoped;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.rest.RestBindingMode;

/**
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 */
@ApplicationScoped
public class UndertowSecureRestDslCdiRoutes4 extends RouteBuilder {

    @Override
    public void configure() throws Exception {

        restConfiguration()
            .host("localhost")
            .port(8080)
            .component("undertow")
            .contextPath("/api")
            .apiProperty("api.title", "WildFly Camel REST API")
            .apiProperty("api.version", "1.0")
            .apiContextPath("swagger");
        ;

        rest()
            .get("/endpoint1")
                .bindingMode(RestBindingMode.auto)
                .id("endpoint1")
                .description("A test endpoint1")
                .outType(String.class)
                .route()
                   .setBody(constant("GET: /api/endpoint1"))
                .endRest()
        ;

    }
}
