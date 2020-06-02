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

import java.security.Principal;

import javax.security.auth.Subject;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;

import org.apache.camel.Exchange;
import org.apache.camel.NamedNode;
import org.apache.camel.Processor;
import org.apache.camel.Route;
import org.apache.camel.spi.AuthorizationPolicy;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;


/**
 * An abstract authorization policy
 *
 * @author Thomas.Diesler@jboss.com
 * @since 28-Oct-2015
 */
public abstract class AbstractAuthorizationPolicy implements AuthorizationPolicy {

	@Override
	public void beforeWrap(Route route, NamedNode definition) {
	}

    @Override
    public Processor wrap(final Route route, final Processor processor) {
        return new Processor() {
            @Override
            public void process(Exchange exchange) throws Exception {
                Subject subject = exchange.getIn().getHeader(Exchange.AUTHENTICATION, Subject.class);
                if (subject == null) {
                    throw new SecurityException("Cannot obtain authentication subject from exchange: " + exchange);
                }
                String domain = null;
                String username = null;
                char[] password = null;
                for (Principal principal : subject.getPrincipals()) {
                    if (principal instanceof UsernamePasswordPrincipal) {
                        username = principal.getName();
                        password = ((UsernamePasswordPrincipal) principal).getPassword();
                    } else if (principal instanceof DomainPrincipal) {
                        domain = principal.getName();
                    } else if (principal instanceof UsernamePasswordAuthenticationToken) {
                        username = principal.getName();
                        Object credentials = ((UsernamePasswordAuthenticationToken) principal).getCredentials();
                        if (credentials instanceof String) {
                            password = ((String) credentials).toCharArray();
                        } else if (credentials instanceof char[]) {
                            password = (char[]) credentials;
                        }
                    }
                }
                if (username == null || password == null) {
                    throw new SecurityException("Cannot obtain credentials from exchange: " + exchange);
                }

                LoginContext context = getLoginContext(domain, username, password);
                context.login();
                try {
                    authorize(context);
                    processor.process(exchange);
                } finally {
                    context.logout();
                }
            }
        };
    }

    protected void authorize(LoginContext context) throws LoginException {
    }

    protected abstract LoginContext getLoginContext(String domain, String username, char[] password) throws LoginException;
}
