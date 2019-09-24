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

import org.apache.camel.NamedNode;
import org.apache.camel.spi.RouteContext;
import org.wildfly.extension.camel.security.LoginContextBuilder.Type;


/**
 * Provides access to RunAs login policy
 *
 * @author Thomas.Diesler@jboss.com
 * @since 08-May-2015
 */
public class ClientAuthorizationPolicy extends AbstractAuthorizationPolicy {

    protected LoginContext getLoginContext(String domain, String username, char[] password) throws LoginException {
        LoginContextBuilder builder = new LoginContextBuilder(Type.CLIENT).domain(domain);
        return builder.username(username).password(password).build();
    }
}
