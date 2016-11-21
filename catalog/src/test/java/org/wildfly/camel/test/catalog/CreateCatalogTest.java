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

package org.wildfly.camel.test.catalog;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.junit.Test;

public final class CreateCatalogTest {

    Path outdir = Paths.get("src/main/resources");
    
    Set<Path> availablePaths = new LinkedHashSet<>();
    Set<Path> components = new LinkedHashSet<>();
    Set<Path> dataformats = new LinkedHashSet<>();
    Set<Path> languages = new LinkedHashSet<>();
    Set<Path> models = new LinkedHashSet<>();

    Set<Path> supported = new LinkedHashSet<>();
    Set<Path> undecided = new LinkedHashSet<>();
    Set<Path> rejected = new LinkedHashSet<>();

    @Test
    public void createCatalog() throws Exception {

        cleanOutputDir();
        
        collectAvailablePaths();
        collectSupportedPaths();
        
        generateProperties(components, "components.properties");
        generateProperties(dataformats, "dataformats.properties");
        generateProperties(languages, "languages.properties");
        generateProperties(models, "models.properties");

    }

    private void cleanOutputDir() throws IOException {
        Files.walkFileTree(outdir, new SimpleFileVisitor<Path>() {
            public FileVisitResult visitFile(Path path, BasicFileAttributes attrs) throws IOException {
                if (path.toString().endsWith(".json")) {
                    path.toFile().delete();
                }
                return FileVisitResult.CONTINUE;
            }
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                if (dir.toFile().list().length == 0) {
                    dir.toFile().delete();
                }
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private void collectAvailablePaths() throws IOException {
        Path rootPath = Paths.get("target", "camel-catalog");
        Files.walkFileTree(rootPath, new SimpleFileVisitor<Path>() {
            public FileVisitResult visitFile(Path path, BasicFileAttributes attrs) throws IOException {
                path = rootPath.relativize(path);
                if (path.toString().endsWith(".json")) {
                    availablePaths.add(path);
                }
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private void collectSupportedPaths() throws IOException {
        Path rootPath = Paths.get("target", "dependency");
        Files.walkFileTree(rootPath, new SimpleFileVisitor<Path>() {
            public FileVisitResult visitFile(Path path, BasicFileAttributes attrs) throws IOException {
                path = rootPath.relativize(path);
                if (path.toString().endsWith(".json")) {
                    if (path.startsWith("org/apache/camel/component")) {
                        copyPath(components, Paths.get("org/apache/camel/catalog/components"), path);
                    } else if (path.startsWith("org/apache/camel/dataformat")) {
                        copyPath(dataformats, Paths.get("org/apache/camel/catalog/dataformats"), path);
                    } else if (path.startsWith("org/apache/camel/language")) {
                        copyPath(languages, Paths.get("org/apache/camel/catalog/languages"), path);
                    } else if (path.startsWith("org/apache/camel/model")) {
                        copyPath(models, Paths.get("org/apache/camel/catalog/models"), path);
                    }
                }
                return FileVisitResult.CONTINUE;
            }

            void copyPath(Set<Path> collection, Path parent, Path source) throws IOException {
                Path target = parent.resolve(source.getFileName());
                if (availablePaths.contains(target)) {
                    target = outdir.resolve(target);
                    target.getParent().toFile().mkdirs();
                    Files.copy(rootPath.resolve(source), target, StandardCopyOption.REPLACE_EXISTING);
                    collection.add(target);
                }
            }
        });
    }

    private void generateProperties(Set<Path> collection, String filename) throws IOException {
        
        List<String> names = new ArrayList<>();
        for (Path path : collection) {
            String name = path.getFileName().toString();
            name = name.substring(0, name.indexOf("."));
            names.add(name);
        }
        Collections.sort(names);
        
        File outfile = outdir.resolve("org/apache/camel/catalog/" + filename).toFile();
        try (PrintWriter pw = new PrintWriter(outfile)) {
            for (String name : names) {
                pw.println(name);
            }
        }
    }
}
