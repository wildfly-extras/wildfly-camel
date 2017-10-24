package org.wildfly.swarm.camel.generator;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.camel.catalog.CamelCatalog;
import org.apache.camel.catalog.DefaultCamelCatalog;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader;
import org.jboss.gravia.utils.IllegalArgumentAssertion;
import org.jboss.gravia.utils.IllegalStateAssertion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wildfly.camel.catalog.WildFlyRuntimeProvider;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class FractionsGenerator {

    public static final Logger LOG = LoggerFactory.getLogger(Main.class.getPackage().getName());
    
    private final Path outpath;
    private Map<String, Config> configs = new HashMap<>();

    public static class Dependency {
        String groupId;
        String artifactId;
        Map<String, Boolean> flags = new HashMap<>();

        static Dependency parse(String line) {
            Map<String, Boolean> flags = new HashMap<>();
            int idx = line.lastIndexOf(",");
            while (idx > 0) {
                String flag = line.substring(idx + 1);
                String[] tok = flag.split("=");
                flags.put(tok[0], Boolean.parseBoolean(tok[1]));
                line = line.substring(0, idx);
                idx = line.lastIndexOf(",");
            }
            Dependency result = new Dependency();
            String[] tok = line.split(":");
            result.groupId = tok[0];
            result.artifactId = tok[1];
            result.flags = flags;
            return result;
        }

        public String getSpec() {
            return groupId + ":" + artifactId;
        }
        
        public String getGroupId() {
            return groupId;
        }

        public String getArtifactId() {
            return artifactId;
        }

        public boolean isExclusions() {
            Boolean result = flags.get("exclusions");
            return result != null ? result : false;
        }
    }

    public static class Component implements Comparable<Component> {
        String compId;
        String description;
        Component(String compId, String description) {
            this.compId = compId;
            this.description = description;
        }
        public String getCompId() {
            return compId;
        }
        public String getDescription() {
            return description;
        }
        @Override
        public int compareTo(Component obj) {
            return compId.compareTo(obj.compId);
        }
    }

    public static class Config {
        public String moduleId;
        public List<String> dependencies = Collections.emptyList();
        public List<String> features = Collections.emptyList();
        public List<String> modules = Collections.emptyList();
    }

    public FractionsGenerator(Path outpath) throws IOException {
        this.outpath = outpath;

        try (InputStreamReader reader = new InputStreamReader(getResourceAsStream("fractions.config"))) {
            configs = new ObjectMapper().readValue(reader, new TypeReference<Map<String, Config>>() {
            });
        }
    }

    public void generate() throws IOException {
        allComponents(getComponents());
    }

    private void allComponents(SortedSet<Component> components) throws IOException {
        IllegalArgumentAssertion.assertNotNull(components, "components");

        VelocityEngine ve = newVelocityEngine();
        VelocityContext context = new VelocityContext();
        context.put("components", components);

        String tmplPath = "templates/parent-pom.vm";
        try (InputStreamReader reader = new InputStreamReader(getResourceAsStream(tmplPath))) {
            Path path = Paths.get("components", "pom.xml");
            FractionsGenerator.LOG.info("Generating: {}", path);

            File outfile = outpath.resolve(path).toFile();
            outfile.getParentFile().mkdirs();
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(outfile))) {
                ve.evaluate(context, writer, tmplPath, reader);
            }
        }

        for (Component comp : components) {
            singleComponent(comp);
        }
    }

    private void singleComponent(Component comp) throws IOException {
        IllegalArgumentAssertion.assertNotNull(comp, "comp");

        Config config = configs.get(comp.compId);
        if (config == null) {
            config = new Config();
        }
        if (config.moduleId == null) {
            config.moduleId = comp.compId;
        }

        VelocityEngine ve = newVelocityEngine();
        VelocityContext context = new VelocityContext();
        context.put("modId", config.moduleId);
        context.put("modules", config.modules);
        context.put("dependencies", getDependencies(config));
        context.put("features", getFeatures(config));
        context.put("compName", getComponentName(comp.compId));
        context.put("comp", comp);
        context.put("compId", comp.compId);

        // Write the component POM
        String tmplPath = "templates/pom.vm";
        try (InputStreamReader reader = new InputStreamReader(getResourceAsStream(tmplPath))) {
            Path path = Paths.get("components", comp.compId, "pom.xml");
            FractionsGenerator.LOG.info("Generating: {}", path);

            File outfile = outpath.resolve(path).toFile();
            outfile.getParentFile().mkdirs();
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(outfile))) {
                ve.evaluate(context, writer, tmplPath, reader);
            }
        }

        // Write the component module.conf
        Path path = Paths.get("components", comp.compId, "module.conf");
        File outfile = outpath.resolve(path).toFile();
        try (PrintWriter writer = new PrintWriter(new FileWriter(outfile))) {
            tmplPath = "templates/module.conf";
            InputStream input = getResourceAsStream(tmplPath);
            try (InputStreamReader reader = new InputStreamReader(input)) {
                ve.evaluate(context, writer, tmplPath, reader);
            }
        }
    }

    private SortedSet<Component> getComponents() throws IOException {
        SortedSet<Component> components = new TreeSet<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(getResourceAsStream("whitelist.txt")))) {
            String line = reader.readLine();
            while (line != null) {
                String compId = line.trim();
                components.add(new Component(compId, "Component " + compId));
                line = reader.readLine();
            }
        }
        List<String> blacklist = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(getResourceAsStream("blacklist.txt")))) {
            String line = reader.readLine();
            while (line != null) {
                blacklist.add(line.trim());
                line = reader.readLine();
            }
        }
        CamelCatalog catalog = new DefaultCamelCatalog();
        catalog.setRuntimeProvider(new WildFlyRuntimeProvider());
        for (String name : catalog.findComponentNames()) {
            String schema = catalog.componentJSonSchema(name);
            if (schema != null) {
                Component comp = getComponent(new ObjectMapper().readTree(schema).get("component"));
                if (comp != null && !blacklist.contains(comp.compId)) {
                    components.add(comp);
                } 
            } else {
                LOG.warn("Cannot obtain schema for: {}", name);
            }
        }
        for (String name : catalog.findDataFormatNames()) {
            String schema = catalog.dataFormatJSonSchema(name);
            if (schema != null) {
                Component comp = getComponent(new ObjectMapper().readTree(schema).get("dataformat"));
                if (comp != null && !blacklist.contains(comp.compId)) {
                    components.add(comp);
                }
            } else {
                LOG.warn("Cannot obtain schema for: {}", name);
            }
        }
        for (String name : catalog.findLanguageNames()) {
            String schema = catalog.languageJSonSchema(name);
            if (schema != null) {
                Component comp = getComponent(new ObjectMapper().readTree(schema).get("language"));
                if (comp != null && !blacklist.contains(comp.compId)) {
                    components.add(comp);
                }
            } else {
                LOG.warn("Cannot obtain schema for: {}", name);
            }
        }
        for (String name : catalog.findOtherNames()) {
            String schema = catalog.otherJSonSchema(name);
            if (schema != null) {
                Component comp = getComponent(new ObjectMapper().readTree(schema).get("other"));
                if (comp != null && !blacklist.contains(comp.compId)) {
                    components.add(comp);
                }
            } else {
                LOG.warn("Cannot obtain schema for: {}", name);
            }
        }
        return components;
    }

    private Component getComponent(JsonNode node) throws IOException {
        String groupId = node.get("groupId").asText();
        String artifactId = node.get("artifactId").asText();
        String description = node.get("description").asText();
        Component result = null;
        if (!groupId.equals("org.apache.camel") || !artifactId.startsWith("camel-")) {
            LOG.info("Ignore: {}:{}", groupId, artifactId);
        } else if (!artifactId.equals("camel-core")) {
            result = new Component(artifactId.substring("camel-".length()), description);
        }
        return result;
    }

    private List<Dependency> getDependencies(Config config) throws IOException {
        List<Dependency> result = new ArrayList<>();
        for (String line : config.dependencies) {
            result.add(Dependency.parse(line));
        }
        return result;
    }

    private List<Dependency> getFeatures(Config config) throws IOException {
        List<Dependency> result = new ArrayList<>();
        for (String line : config.features) {
            result.add(Dependency.parse(line));
        }
        return result;
    }

    private String getComponentName(String compId) {
        return compId.toUpperCase().charAt(0) + compId.substring(1);
    }

    private VelocityEngine newVelocityEngine() {
        VelocityEngine ve = new VelocityEngine();
        ve.setProperty(RuntimeConstants.RESOURCE_LOADER, "classpath");
        ve.setProperty("classpath.resource.loader.class", ClasspathResourceLoader.class.getName());
        ve.init();
        return ve;
    }

    private InputStream getResourceAsStream(String resname) {
        InputStream stream = getClass().getClassLoader().getResourceAsStream(resname);
        IllegalStateAssertion.assertNotNull(stream, "Cannot obtain resource stream: " + resname);
        return stream;
    }
}
