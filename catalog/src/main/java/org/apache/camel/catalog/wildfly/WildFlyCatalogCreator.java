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

package org.apache.camel.catalog.wildfly;

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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public final class WildFlyCatalogCreator {

    final Path basedir = Paths.get(System.getProperty("basedir"));
    final Path resdir = basedir.resolve(Paths.get("src/main/resources"));
    final Path srcdir = basedir.resolve(Paths.get("target/camel-catalog"));
    final Path outdir = basedir.resolve(Paths.get("target/classes"));
    
    enum Kind {
        component, dataformat, language;
    }
    
    enum State {
        supported, planned, undecided, rejected
    }
    
    class Item {
        final Path path;
        final Kind kind;
        final String name;
        final String javaType;
        State state = State.undecided;
        Item(Path path, Kind kind, String javaType) {
            this.path = path;
            this.kind = kind;
            this.javaType = javaType;
            String nspec = path.getFileName().toString();
            nspec = nspec.substring(0, nspec.indexOf("."));
            this.name = nspec;
        }
    }
    
    class RoadMap {
        final Kind kind;
        final Path outpath;
        final Map<String, Item> items = new HashMap<>();
        RoadMap(Kind kind) {
            this.outpath = resdir.resolve(kind + ".roadmap");
            this.kind = kind;
        }
        void add(Item item) {
            items.put(item.name, item);
        }
        Item item(String name) {
            return items.get(name);
        }
        List<String> sortedNames(State state) {
            List<String> result = new ArrayList<>();
            for (Item item : items.values()) {
                if (item.state == state) {
                    result.add(item.name);
                }
            }
            Collections.sort(result);
            return result;
        }
    }
    
    Map<Kind, RoadMap> roadmaps = new HashMap<>();
    {
        roadmaps.put(Kind.component, new RoadMap(Kind.component));
        roadmaps.put(Kind.dataformat, new RoadMap(Kind.dataformat));
        roadmaps.put(Kind.language, new RoadMap(Kind.language));
    }

    public static void main(String[] args) throws Exception {
        new WildFlyCatalogCreator().createCatalog();
    }
    
    private void createCatalog() throws Exception {

        collectAvailable();
        collectSupported();
        
        generateProperties();
        generateRoadmaps();
    }

    private void collectAvailable() throws IOException {
        
        // Walk the available camel catalog items
        Files.walkFileTree(srcdir, new SimpleFileVisitor<Path>() {
            public FileVisitResult visitFile(Path path, BasicFileAttributes attrs) throws IOException {
                if (path.toString().endsWith(".json")) {
                    ObjectMapper mapper = new ObjectMapper();
                    JsonNode tree = mapper.readTree(path.toFile());
                    String kind = tree.findValue("kind").textValue();
                    String javaType = tree.findValue("javaType").textValue();
                    if (validKind(kind) && javaType != null) {
                        Item item = new Item(srcdir.relativize(path), Kind.valueOf(kind), javaType);
                        roadmaps.get(item.kind).add(item);
                   }
                }
                return FileVisitResult.CONTINUE;
            }
            boolean validKind(String kind) {
                try {
                    Kind.valueOf(kind);
                    return true;
                } catch (IllegalArgumentException e) {
                    return false;
                }
            }
        });
        
        // Change state when planned or rejected 
        for (RoadMap roadmap : roadmaps.values()) {
            State state = null;
            File file = roadmap.outpath.toFile();
            try (BufferedReader br = new BufferedReader(new FileReader(file))) {
                String line = br.readLine();
                while (line != null) {
                    if (line.length() > 0 && !line.startsWith("#")) {
                        if (line.equals("[" + State.planned + "]")) {
                            state = State.planned;
                        } else if (line.equals("[" + State.rejected + "]")) {
                            state = State.rejected;
                        } else if (line.startsWith("[")) {
                            state = null;
                        } else if (state != null) {
                            Item item = roadmap.item(line.trim());
                            if (item != null) {
                                item.state = state;
                            }
                        }
                    }
                    line = br.readLine();
                }
            } catch (IOException ex) {
                throw new IllegalStateException(ex);
            }
        }
    }

    private void collectSupported() throws IOException {
        Path rootPath = basedir.resolve(Paths.get("target", "dependency"));
        for (RoadMap roadmap : roadmaps.values()) {
            for (Item item : roadmap.items.values()) {
                Path javaType = Paths.get(item.javaType.replace('.', '/') + ".class");
                if (rootPath.resolve(javaType).toFile().isFile()) {
                    Path target = outdir.resolve(item.path);
                    Path source = srcdir.resolve(item.path);
                    target.getParent().toFile().mkdirs();
                    Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
                    item.state = State.supported;
                }
            }
        }
    }

    private void generateProperties() throws IOException {
        for (RoadMap roadmap : roadmaps.values()) {
            File outfile = outdir.resolve("org/apache/camel/catalog/" + roadmap.kind + "s.properties").toFile();
            try (PrintWriter pw = new PrintWriter(outfile)) {
                for (String name : roadmap.sortedNames(State.supported)) {
                    pw.println(name);
                }
            }
        }
    }

    private void generateRoadmaps() throws IOException {
        for (RoadMap roadmap : roadmaps.values()) {
            try (PrintWriter pw = new PrintWriter(roadmap.outpath.toFile())) {
                for (State state : State.values()) {
                    pw.println("[" + state + "]");
                    for (String entry : roadmap.sortedNames(state)) {
                        pw.println(entry);
                    }
                    pw.println();
                }
            }
        }
    }
}
