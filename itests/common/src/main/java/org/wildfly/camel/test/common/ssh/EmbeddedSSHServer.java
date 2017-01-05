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
import java.nio.file.Path;
import java.util.Arrays;
import java.util.EnumSet;

import org.apache.sshd.SshServer;
import org.apache.sshd.server.command.ScpCommandFactory;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import org.apache.sshd.server.sftp.SftpSubsystem;
import org.apache.sshd.server.shell.ProcessShellFactory;
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
        this.sshServer.setCommandFactory(new ScpCommandFactory(command -> new ProcessShellFactory(command.split(" ")).create()));
        this.sshServer.setSubsystemFactories(Arrays.asList(new SftpSubsystem.Factory()));
        this.sshServer.setKeyPairProvider(new SimpleGeneratorHostKeyProvider());
        this.sshServer.setPasswordAuthenticator((username, password, serverSession) -> username.equals("admin") && password.equals("admin"));
        this.sshServer.setPublickeyAuthenticator((s, publicKey, serverSession) -> true);
        this.homeDir = homeDir;

        if (EnvironmentUtils.isWindows()) {
            sshServer.setShellFactory(new ProcessShellFactory(new String[] { "cmd.exe " }, EnumSet.of(
                ProcessShellFactory.TtyOptions.Echo, ProcessShellFactory.TtyOptions.ICrNl,
                ProcessShellFactory.TtyOptions.ONlCr)));
        } else {
            sshServer.setShellFactory(new ProcessShellFactory(new String[] { "/bin/sh", "-i", "-s" }, EnumSet
                .of(ProcessShellFactory.TtyOptions.ONlCr)));
        }
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
}
