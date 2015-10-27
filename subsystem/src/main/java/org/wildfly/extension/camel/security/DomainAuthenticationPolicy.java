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

import java.util.HashSet;
import java.util.Set;

import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;

import org.wildfly.extension.camel.security.LoginContextBuilder.Type;


/**
 * Authenticates against a given domain
 *
 * @author Thomas.Diesler@jboss.com
 * @since 08-May-2015
 */
public class DomainAuthenticationPolicy extends AbstractAuthorizationPolicy {

    private final Set<String> roles = new HashSet<>();

    public DomainAuthenticationPolicy roles(String... roles) {
        for (String role : roles) {
            this.roles.add(role);
        }
        return this;
    }

    protected LoginContext getLoginContext(String domain, String username, char[] password) throws LoginException {
        String[] rolesArr = roles.toArray(new String[roles.size()]);
        LoginContextBuilder builder = new LoginContextBuilder(Type.AUTHENTICATION).domain(domain);
        return builder.username(username).password(password).roles(rolesArr).build();
    }
}
