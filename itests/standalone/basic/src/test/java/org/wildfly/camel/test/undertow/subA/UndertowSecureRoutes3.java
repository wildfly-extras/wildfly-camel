/*
 * #%L
 * Wildfly Camel :: Testsuite
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
package org.wildfly.camel.test.undertow.subA;

import javax.enterprise.context.ApplicationScoped;

import org.apache.camel.builder.RouteBuilder;

/**
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 */
@ApplicationScoped
public class UndertowSecureRoutes3 extends RouteBuilder {

    @Override
    public void configure() throws Exception {

        from("undertow:http://localhost:8080/app3?httpMethodRestrict=GET").setBody(constant("GET: /app3"));
        from("undertow:http://localhost:8080/test?httpMethodRestrict=POST").setBody(constant("POST: /test"));

    }
}
