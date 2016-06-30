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

import java.security.acl.Group;
import java.util.HashSet;
import java.util.Set;

import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;

import org.jboss.security.SimplePrincipal;
import org.wildfly.extension.camel.security.LoginContextBuilder.Type;


/**
 * Authenticates against a given domain
 *
 * @author Thomas.Diesler@jboss.com
 * @since 08-May-2015
 */
public class DomainAuthorizationPolicy extends AbstractAuthorizationPolicy {

    private final Set<String> requiredRoles = new HashSet<>();

    public DomainAuthorizationPolicy roles(String... roles) {
        for (String role : roles) {
            this.requiredRoles.add(role);
        }
        return this;
    }

    // for use in spring xml
    public void setRole(String role) {
        this.requiredRoles.add(role);
    }

    protected LoginContext getLoginContext(String domain, String username, char[] password) throws LoginException {
        LoginContextBuilder builder = new LoginContextBuilder(Type.AUTHENTICATION).domain(domain);
        return builder.username(username).password(password).build();
    }

    @Override
    protected void authorize(LoginContext context) throws LoginException {
        HashSet<String> required = new HashSet<>(requiredRoles);
        Set<Group> groups = context.getSubject().getPrincipals(Group.class);
        if (groups != null) {
            for (Group group : groups) {
                if ("Roles".equals(group.getName())) {
                    for (String role : requiredRoles) {
                        if (group.isMember(new SimplePrincipal(role))) {
                            required.remove(role);
                        }
                    }
                }
            }
        }
        if (!required.isEmpty())
            throw new LoginException("User does not have required roles: " + required);
    }
}
