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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.UnknownHostException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.wildfly.camel.utils.IllegalStateAssertion;

/**
 * Finds currently available server ports.
 */
public final class AvailablePortFinder {

    public static final int MIN_PORT_NUMBER = 1100;
    public static final int MAX_PORT_NUMBER = 65535;

    // Note, this is scoped on the ClassLoader
    private static Set<Integer> alreadyUsed = new HashSet<>();

    public static int getNextAvailable() {
        return getNextAvailable(MIN_PORT_NUMBER);
    }

    public static int getNextAvailable(int fromPort) {
        InetAddress addr;
        try {
            addr = InetAddress.getLocalHost();
        } catch (UnknownHostException ex) {
            throw new IllegalStateException(ex);
        }
        return getNextAvailable(addr, fromPort);
    }

    public static int getNextAvailable(InetAddress addr) {
        return getNextAvailable(addr, MIN_PORT_NUMBER);
    }

    public static int getNextAvailable(InetAddress addr, int fromPort) {
        if (fromPort < MIN_PORT_NUMBER || fromPort > MAX_PORT_NUMBER)
            throw new IllegalArgumentException("Invalid start port: " + fromPort);

        for (int i = fromPort; i <= MAX_PORT_NUMBER; i++) {
            if (available(addr, i)) {
                return i;
            }
        }

        throw new NoSuchElementException("Could not find an available port above " + fromPort);
    }

    public static int getAndStoreNextAvailable(String filename) {
        int port = getNextAvailable();
        storeServerData(filename, port);
        return port;
    }

    public synchronized static boolean available(InetAddress addr, int port) {

        try (ServerSocket ss = new ServerSocket()) {
            ss.setReuseAddress(false);
            ss.bind(new InetSocketAddress(addr, port));
        } catch (IOException e) {
            return false;
        }

        return alreadyUsed.add(port);
    }

    public static Path storeServerData(String filename, Object port) {
        Path filePath = getDataDirPath().resolve(filename);
        try (PrintWriter fw = new PrintWriter(new FileWriter(filePath.toFile()))) {
            fw.println("" + port);
        } catch (IOException ex) {
            throw new IllegalStateException(ex);
        }
        return filePath;
    }

    public static String readServerData(String filename) {
        Path filePath = getDataDirPath().resolve(filename);
        String result = null;
        try {
            try (BufferedReader fw = new BufferedReader(new FileReader(filePath.toFile()))) {
                result = fw.readLine().trim();
            }
        } catch (FileNotFoundException ex) {
            return null;
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
        return result;
    }

    public static Future<String> readServerDataAsync(String filename) {
        Future<String> future = new Future<String>() {

            private String result;
            private boolean cancelled;

            @Override
            public boolean cancel(boolean mayInterruptIfRunning) {
                cancelled = true;
                return true;
            }

            @Override
            public boolean isCancelled() {
                return cancelled;
            }

            @Override
            public boolean isDone() {
                return cancelled || result != null;
            }

            @Override
            public String get() throws InterruptedException, ExecutionException {
                if (result != null) return result;
                result = readServerData(filename);
                return result;
            }

            @Override
            public String get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {

                result = readServerData(filename);
                if (result != null) return result;

                long endTime = System.currentTimeMillis() + unit.toMillis(timeout);
                while (!cancelled && result == null && System.currentTimeMillis() < endTime) {
                    Thread.sleep(100);
                    result = readServerData(filename);
                }
                if (result != null) return result;
                throw new TimeoutException();
            }
        };
        return future;
    }

    public static void removeServerData(String filename) {
        File dataFile = getDataDirPath().resolve(filename).toFile();
        try {
            if (dataFile.exists())
                dataFile.delete();
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    private static Path getDataDirPath() {
        String jbossHome = System.getProperty("jboss.home.dir");
        IllegalStateAssertion.assertNotNull(jbossHome, "Property 'jboss.home.dir' not set");
        IllegalStateAssertion.assertTrue(new File(jbossHome).isDirectory(), "Not a directory: " + jbossHome);
        Path dirPath = Paths.get(jbossHome, "standalone", "data");
        return dirPath;
    }
}
