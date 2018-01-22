/*
 * #%L
 * Wildfly Camel :: Testsuite :: Common
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
package org.wildfly.camel.test.common.ssh;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.Arrays;

import org.apache.sshd.server.Command;
import org.apache.sshd.server.CommandFactory;
import org.apache.sshd.server.Environment;
import org.apache.sshd.server.ExitCallback;
import org.apache.sshd.server.SshServer;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import org.apache.sshd.server.scp.ScpCommandFactory;
import org.apache.sshd.server.shell.ProcessShellFactory;
import org.apache.sshd.server.subsystem.sftp.SftpSubsystemFactory;
import org.wildfly.camel.test.common.utils.AvailablePortFinder;
import org.wildfly.camel.test.common.utils.EnvironmentUtils;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.UserInfo;

public class EmbeddedSSHServer {

    private SshServer sshServer;
    private Path homeDir;

    public EmbeddedSSHServer(Path homeDir) {
        this(homeDir, AvailablePortFinder.getNextAvailable());
    }

    public EmbeddedSSHServer(Path homeDir, int port) {
        this.sshServer = SshServer.setUpDefaultServer();
        this.sshServer.setPort(port);
        this.sshServer.setCommandFactory(new EchoCommandFactory());
        this.sshServer.setKeyPairProvider(new SimpleGeneratorHostKeyProvider());
        this.sshServer.setPasswordAuthenticator((username, password, serverSession) -> username.equals("admin") && password.equals("admin"));
        this.sshServer.setPublickeyAuthenticator((s, publicKey, serverSession) -> true);
        this.homeDir = homeDir;

        if (EnvironmentUtils.isWindows()) {
            sshServer.setShellFactory(new ProcessShellFactory(new String[] { "cmd.exe " }));
        } else {
            sshServer.setShellFactory(new ProcessShellFactory(new String[] { "/bin/sh", "-i", "-s" }));
        }
    }

    public void setupSftp() {
        this.sshServer.setCommandFactory(new ScpCommandFactory());
        this.sshServer.setSubsystemFactories(Arrays.asList(new SftpSubsystemFactory()));
    }

    public void start() throws Exception {
        sshServer.start();
        setupKnownHosts();
    }

    public void stop() throws Exception {
        sshServer.stop();
    }

    public String getConnection() {
        return String.format("localhost:%d", sshServer.getPort());
    }

    private void setupKnownHosts() {
        // Add a localhost entry for the relevant host / port combination to known_hosts
        File knownHostsFile = homeDir.resolve("known_hosts").toFile();

        JSch jsch = new JSch();
        try {
            homeDir.toFile().mkdirs();
            knownHostsFile.createNewFile();

            jsch.setKnownHosts(knownHostsFile.getPath());
            Session s = jsch.getSession("admin", "localhost", sshServer.getPort());
            s.setConfig("StrictHostKeyChecking",  "ask");

            s.setConfig("HashKnownHosts",  "no");
            s.setUserInfo(new UserInfo() {
                @Override
                public String getPassphrase() {
                    return null;
                }
                @Override
                public String getPassword() {
                    return "admin";
                }
                @Override
                public boolean promptPassword(String message) {
                    return true;
                }
                @Override
                public boolean promptPassphrase(String message) {
                    return false;
                }
                @Override
                public boolean promptYesNo(String message) {
                    return true;
                }
                @Override
                public void showMessage(String message) {
                }
            });

            s.connect();
            s.disconnect();
        } catch (Exception e) {
            throw new IllegalStateException("Unable to configure known_hosts file", e);
        }
    }

    private static final class EchoCommandFactory implements CommandFactory {

        @Override
        public Command createCommand(String command) {
            return new EchoCommand(command);
        }
    }

    private static final class EchoCommand implements Command, Runnable {

        private String command;
        private OutputStream out;
        private ExitCallback callback;
        private Thread thread;

        public EchoCommand(String command) {
            this.command = command;
        }

        @Override
        public void setInputStream(InputStream in) {
            // Ignore
        }

        @Override
        public void setOutputStream(OutputStream out) {
            this.out = out;
        }

        @Override
        public void setErrorStream(OutputStream err) {
            // Ignore
        }

        @Override
        public void setExitCallback(ExitCallback callback) {
            this.callback = callback;
        }

        @Override
        public void start(Environment env) throws IOException {
            this.thread = new Thread(this, "EchoCommand");
            this.thread.start();
        }

        @Override
        public void run() {
            boolean success = true;
            String message = null;
            try {
                String fakeCommand = "Running command: " + command;
                out.write(fakeCommand.getBytes());
                out.flush();
            } catch (Exception e) {
                success = false;
                message = e.toString();
            } finally {
                if (success) {
                    callback.onExit(0);
                } else {
                    callback.onExit(1, message);
                }
            }
        }

        @Override
        public void destroy() throws Exception {
            thread.interrupt();
        }
    }
}
