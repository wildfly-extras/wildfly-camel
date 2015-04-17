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

import org.jboss.modules.ModuleClassLoader;
import org.jboss.security.ClientLoginModule;

/**
 * Provides access RunAs Client login context
 *
 * @author Thomas.Diesler@jboss.com
 * @since 08-May-2015
 */
public final class ClientLoginContext {

    // Hide ctor
    private ClientLoginContext() {
    }

    public static LoginContext newLoginContext(final String username, final String password) throws LoginException {
        final String configurationName = "WildFly-Camel";
        CallbackHandler cbh = new CallbackHandler() {
            public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
                for (Callback current : callbacks) {
                    if (current instanceof NameCallback) {
                        ((NameCallback) current).setName(username);
                    } else if (current instanceof PasswordCallback) {
                        ((PasswordCallback) current).setPassword(password.toCharArray());
                    } else {
                        throw new UnsupportedCallbackException(current);
                    }
                }
            }
        };
        Configuration config = new Configuration() {
            @Override
            public AppConfigurationEntry[] getAppConfigurationEntry(String name) {
                if (configurationName.equals(name) == false) {
                    throw new IllegalArgumentException("Unexpected configuration name '" + name + "'");
                }
                Map<String, String> options = new HashMap<String, String>();
                options.put("multi-threaded", "true");
                options.put("restore-login-identity", "true");

                AppConfigurationEntry clmEntry = new AppConfigurationEntry(ClientLoginModule.class.getName(), LoginModuleControlFlag.REQUIRED, options);

                return new AppConfigurationEntry[] { clmEntry };
            }
        };

        ClassLoader tccl = SecurityActions.getContextClassLoader();
        try {
            if (!(tccl instanceof ModuleClassLoader)) {
                ClassLoader modcl = ClientLoginContext.class.getClassLoader();
                SecurityActions.setContextClassLoader(modcl);
            }
            return new LoginContext(configurationName, new Subject(), cbh, config);
        } finally {
            SecurityActions.setContextClassLoader(tccl);
        }
    }
}
