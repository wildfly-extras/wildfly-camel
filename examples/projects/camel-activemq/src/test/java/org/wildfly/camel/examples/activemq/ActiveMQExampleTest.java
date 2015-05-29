/*
 * #%L
 * Wildfly Camel :: Example :: Camel ActiveMQ
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
package org.wildfly.camel.examples.activemq;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.junit.After;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.camel.test.common.HttpRequest;
import org.wildfly.camel.test.common.HttpRequest.HttpResponse;

@RunAsClient
@RunWith(Arquillian.class)
public class ActiveMQExampleTest {

    private File destination = new File(System.getProperty("jboss.data.dir") + "/orders");

    @After
    public void tearDown() throws IOException {
        Files.walkFileTree(destination.toPath(), new SimpleFileVisitor<Path>() {
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

    @Test
    public void testFileToActiveMQRoute() throws Exception {

        // [ENTESB-3281] Wildfly-Camel build fails on OpenJDK
        String vmname = System.getProperty("java.vm.name");
        Assume.assumeFalse(vmname.contains("OpenJDK"));

        InputStream input = getClass().getResourceAsStream("/orders/test-order.xml");
        Files.copy(input, destination.toPath().resolve("test-order.xml"));
        input.close();

        // Give camel a chance to consume the test order file
        Thread.sleep(2000);

        HttpResponse result = HttpRequest.get(getEndpointAddress("/example-camel-activemq/orders")).getResponse();
        Assert.assertTrue(result.getBody().contains("UK: 1"));
    }

    private String getEndpointAddress(String contextPath) throws MalformedURLException {
        return "http://localhost:8080" + contextPath;
    }
}
