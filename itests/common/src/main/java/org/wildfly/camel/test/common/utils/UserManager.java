/*
 * #%L
 * Wildfly Camel :: Testsuite
 * %%
 * Copyright (C) 2013 - 2016 RedHat
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
package org.wildfly.camel.test.common.utils;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.stream.Collectors;

/**
 * A utility to edit property files storing user credentials and roles. Note that {@link UserManager} implements
 * {@link Closeable}. {@link #close()} writes the internal data into the respective property files.
 *
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 */
public final class UserManager implements Closeable {

    private static final String APPLICATION_REALM = "ApplicationRealm";
    private static final char[] HEX_CHARS = "0123456789abcdef".toCharArray();

    private static final String MANAGEMENT_REALM = "ManagementRealm";

    public static String encryptPassword(String userName, String password, String realm) {
        try {
            String stringToEncrypt = String.format("%s:%s:%s", userName, realm, password);

            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hashedPassword = md.digest(stringToEncrypt.getBytes(StandardCharsets.UTF_8));

            char[] converted = new char[hashedPassword.length * 2];
            for (int i = 0; i < hashedPassword.length; i++) {
                byte b = hashedPassword[i];
                converted[i * 2] = HEX_CHARS[b >> 4 & 0x0F];
                converted[i * 2 + 1] = HEX_CHARS[b & 0x0F];
            }
            return String.valueOf(converted);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * @param jbossHome relative to where {@code application-users.properties} and {@code application-roles.properties}
     *        should be resolved
     * @return a new {@link UserManager} that will operate on
     *         {@code $jbossHome/standalone/configuration/application-users.properties} and
     *         {@code $jbossHome/standalone/configuration/application-roles.properties}
     * @throws IOException
     */
    public static UserManager forStandaloneApplicationRealm(Path jbossHome) throws IOException {
        final Path userPropertiesPath = jbossHome.resolve("standalone/configuration/application-users.properties");
        final Path rolePropertiesPath = jbossHome.resolve("standalone/configuration/application-roles.properties");
        return new UserManager(userPropertiesPath, rolePropertiesPath, APPLICATION_REALM);
    }

    /**
     * @param jbossHome relative to where {@code mgmt-users.properties} and {@code mgmt-roles.properties} should be
     *        resolved
     * @return a new {@link UserManager} that will operate on
     *         {@code $jbossHome/standalone/configuration/mgmt-users.properties} and
     *         {@code $jbossHome/standalone/configuration/mgmt-roles.properties}
     * @throws IOException
     */
    public static UserManager forStandaloneManagementRealm(Path jbossHome) throws IOException {
        final Path userPropertiesPath = jbossHome.resolve("standalone/configuration/mgmt-users.properties");
        final Path rolePropertiesPath = jbossHome.resolve("standalone/configuration/mgmt-roles.properties");
        return new UserManager(userPropertiesPath, rolePropertiesPath, MANAGEMENT_REALM);
    }

    private final String realm;
    private Properties roleProperties;
    private final Path rolePropertiesPath;
    private Properties userProperties;
    private final Path userPropertiesPath;

    /**
     * Consider using {@link #forStandaloneApplicationRealm(Path)} or {@link #forStandaloneManagementRealm(Path)}
     * instead.
     *
     * @param userPropertiesPath
     * @param rolePropertiesPath
     * @param realm
     * @throws IOException
     */
    public UserManager(Path userPropertiesPath, Path rolePropertiesPath, String realm) throws IOException {
        this.userPropertiesPath = userPropertiesPath;
        this.rolePropertiesPath = rolePropertiesPath;
        this.realm = realm;
        this.userProperties = new Properties();
        if (Files.exists(userPropertiesPath)) {
            try (InputStream in = Files.newInputStream(userPropertiesPath)) {
                userProperties.load(in);
            }
        }
        this.roleProperties = new Properties();
        if (Files.exists(rolePropertiesPath)) {
            try (InputStream in = Files.newInputStream(rolePropertiesPath)) {
                roleProperties.load(in);
            }
        }
    }

    public UserManager addRole(String userName, String role) {
        final List<String> roles = Optional.<String>ofNullable(roleProperties.getProperty(userName))
                .map(list -> new ArrayList<>(Arrays.asList(list.split(","))))
                .orElse(new ArrayList<>());
        roles.add(role);
        roleProperties.put(userName, roles.stream().collect(Collectors.joining(",")));
        return this;
    }

    public UserManager addUser(String userName, String password) {
        userProperties.put(userName, encryptPassword(userName, password, realm));
        return this;
    }

    @Override
    public void close() throws IOException {
        try (OutputStream out = Files.newOutputStream(userPropertiesPath)) {
            userProperties.store(out, null);
        } finally {
            userProperties = null;
            try (OutputStream out = Files.newOutputStream(rolePropertiesPath)) {
                roleProperties.store(out, null);
            } finally {
                roleProperties = null;
            }
        }
    }

    public UserManager removeRole(String userName, String role) {
        final List<String> roles = Optional.<String>ofNullable(roleProperties.getProperty(userName))
                .map(list -> new ArrayList<>(Arrays.asList(list.split(","))))
                .orElse(new ArrayList<>());
        roles.remove(role);
        if (roles.isEmpty()) {
            roleProperties.remove(userName);
        } else {
            roleProperties.put(userName, roles.stream().collect(Collectors.joining(",")));
        }
        return this;
    }

    public UserManager removeUser(String userName) {
        userProperties.remove(userName);
        return this;
    }

}
