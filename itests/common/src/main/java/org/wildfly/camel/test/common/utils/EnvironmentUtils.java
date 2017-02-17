/*
 * #%L
 * Wildfly Camel :: Testsuite
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

package org.wildfly.camel.test.common.utils;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;

/**
 * Collection of Environment utilities
 *
 * @author Thomas.Diesler@jboss.com
 * @since 22-May-2015
 */
public final class EnvironmentUtils {

    private static final boolean AIX;
    private static final boolean LINUX;
    private static final boolean MAC;
    private static final boolean WINDOWS;
    private static final String JAVA;
    private static final Path JAVA_HOME;

    static {
        final String os = System.getProperty("os.name").toLowerCase(Locale.ROOT);
        AIX = os.equals("aix");
        LINUX = os.equals("linux");
        MAC = os.startsWith("mac");
        WINDOWS = os.contains("win");

        String javaExecutable = "java";
        if (WINDOWS) {
            javaExecutable = "java.exe";
        }
        JAVA = javaExecutable;

        String javaHome = System.getenv("JAVA_HOME");
        if (javaHome == null) {
            javaHome = System.getProperty("java.home");
        }
        JAVA_HOME = Paths.get(javaHome);
    }

    // hide ctor
    private EnvironmentUtils() {
    }

    public static boolean isAIX() {
        return AIX;
    }

    public static boolean isLinux() {
        return LINUX;
    }

    public static boolean isMac() {
        return MAC;
    }

    public static boolean isWindows() {
        return WINDOWS;
    }

    public static boolean isUnknown() {
        return !LINUX && !MAC && !WINDOWS;
    }

    public static Path getJavaExecutablePath() {
        return Paths.get(JAVA_HOME.toString(), "bin", JAVA);
    }
}
