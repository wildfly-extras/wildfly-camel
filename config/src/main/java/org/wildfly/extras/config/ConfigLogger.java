/*
 * #%L
 * Fuse Patch :: Core
 * %%
 * Copyright (C) 2015 Private
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
package org.wildfly.extras.config;

public final class ConfigLogger {

    public static void info(String message) {
        System.out.println(message);
    }

    public static void warn(String message) {
        System.err.println("Warning: " + message);
    }

    public static void error(String message) {
        System.err.println("Error: " + message);
    }

    public static void error(ConfigException ex) {
        System.err.println("Error: " + ex.getMessage());
    }

    public static void error(Throwable th) {
        System.err.println("Error: " + th.getMessage());
        th.printStackTrace(System.err);
    }
}
