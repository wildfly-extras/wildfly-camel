/*
 * #%L
 * Wildfly Camel :: Testsuite
 * %%
 * Copyright (C) 2013 - 2015 RedHat
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

package org.wildfly.camel.test.ftp;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.camel.CamelContext;
import org.apache.camel.Endpoint;
import org.apache.camel.impl.DefaultCamelContext;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.gravia.resource.ManifestBuilder;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.camel.test.common.ssh.EmbeddedSSHServer;
import org.wildfly.camel.test.common.utils.TestUtils;
import org.wildfly.extension.camel.CamelAware;

@CamelAware
@RunWith(Arquillian.class)
public class SftpIntegrationTest {

    private static final String FILE_BASEDIR = "basedir.txt";
    private static final Path FTP_ROOT_DIR = Paths.get("target/sftp");
    private EmbeddedSSHServer sshServer;

    @Deployment
    public static WebArchive createDeployment() throws IOException {
        File[] libraryDependencies = Maven.configureResolverViaPlugin().
                resolve("org.apache.sshd:sshd-core").
                withTransitivity().
                asFile();

        final WebArchive archive = ShrinkWrap.create(WebArchive.class, "camel-ftp-tests.war");
        archive.addClasses(EmbeddedSSHServer.class, TestUtils.class);
        archive.addAsResource(new StringAsset(System.getProperty("basedir")), FILE_BASEDIR);
        archive.addAsLibraries(libraryDependencies);
        archive.setManifest(() -> {
            ManifestBuilder builder = new ManifestBuilder();
            builder.addManifestHeader("Dependencies", "com.jcraft.jsch");
            return builder.openStream();
        });
        return archive;
    }

    @Before
    public void setUp() throws Exception {
        recursiveDelete(resolvePath(FTP_ROOT_DIR).toFile());
        sshServer = new EmbeddedSSHServer(FTP_ROOT_DIR);
        sshServer.start();
    }

    @After
    public void tearDown() throws Exception {
        if (sshServer != null) {
            sshServer.stop();
        }
    }

    @Test
    public void testSendFile() throws Exception {

        File testFile = resolvePath(FTP_ROOT_DIR).resolve("test.txt").toFile();

        CamelContext camelctx = new DefaultCamelContext();
        try {
            Endpoint endpoint = camelctx.getEndpoint("sftp://" + sshServer.getConnection() + "/target/sftp?username=admin&password=admin");
            Assert.assertFalse(testFile.exists());
            camelctx.createProducerTemplate().sendBodyAndHeader(endpoint, "Hello", "CamelFileName", "test.txt");
            Assert.assertTrue(testFile.exists());
        } finally {
            camelctx.stop();
        }
    }

    private void recursiveDelete(File file) {
        if (file.exists()) {
            if (file.isDirectory()) {
                File[] files = file.listFiles();
                if (files != null) {
                    for (File f : files) {
                        recursiveDelete(f);
                    }
                }
            }
            file.delete();
        }
    }

    private Path resolvePath(Path other) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(getClass().getResourceAsStream("/" + FILE_BASEDIR)));
        try {
            return Paths.get(reader.readLine()).resolve(other);
        } finally {
            reader.close();
        }
    }
}
