/*
 * #%L
 * Wildfly Camel :: Testsuite
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
package org.wildfly.camel.test.switchyard.subA;

import org.apache.camel.builder.RouteBuilder;

public class JavaDSLBuilder extends RouteBuilder {

    public void configure() {
        from("switchyard://JavaDSL")
            .log("Message received in Java DSL Route")
            .log("${body}")
            .split(body(String.class).tokenize("\n"))
            .filter()
            .groovy("request.getBody().startsWith('sally:')")
            .transform().javaScript("request.getBody().substring(6, request.getBody().length())")
            .to("switchyard://XMLService?operationName=acceptMessage");
    }
}