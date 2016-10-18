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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
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

    RoadMap componentRM = new RoadMap(outdir.resolve("component.roadmap"));
    RoadMap dataformatRM = new RoadMap(outdir.resolve("dataformat.roadmap"));
    RoadMap languageRM = new RoadMap(outdir.resolve("language.roadmap"));

    static class RoadMap {
        public final Path outpath;
        public String type;
        public Set<String> available = new LinkedHashSet<>();
        public Set<String> supported = new LinkedHashSet<>();
        public Set<String> planned = new LinkedHashSet<>();
        public Set<String> undecided = new LinkedHashSet<>();
        public Set<String> rejected = new LinkedHashSet<>();
        RoadMap(Path outpath) {
            this.outpath = outpath;
            type = outpath.getFileName().toString();
            type = type.substring(0, type.indexOf('.'));
        }
    }
    
    @Test
    public void createCatalog() throws Exception {

        cleanOutputDir();
        
        collectAvailablePaths();
        collectSupportedPaths();
        
        generateProperties(components, "components.properties");
        generateProperties(dataformats, "dataformats.properties");
        generateProperties(languages, "languages.properties");

        generateRoadmap(componentRM);
        generateRoadmap(dataformatRM);
        generateRoadmap(languageRM);
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
                    if (path.startsWith("org/apache/camel/catalog/components")) {
                        componentRM.available.add(simpleName(path));
                        availablePaths.add(path);
                    } else if (path.startsWith("org/apache/camel/catalog/dataformats")) {
                        dataformatRM.available.add(simpleName(path));
                        availablePaths.add(path);
                    } else if (path.startsWith("org/apache/camel/catalog/languages")) {
                        languageRM.available.add(simpleName(path));
                        availablePaths.add(path);
                    }
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
                        supportedPath(components, Paths.get("org/apache/camel/catalog/components"), path);
                    } else if (path.startsWith("org/apache/camel/dataformat")) {
                        supportedPath(dataformats, Paths.get("org/apache/camel/catalog/dataformats"), path);
                    } else if (path.startsWith("org/apache/camel/language")) {
                        supportedPath(languages, Paths.get("org/apache/camel/catalog/languages"), path);
                    }
                }
                return FileVisitResult.CONTINUE;
            }

            void supportedPath(Set<Path> collection, Path parent, Path source) throws IOException {
                Path target = parent.resolve(source.getFileName());
                if (availablePaths.contains(target)) {
                    target = outdir.resolve(target);
                    target.getParent().toFile().mkdirs();
                    Files.copy(rootPath.resolve(source), target, StandardCopyOption.REPLACE_EXISTING);
                    collection.add(target);
                    
                    if (collection == components) {
                        componentRM.supported.add(simpleName(target));
                    } else if (collection == dataformats) {
                        dataformatRM.supported.add(simpleName(target));
                    } else if (collection == languages) {
                        languageRM.supported.add(simpleName(target));
                    }
                }
            }
        });
    }

    private void generateProperties(Set<Path> collection, String filename) throws IOException {
        
        List<String> names = new ArrayList<>();
        for (Path path : collection) {
            names.add(simpleName(path));
        }
        Collections.sort(names);
        
        File outfile = outdir.resolve("org/apache/camel/catalog/" + filename).toFile();
        try (PrintWriter pw = new PrintWriter(outfile)) {
            for (String name : names) {
                pw.println(name);
            }
        }
    }

    private String simpleName(Path path) {
        String name = path.getFileName().toString();
        return name.substring(0, name.indexOf("."));
    }

    private void generateRoadmap(RoadMap roadmap) throws IOException {
        
        Set<String> collection = null;
        File file = roadmap.outpath.toFile();
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line = br.readLine();
            while (line != null) {
                if (line.length() > 0 && !line.startsWith("#")) {
                    if (line.equals("[planned]")) {
                        collection = roadmap.planned;
                    } else if (line.equals("[rejected]")) {
                        collection = roadmap.rejected;
                    } else if (line.startsWith("[")) {
                        collection = null;
                    } else if (collection != null && roadmap.available.contains(line)) {
                        collection.add(line);
                    }
                }
                line = br.readLine();
            }
        }
        
        for (String entry : roadmap.available) {
            if (!roadmap.supported.contains(entry) && !roadmap.planned.contains(entry) && !roadmap.rejected.contains(entry)) {
                roadmap.undecided.add(entry);
                
            }
        }

        try (PrintWriter pw = new PrintWriter(file)) {
            pw.println("[supported]");
            for (String entry : sorted(roadmap.supported)) {
                pw.println(entry);
            }
            pw.println();
            pw.println("[planned]");
            for (String entry : sorted(roadmap.planned)) {
                pw.println(entry);
            }
            pw.println();
            pw.println("[undecided]");
            for (String entry : sorted(roadmap.undecided)) {
                pw.println(entry);
            }
            pw.println();
            pw.println("[rejected]");
            for (String entry : sorted(roadmap.rejected)) {
                pw.println(entry);
            }
        }
    }
    
    List<String> sorted(Set<String> set) {
        List<String> result = new ArrayList<>(set);
        Collections.sort(result);
        return result; 
    }
}
