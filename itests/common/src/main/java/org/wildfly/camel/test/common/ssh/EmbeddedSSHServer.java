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
import java.nio.file.Path;
import java.util.Arrays;

import org.apache.sshd.SshServer;
import org.apache.sshd.server.command.ScpCommandFactory;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import org.apache.sshd.server.sftp.SftpSubsystem;
import org.wildfly.camel.test.common.utils.TestUtils;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.UserInfo;

public class EmbeddedSSHServer {

    private SshServer sshServer;
    private Path homeDir;
    private int port;

    public EmbeddedSSHServer(Path homeDir) {
        this.sshServer = SshServer.setUpDefaultServer();
        this.port = TestUtils.getAvailablePort();
        this.sshServer.setPort(this.port);
        this.sshServer.setKeyPairProvider(new SimpleGeneratorHostKeyProvider());
        this.sshServer.setSubsystemFactories(Arrays.asList(new SftpSubsystem.Factory()));
        this.sshServer.setCommandFactory(new ScpCommandFactory());
        this.sshServer.setPasswordAuthenticator((username, password, serverSession) -> username.equals("admin") && password.equals("admin"));
        this.sshServer.setPublickeyAuthenticator((s, publicKey, serverSession) -> true);
        this.homeDir = homeDir;
    }

    public void start() throws IOException {
        if (this.sshServer != null) {
            this.sshServer.start();
            setupKnownHosts();
        }
    }

    public void stop() throws InterruptedException {
        if (this.sshServer != null) {
            this.sshServer.stop();
        }
    }

    public String getConnection() {
        return String.format("localhost:%d", this.port);
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
}
