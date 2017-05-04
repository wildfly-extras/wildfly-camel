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
package org.wildfly.camel.test.common;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class ServerLogReader {
    private static final Path SERVER_LOG = Paths.get(System.getProperty("jboss.home"), "standalone/log/server.log");

    /**
     * @param message The log message to match on
     * @param timeout The timeout period in milliseconds for the log message to appear
     * @return true if the log message was found else false
     */
    public static boolean awaitLogMessage(String message, long timeout) {
        long start = System.currentTimeMillis();
        do {
            try {
                List<String> logLines = Files.readAllLines(SERVER_LOG);
                if (logLines.stream().filter(line -> line.contains(message)).count() > 0) {
                    return true;
                }
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
            try {
                Thread.sleep(100L);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        } while (!((System.currentTimeMillis() - start) >= timeout));
        return false;
    }
}
