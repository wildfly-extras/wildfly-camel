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
import java.io.FileNotFoundException;
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
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public final class CreateCatalogTest {

    List<String> kinds = Arrays.asList("component", "dataformat", "language");
    Path outdir = Paths.get("src/main/resources");
    
    static class RoadMap {
        public final Path outpath;
        public String type;
        public Set<String> supported = new LinkedHashSet<>();
        public Set<String> planned = new LinkedHashSet<>();
        public Set<String> undecided = new LinkedHashSet<>();
        public Set<String> rejected = new LinkedHashSet<>();
        RoadMap(Path outpath) {
            this.outpath = outpath;
            type = outpath.getFileName().toString();
            type = type.substring(0, type.indexOf('.'));
            
            Set<String> collection = null;
            File file = outpath.toFile();
            try (BufferedReader br = new BufferedReader(new FileReader(file))) {
                String line = br.readLine();
                while (line != null) {
                    if (line.length() > 0 && !line.startsWith("#")) {
                        if (line.equals("[planned]")) {
                            collection = planned;
                        } else if (line.equals("[rejected]")) {
                            collection = rejected;
                        } else if (line.startsWith("[")) {
                            collection = null;
                        } else if (collection != null) {
                            collection.add(line);
                        }
                    }
                    line = br.readLine();
                }
            } catch (IOException ex) {
                throw new IllegalStateException(ex);
            }
        }
    }
    
    RoadMap componentRM = new RoadMap(outdir.resolve("component.roadmap"));
    RoadMap dataformatRM = new RoadMap(outdir.resolve("dataformat.roadmap"));
    RoadMap languageRM = new RoadMap(outdir.resolve("language.roadmap"));

    class Item {
        Path path;
        String kind;
        String javaType;
        RoadMap roadmap;
        Item(Path path, String kind, String javaType) {
            this.path = path;
            this.kind = kind;
            this.javaType = javaType;
            
        }
        RoadMap roadmap() {
            if ("dataformat".equals(kind)) {
                return dataformatRM;
            } else if ("language".equals(kind)) {
                return languageRM;
            } else {
                return componentRM;
            }
        }
    }
    
    Set<Item> available = new LinkedHashSet<>();
    
    @Test
    public void createCatalog() throws Exception {

        cleanOutputDir();
        
        collectAvailable();
        collectSupported();
        
        generateProperties(componentRM, "components.properties");
        generateProperties(dataformatRM, "dataformats.properties");
        generateProperties(languageRM, "languages.properties");

        generateRoadmap(componentRM);
        generateRoadmap(dataformatRM);
        generateRoadmap(languageRM);
    }

    private void cleanOutputDir() throws IOException {
        Files.walkFileTree(outdir, new SimpleFileVisitor<Path>() {
            public FileVisitResult visitFile(Path path, BasicFileAttributes attrs) throws IOException {
                if (path.toString().endsWith(".json") || path.toString().endsWith(".properties")) {
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

    private void collectAvailable() throws IOException {
        Path rootPath = Paths.get("target", "camel-catalog");
        Files.walkFileTree(rootPath, new SimpleFileVisitor<Path>() {
            public FileVisitResult visitFile(Path path, BasicFileAttributes attrs) throws IOException {
                if (path.toString().endsWith(".json")) {
                    ObjectMapper mapper = new ObjectMapper();
                    JsonNode tree = mapper.readTree(path.toFile());
                    String kind = tree.findValue("kind").textValue();
                    String javaType = tree.findValue("javaType").textValue();
                    if (kinds.contains(kind) && javaType != null) {
                        available.add(new Item(rootPath.relativize(path), kind, javaType));
                    }
                }
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private void collectSupported() throws IOException {
        Path rootPath = Paths.get("target", "dependency");
        for(Item item : available) {
            String name = simpleName(item.path);
            
            RoadMap roadmap = item.roadmap();
            if (!roadmap.planned.contains(name) && !roadmap.rejected.contains(name)) {
                roadmap.undecided.add(name);
            }
            
            Path javaType = Paths.get(item.javaType.replace('.', '/') + ".class");
            if (rootPath.resolve(javaType).toFile().isFile()) {
                Path target = outdir.resolve(item.path);
                Path source = Paths.get("target", "camel-catalog").resolve(item.path);
                target.getParent().toFile().mkdirs();
                Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
                roadmap.supported.add(name);
                roadmap.planned.remove(name);
                roadmap.undecided.remove(name);
                roadmap.rejected.remove(name);
            }
        }
    }

    private void generateProperties(RoadMap roadmap, String filename) throws IOException {
        File outfile = outdir.resolve("org/apache/camel/catalog/" + filename).toFile();
        try (PrintWriter pw = new PrintWriter(outfile)) {
            for (String name : sorted(roadmap.supported)) {
                pw.println(name);
            }
        }
    }

    private String simpleName(Path path) {
        String name = path.getFileName().toString();
        return name.substring(0, name.indexOf("."));
    }

    private void generateRoadmap(RoadMap roadmap) throws IOException {
        
        
        try (PrintWriter pw = new PrintWriter(roadmap.outpath.toFile())) {
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
    
    private List<String> sorted(Set<String> set) {
        List<String> result = new ArrayList<>(set);
        Collections.sort(result);
        return result; 
    }
}
