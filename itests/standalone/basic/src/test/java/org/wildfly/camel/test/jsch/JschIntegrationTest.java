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

package org.wildfly.camel.test.jsch;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.camel.CamelContext;
import org.apache.camel.Endpoint;
import org.apache.camel.impl.DefaultCamelContext;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.camel.test.common.ssh.EmbeddedSSHServer;
import org.wildfly.camel.test.common.utils.TestUtils;
import org.wildfly.extension.camel.CamelAware;

@CamelAware
@RunWith(Arquillian.class)
@ServerSetup({ JschIntegrationTest.SSHServerSetupTask.class })
public class JschIntegrationTest {
    private static final String FILE_BASEDIR = "basedir.txt";
    private static final Path SSHD_ROOT_DIR = Paths.get("target/sshd");
    private static final Path KNOWN_HOSTS = SSHD_ROOT_DIR.resolve("known_hosts");

    static class SSHServerSetupTask implements ServerSetupTask {

        static final EmbeddedSSHServer sshServer = new EmbeddedSSHServer(Paths.get("target/sshd"));

        @Override
        public void setup(ManagementClient managementClient, String containerId) throws Exception {
            sshServer.setupSftp();
            sshServer.start();
        }

        @Override
        public void tearDown(ManagementClient managementClient, String containerId) throws Exception {
            sshServer.stop();
        }
    }

    @Deployment
    public static JavaArchive createDeployment() {
        return ShrinkWrap.create(JavaArchive.class, "camel-jsch-tests.jar")
            .addAsResource(new StringAsset(JschIntegrationTest.SSHServerSetupTask.sshServer.getConnection()), "jsch-connection")
            .addAsResource(new StringAsset(System.getProperty("basedir")), FILE_BASEDIR)
            .addClasses(TestUtils.class);
    }

    @Test
    public void testScpFile() throws Exception {

        File testFile = resolvePath(SSHD_ROOT_DIR).resolve("test.txt").toFile();
        CamelContext camelctx = new DefaultCamelContext();
        try {
            Endpoint endpoint = camelctx.getEndpoint(getScpEndpointUri());
            Assert.assertFalse(testFile.exists());
            camelctx.createProducerTemplate().sendBodyAndHeader(endpoint, "Hello", "CamelFileName", "test.txt");
            Assert.assertTrue(testFile.exists());
        } finally {
            camelctx.stop();
        }
    }

    private String getScpEndpointUri() throws IOException {
        String conUrl = TestUtils.getResourceValue(getClass(), "/jsch-connection");
        return String.format("scp://%s/%s?username=admin&password=admin&knownHostsFile=%s", conUrl, SSHD_ROOT_DIR, KNOWN_HOSTS);
    }

    private Path resolvePath(Path other) throws IOException {
        return Paths.get(TestUtils.getResourceValue(getClass(), "/" + FILE_BASEDIR)).resolve(other);
    }
}
