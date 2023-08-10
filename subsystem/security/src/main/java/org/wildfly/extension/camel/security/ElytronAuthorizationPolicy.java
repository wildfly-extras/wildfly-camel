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

import org.apache.camel.Exchange;
import org.apache.camel.NamedNode;
import org.apache.camel.Processor;
import org.apache.camel.Route;
import org.apache.camel.spi.AuthorizationPolicy;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.wildfly.security.auth.server.SecurityDomain;
import org.wildfly.security.auth.server.SecurityIdentity;
import org.wildfly.security.authz.Roles;
import org.wildfly.security.evidence.PasswordGuessEvidence;

import javax.security.auth.Subject;
import javax.security.auth.login.LoginException;
import java.security.Principal;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Authorization policy compatible with Elytron security. Checks if current user has required roles in order to be able
 * to run a Camel route. Alternatively, one can use a specific user instead of the currently logged one.
 */
public class ElytronAuthorizationPolicy implements AuthorizationPolicy {

    private Set<String> requiredRoles;


    public ElytronAuthorizationPolicy() {
        requiredRoles = new HashSet<>();
    }


    public ElytronAuthorizationPolicy roles(String... roles) {
        requiredRoles.addAll(Arrays.asList(roles));
        return this;
    }

    // for use in spring xml
    public void setRole(String role) {
        requiredRoles.add(role);
    }

    @Override
    public void beforeWrap(Route route, NamedNode definition) {
        // no code
    }

    @Override
    public Processor wrap(Route route, Processor processor) {
        return new Processor() {
            @Override
            public void process(Exchange exchange) throws Exception {
                SecurityDomain securityDomain = SecurityDomain.getCurrent();
                SecurityIdentity securityIdentity;

                Subject subject = exchange.getIn().getHeader(Exchange.AUTHENTICATION, Subject.class);
                if (subject == null) {
                    // use currently logged user
                    securityIdentity = securityDomain.getCurrentSecurityIdentity();
                } else {
                    // use user specified in the exchange
                    UsernamePasswordPrincipal credentials = getCredentials(subject);
                    String username = credentials.getName();
                    char[] password = credentials.getPassword();
                    securityIdentity = securityDomain.authenticate(username, new PasswordGuessEvidence(password));
                }

                checkRequiredRoles(securityIdentity.getRoles());
                processor.process(exchange);
            }
        };
    }

    private UsernamePasswordPrincipal getCredentials(Subject subject) {
        String username = null;
        char[] password = null;
        for (Principal principal : subject.getPrincipals()) {
            if (principal instanceof UsernamePasswordPrincipal) {
                UsernamePasswordPrincipal p = (UsernamePasswordPrincipal) principal;
                username = p.getName();
                password = p.getPassword();
                break;
            }
            if (principal instanceof UsernamePasswordAuthenticationToken) {
                UsernamePasswordAuthenticationToken p = (UsernamePasswordAuthenticationToken) principal;
                username = p.getName();
                Object credentials = p.getCredentials();
                if (credentials instanceof String) {
                    password = ((String) credentials).toCharArray();
                } else if (credentials instanceof char[]) {
                    password = (char[]) credentials;
                }
                break;
            }
        }
        if (username == null || password == null) {
            throw new SecurityException("Cannot obtain credentials from exchange");
        }
        return new UsernamePasswordPrincipal(username, password);
    }

    private void checkRequiredRoles(Roles roles) throws LoginException {
        for (String role : requiredRoles) {
            if (!roles.contains(role)) {
                throw new LoginException("User does not have required roles: " + role);
            }
        }
    }

}