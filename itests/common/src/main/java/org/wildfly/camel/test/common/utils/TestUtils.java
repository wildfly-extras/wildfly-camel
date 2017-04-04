/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.wildfly.camel.test.common.utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.net.InetAddress;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.Random;
import java.util.StringJoiner;

public final class TestUtils {
    private static final Random RANDOM = new Random();

    private TestUtils() {
    }

    public static File constructTempDir(String dirPrefix) {
        File file = new File(System.getProperty("java.io.tmpdir"), dirPrefix + RANDOM.nextInt(10000000));
        if (!file.mkdirs()) {
            throw new RuntimeException("could not create temp directory: " + file.getAbsolutePath());
        }
        file.deleteOnExit();
        return file;
    }

    public static boolean deleteFile(File path) throws FileNotFoundException {
        if (!path.exists()) {
            throw new FileNotFoundException(path.getAbsolutePath());
        }
        boolean ret = true;
        if (path.isDirectory()) {
            for (File f : path.listFiles()) {
                ret = ret && deleteFile(f);
            }
        }
        return ret && path.delete();
    }

    public static void addUser(String user, String password, String configFile) throws Exception {
        Path configPath = getStandaloneConfiguratonPath();
        File userConfig = configPath.resolve(configFile).toFile();
        Properties properties = new Properties();

        if (!userConfig.exists()) {
            userConfig.createNewFile();
        } else {
            try (FileReader fr = new FileReader(userConfig)) {
                properties.load(fr);
            }
        }

        properties.put(user, password);

        try (FileWriter fw = new FileWriter(userConfig)) {
            properties.store(fw, null);
        }
    }

    public static void removeUser(String user, String configFile) throws Exception {
        Path configPath = getStandaloneConfiguratonPath();
        File userConfig = configPath.resolve(configFile).toFile();
        Properties properties = new Properties();

        try (FileReader fr = new FileReader(userConfig)) {
            properties.load(fr);
        }

        properties.remove(user);

        try (FileWriter fw = new FileWriter(userConfig)) {
            properties.store(fw, null);
        }
    }

    public static void addUserRoles(String user, String configFile, String... roles) throws Exception {
        Path configPath = getStandaloneConfiguratonPath();
        File roleConfig = configPath.resolve(configFile).toFile();
        Properties properties = new Properties();

        if (!roleConfig.exists()) {
            roleConfig.createNewFile();
        } else {
            try (FileReader fr = new FileReader(roleConfig)) {
                properties.load(fr);
            }
        }

        StringJoiner joiner = new StringJoiner(",");
        for (String role : roles) {
            joiner.add(role);
        }

        properties.put(user, joiner.toString());

        try (FileWriter fw = new FileWriter(roleConfig)) {
            properties.store(fw, null);
        }
    }

    public static void removeUserRoles(String user, String configFile) throws Exception {
        Path configPath = getStandaloneConfiguratonPath();
        File roleConfig = configPath.resolve(configFile).toFile();
        Properties properties = new Properties();

        try (FileReader fr = new FileReader(roleConfig)) {
            properties.load(fr);
        }

        properties.remove(user);

        try (FileWriter fw = new FileWriter(roleConfig)) {
            properties.store(fw, null);
        }
    }

    public static Path getStandaloneConfiguratonPath() {
        String jbossHome = System.getProperty("jboss.home");
        if (jbossHome == null) {
            throw new IllegalStateException("Unable to determine server configuration path. Please define jboss.home");
        }

        return Paths.get(jbossHome,"standalone", "configuration");
    }

    public static String getDockerHost() throws Exception {
        String dockerHost = System.getenv("DOCKER_HOST");
        if (dockerHost == null) {
            return InetAddress.getLocalHost().getHostName();
        }

        URI uri = new URI(dockerHost);
        return uri.getHost();
    }
}
