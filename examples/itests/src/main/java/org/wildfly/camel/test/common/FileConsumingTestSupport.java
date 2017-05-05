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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;

public abstract class FileConsumingTestSupport {

    @Before
    public void setUp() throws Exception {
        String packageName = getClass().getPackage().getName();
        String fileLocation = packageName.replace("org.wildfly.camel.test.", "").replace(".", "/");
        String sourceFileName = sourceFilename();
        InputStream input = getClass().getResourceAsStream("/" + fileLocation + "/" + sourceFileName);
        Files.copy(input, destinationPath().resolve(sourceFileName));
        input.close();

        boolean await = awaitFileProcessed();
        Assert.assertTrue("Gave up waiting for file to be processed", await);
    }

    @After
    public void tearDown () throws IOException {
        Files.walkFileTree(destinationPath(), new SimpleFileVisitor<Path>() {
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

    private boolean awaitFileProcessed() throws Exception {
        long start = System.currentTimeMillis();
        long timeout = 15000;
        do {
            File file = processedPath().toFile();
            if (file.exists()) {
                if ((file.isDirectory() && file.list().length > 0) || (file.isFile())) {
                    return true;
                }
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

    protected abstract String sourceFilename();
    protected abstract Path destinationPath();
    protected abstract Path processedPath();
}
