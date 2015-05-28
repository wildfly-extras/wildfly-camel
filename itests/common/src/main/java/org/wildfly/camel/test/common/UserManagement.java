/*
* #%L
* Wildfly Camel :: Testsuite
* %%
* Copyright (C) 2013 - 2015 RedHat
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
package org.wildfly.camel.test.common;

import java.util.ArrayList;
import java.util.List;

import org.jboss.as.domain.management.security.adduser.AddUser;

public final class UserManagement {

    // Hide ctor
    private UserManagement() {
    }

    public static UserBuilder applicationUser() {
        return new UserBuilder().applicationUser();
    }

    public static UserBuilder managementUser() {
        return new UserBuilder();
    }

    public static final class UserBuilder {
        private List<String> args = new ArrayList<>();

        private UserBuilder() {
            args.add("-s");
        }

        public UserBuilder username(String username) {
            addArgs("-u", username);
            return this;
        }

        public UserBuilder password(String password) {
            addArgs("-p", password);
            return this;
        }

        public UserBuilder group(String group) {
            addArgs("-g", group);
            return this;
        }

        public void create() {
            AddUser.main(args.toArray(new String[args.size()]));
        }

        private UserBuilder applicationUser() {
            addArgs("-a");
            return this;
        }

        private void addArgs(String... args) {
            for (String arg : args) {
                this.args.add(arg);
            }
        }
    }
}
