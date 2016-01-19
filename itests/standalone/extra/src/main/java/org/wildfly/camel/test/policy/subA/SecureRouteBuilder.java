/*
 * #%L
 * Wildfly Camel :: Example :: Camel ActiveMQ
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
package org.wildfly.camel.test.policy.subA;

import javax.ejb.Startup;
import javax.enterprise.context.ApplicationScoped;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.impl.ExplicitCamelContextNameStrategy;
import org.wildfly.extension.camel.CamelAware;
import org.wildfly.extension.camel.security.DomainAuthorizationPolicy;

@Startup
@CamelAware
@ApplicationScoped
public class SecureRouteBuilder extends RouteBuilder {

    @Override
    public void configure() throws Exception {
        getContext().setNameStrategy(new ExplicitCamelContextNameStrategy("secured-context"));
        from("direct:start")
        .policy(new DomainAuthorizationPolicy().roles("Role2"))
        .transform(body().prepend("Hello "));
    }
}
