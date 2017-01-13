/*
 * #%L
 * Wildfly Camel :: Testsuite
 * %%
 * Copyright (C) 2013 - 2017 RedHat
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

package org.wildfly.camel.test.context.subA;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Named;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.impl.DefaultCamelContext;
import org.wildfly.camel.test.common.types.HelloBean;

@ApplicationScoped
@Named("greeting")
public class CamelContextA extends DefaultCamelContext {

    @PostConstruct
    public void init() {
        try {
            setName("contextA");
            addRoutes(new RouteBuilder() {
                @Override
                public void configure() throws Exception {
                    from("direct:hello")
                    .bean(HelloBean.class);
                }
            });
        } catch (Exception e) {
            throw new IllegalStateException("Error configuring camel routes", e);
        }
    }
}
