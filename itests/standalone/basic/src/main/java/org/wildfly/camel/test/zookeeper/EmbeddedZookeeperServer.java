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
package org.wildfly.camel.test.zookeeper;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.concurrent.TimeUnit;

import org.apache.camel.util.IOHelper;
import org.apache.zookeeper.server.NIOServerCnxnFactory;
import org.apache.zookeeper.server.ZooKeeperServer;
import org.apache.zookeeper.server.persistence.FileTxnSnapLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EmbeddedZookeeperServer {

    private static final Logger LOG = LoggerFactory.getLogger(EmbeddedZookeeperServer.class);

    private final NIOServerCnxnFactory connectionFactory;
    private final ZooKeeperServer zkServer;
    private final Path zookeeperBaseDir;
    private final int port;

    public EmbeddedZookeeperServer() throws Exception {
        this(PortUtils.getAvailablePort(), Files.createTempDirectory("zktemp"));
    }

    public EmbeddedZookeeperServer(int port, Path baseDir) throws Exception {
        this.port = port;

        zookeeperBaseDir = baseDir;

        zkServer = new ZooKeeperServer();
        File dataDir = zookeeperBaseDir.resolve("log").toFile();
        File snapDir = zookeeperBaseDir.resolve("data").toFile();
        FileTxnSnapLog ftxn = new FileTxnSnapLog(dataDir, snapDir);
        zkServer.setTxnLogFactory(ftxn);
        zkServer.setTickTime(1000);
        connectionFactory = new NIOServerCnxnFactory() {
            @Override
            protected void configureSaslLogin() throws IOException {
                // do nothing
            }
        };
        connectionFactory.configure(new InetSocketAddress("localhost", port), 0);
    }

    public int getServerPort() {
        return port;
    }

    public String getConnection() {
        return "localhost:" + port;
    }

    public EmbeddedZookeeperServer startup(int timeout, TimeUnit unit) throws Exception {
        connectionFactory.startup(zkServer);
        if (timeout > 0) {
            waitForServerUp(getConnection(), unit.toMillis(timeout));
        }
        return this;
    }

    public void shutdown() throws Exception {
        try {
            connectionFactory.shutdown();
            connectionFactory.join();
            zkServer.shutdown();

            while (zkServer.isRunning()) {
                zkServer.shutdown();
                Thread.sleep(100);
            }
        } finally {
            cleanZookeeperDir();
        }
    }

    public static boolean waitForServerUp(String hp, long timeout) {
        long start = System.currentTimeMillis();
        while (true) {
            try {
                // if there are multiple hostports, just take the first one
                hp = hp.split(",")[0];
                String result = send4LetterWord(hp, "stat");
                if (result.startsWith("Zookeeper version:")) {
                    return true;
                }
            } catch (IOException e) {
                LOG.info("server {} not up {}", hp, e);
            }

            if (System.currentTimeMillis() > start + timeout) {
                break;
            }
            try {
                Thread.sleep(250);
            } catch (InterruptedException e) {
                // ignore
            }
        }
        return false;
    }

    private static String send4LetterWord(String hp, String cmd) throws IOException {
        String split[] = hp.split(":");
        String host = split[0];
        int port;
        try {
            port = Integer.parseInt(split[1]);
        } catch (RuntimeException e) {
            throw new RuntimeException("Problem parsing " + hp + e.toString());
        }

        Socket sock = new Socket(host, port);
        BufferedReader reader = null;
        try {
            OutputStream outstream = sock.getOutputStream();
            outstream.write(cmd.getBytes());
            outstream.flush();

            reader = IOHelper.buffered(new InputStreamReader(sock.getInputStream()));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line + "\n");
            }
            return sb.toString();
        } finally {
            sock.close();
            if (reader != null) {
                reader.close();
            }
        }
    }

    private void cleanZookeeperDir() throws Exception {
        Files.walkFileTree(zookeeperBaseDir, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exception) throws IOException {
                exception.printStackTrace();
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exception) throws IOException {
                if (exception == null) {
                    Files.delete(dir);
                }
                return FileVisitResult.CONTINUE;
            }
        });
    }

    final static class PortUtils {

        static int getAvailablePort() {
            try {
                ServerSocket socket = new ServerSocket(0);
                try {
                    return socket.getLocalPort();
                } finally {
                    socket.close();
                }
            } catch (IOException e) {
                throw new IllegalStateException("Cannot find available port: " + e.getMessage(), e);
            }
        }

    }
}
