/*
 * #%L
 * Wildfly Camel :: Subsystem
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

package org.wildfly.extension.camel.security;

import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.model.ProcessorDefinition;
import org.apache.camel.spi.AuthorizationPolicy;
import org.apache.camel.spi.RouteContext;


/**
 * Provides access RunAs login policy
 *
 * @author Thomas.Diesler@jboss.com
 * @since 08-May-2015
 */
public class LoginPolicy implements AuthorizationPolicy {

    private final LoginContext loginContext;

    private LoginPolicy(LoginContext loginContext) {
        this.loginContext = loginContext;
    }

    public static LoginPolicy newLoginPolicy(String username, String password) throws LoginException {
        return new LoginPolicy(ClientLoginContext.newLoginContext(username, password));

    }
    @Override
    public void beforeWrap(RouteContext routeContext, ProcessorDefinition<?> definition) {
    }

    @Override
    public Processor wrap(final RouteContext routeContext, final Processor processor) {
        return new Processor() {
            @Override
            public void process(Exchange exchange) throws Exception {
                loginContext.login();
                try {
                    processor.process(exchange);
                } finally {
                    loginContext.logout();
                }
            }
        };
    }

}
