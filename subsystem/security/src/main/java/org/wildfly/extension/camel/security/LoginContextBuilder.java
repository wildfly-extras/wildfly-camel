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

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.AppConfigurationEntry;
import javax.security.auth.login.AppConfigurationEntry.LoginModuleControlFlag;
import javax.security.auth.login.Configuration;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;

import org.jboss.gravia.utils.IllegalArgumentAssertion;
import org.jboss.gravia.utils.IllegalStateAssertion;
import org.jboss.modules.ModuleClassLoader;
import org.jboss.security.ClientLoginModule;

/**
 * A login context builder
 *
 * @author Thomas.Diesler@jboss.com
 * @since 28-Oct-2015
 */
public final class LoginContextBuilder {

    public enum Type { CLIENT, AUTHENTICATION };

    private final Type contextType;
    private String domain;
    private String username;
    private char[] password;

    public LoginContextBuilder(Type type) {
        IllegalArgumentAssertion.assertNotNull(type, "type");
        this.contextType = type;
    }

    public LoginContextBuilder username(String username) {
        this.username = username;
        return this;
    }

    public LoginContextBuilder password(char[] password) {
        this.password = Arrays.copyOf(password, password.length);
        return this;
    }

    public LoginContextBuilder encryptedPassword(char[] password) {
        return encryptedPassword("ApplicationRealm", password);
    }

    public LoginContextBuilder encryptedPassword(String realm, char[] password) {
        IllegalStateAssertion.assertNotNull(username, "Username cannot be null");
        this.password = EncodedUsernamePasswordPrincipal.encryptPassword(realm, username, password);
        return this;
    }

    public LoginContextBuilder domain(String domain) {
        this.domain = domain;
        return this;
    }

    public LoginContext build() throws LoginException {
        if (contextType == Type.CLIENT) {
            return getClientLoginContext();
        } else if (contextType == Type.AUTHENTICATION) {
            return getAuthenticationLoginContext();
        } else {
            throw new IllegalStateException("Unsupported type: " + contextType);
        }
    }

    // Provides a RunAs client login context
    private LoginContext getClientLoginContext() throws LoginException {
        Configuration config = new Configuration() {
            @Override
            public AppConfigurationEntry[] getAppConfigurationEntry(String name) {
                Map<String, String> options = new HashMap<String, String>();
                options.put("multi-threaded", "true");
                options.put("restore-login-identity", "true");

                AppConfigurationEntry clmEntry = new AppConfigurationEntry(ClientLoginModule.class.getName(), LoginModuleControlFlag.REQUIRED, options);
                return new AppConfigurationEntry[] { clmEntry };
            }
        };
        return getLoginContext(config);
    }

    // Provides an authentication login context
    private LoginContext getAuthenticationLoginContext() throws LoginException {
        return getLoginContext(null);
    }

    private LoginContext getLoginContext(Configuration config) throws LoginException {
        IllegalStateAssertion.assertNotNull(username, "username");
        IllegalStateAssertion.assertNotNull(password, "password");

        final String configName = domain != null ? domain : "other";

        CallbackHandler cbh = new CallbackHandler() {
            public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
                for (Callback current : callbacks) {
                    if (current instanceof NameCallback) {
                        ((NameCallback) current).setName(username);
                    } else if (current instanceof PasswordCallback) {
                        ((PasswordCallback) current).setPassword(password);
                    } else {
                        throw new UnsupportedCallbackException(current);
                    }
                }
            }
        };

        ClassLoader tccl = SecurityActions.getContextClassLoader();
        try {
            if (!(tccl instanceof ModuleClassLoader)) {
                ClassLoader modcl = LoginContextBuilder.class.getClassLoader();
                SecurityActions.setContextClassLoader(modcl);
            }
            return new LoginContext(configName, new Subject(), cbh, config);
        } finally {
            SecurityActions.setContextClassLoader(tccl);
        }
    }
}
