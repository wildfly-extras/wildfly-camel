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

/**
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 */
@ApplicationScoped
public class UndertowSecureRestDslCdiRoutes1 extends RouteBuilder {

    @Override
    public void configure() throws Exception {

        restConfiguration()
            .host("localhost")
            .port(8080)
            .component("undertow")
        ;

        rest()
            .get("/test")
                .route()
                   .setBody(constant("GET: /test"))
                .endRest()
        ;

    }
}
