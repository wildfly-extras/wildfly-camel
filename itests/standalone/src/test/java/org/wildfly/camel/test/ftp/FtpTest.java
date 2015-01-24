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

package org.wildfly.camel.test.ftp;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import org.apache.camel.CamelContext;
import org.apache.camel.Endpoint;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.test.AvailablePortFinder;
import org.apache.ftpserver.ConnectionConfigFactory;
import org.apache.ftpserver.FtpServer;
import org.apache.ftpserver.FtpServerFactory;
import org.apache.ftpserver.filesystem.nativefs.NativeFileSystemFactory;
import org.apache.ftpserver.ftplet.UserManager;
import org.apache.ftpserver.listener.ListenerFactory;
import org.apache.ftpserver.usermanager.ClearTextPasswordEncryptor;
import org.apache.ftpserver.usermanager.PasswordEncryptor;
import org.apache.ftpserver.usermanager.PropertiesUserManagerFactory;
import org.apache.mina.filter.executor.OrderedThreadPoolExecutor;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(Arquillian.class)
public class FtpTest {

    static final File FTP_ROOT_DIR = new File("./target/res/home");
    static final File USERS_FILE = new File("./src/test/resources/users.properties");
    static final int PORT = AvailablePortFinder.getNextAvailable(21000);

    FtpServer ftpServer;

    @Before
    public void startFtpServer() throws Exception {
        recursiveDelete(FTP_ROOT_DIR);
        System.out.println(new File(".").getCanonicalPath());
        assertTrue(new File(".").getCanonicalPath(), USERS_FILE.exists());

        NativeFileSystemFactory fsf = new NativeFileSystemFactory();
        fsf.setCreateHome(true);

        PropertiesUserManagerFactory pumf = new PropertiesUserManagerFactory();
        pumf.setAdminName("admin");
        pumf.setPasswordEncryptor(new ClearTextPasswordEncryptor());
        pumf.setFile(USERS_FILE);
        UserManager userMgr = pumf.createUserManager();

        ListenerFactory factory1 = new ListenerFactory();
        factory1.setPort(PORT);

        FtpServerFactory serverFactory = new FtpServerFactory();
        serverFactory.setUserManager(userMgr);
        serverFactory.setFileSystem(fsf);
        serverFactory.setConnectionConfig(new ConnectionConfigFactory().createConnectionConfig());
        serverFactory.addListener("default", factory1.createListener());

        FtpServerFactory factory = serverFactory;
        ftpServer = factory.createServer();
        ftpServer.start();
    }

    @After
    public void stopFtpServer() throws Exception {
        if (ftpServer != null) {
            try {
                ftpServer.stop();
                ftpServer = null;
            } catch (Exception e) {
                // ignore while shutting down as we could be polling during shutdown
                // and get errors when the ftp server is stopping. This is only an issue
                // since we host the ftp server embedded in the same jvm for unit testing
            }
        }
    }

    @Deployment
    public static WebArchive createdeployment() throws IOException {
        final WebArchive archive = ShrinkWrap.create(WebArchive.class, "camel-ftp-tests.war");
        addJarHolding(archive, PasswordEncryptor.class);
        addJarHolding(archive, AvailablePortFinder.class);
        addJarHolding(archive, OrderedThreadPoolExecutor.class);
        return archive;
    }

    @Test
    public void testSendFile() throws Exception {

        CamelContext camelctx = new DefaultCamelContext();
        Endpoint endpoint = camelctx.getEndpoint("ftp://localhost:" + PORT + "/foo?username=admin&password=admin");

        File testFile = new File(FTP_ROOT_DIR, "foo/test.txt");

        Assert.assertFalse(testFile.exists());
        camelctx.createProducerTemplate().sendBodyAndHeader(endpoint, "Hello", "CamelFileName", "test.txt");
        Assert.assertTrue(testFile.exists());

        camelctx.stop();
    }

    @Test
    public void testComponentLoads() throws Exception {
        CamelContext camelctx = new DefaultCamelContext();
        Endpoint endpoint = camelctx.getEndpoint("ftp://localhost/foo");
        Assert.assertNotNull(endpoint);
        Assert.assertEquals(endpoint.getClass().getName(), "org.apache.camel.component.file.remote.FtpEndpoint");
        camelctx.stop();
    }

    private static void recursiveDelete(File file) {
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

    private static void addJarHolding(WebArchive archive, Class<?> clazz) {
        URL location = clazz.getProtectionDomain().getCodeSource().getLocation();
        if (location != null && location.getProtocol().equals("file")) {
            File path = new File(location.getPath());
            if (path.isFile()) {
                System.out.println("Adding jar lib to war: " + path);
                archive.addAsLibrary(path);
            }
        }
    }
}
