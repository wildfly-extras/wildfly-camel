/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wildfly.extension.camel.config;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Properties;
import java.util.ServiceLoader;

import org.jdom.Attribute;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.Namespace;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

public class ConfigSupport {

    public static ConfigContext createContext(Path jbossHome, Path configuration, Document doc) {
        return new ConfigContext(jbossHome, configuration, doc);
    }

    public static void applyConfigChange(Path jbossHome, boolean enable) throws Exception {

        List<Path> standalonePaths = new ArrayList<>();
        standalonePaths.add(Paths.get("standalone", "configuration", "standalone.xml"));
        standalonePaths.add(Paths.get("standalone", "configuration", "standalone-full.xml"));
        standalonePaths.add(Paths.get("standalone", "configuration", "standalone-full-ha.xml"));
        standalonePaths.add(Paths.get("standalone", "configuration", "standalone-ha.xml"));
        
        List<Path> domainPaths = new ArrayList<>();
        domainPaths.add(Paths.get("domain", "configuration", "domain.xml"));
        
        Iterator<ConfigPlugin> itsrv = ServiceLoader.load(ConfigPlugin.class, ConfigSupport.class.getClassLoader()).iterator();
        while (itsrv.hasNext()) {
            ConfigPlugin plugin = itsrv.next();
            System.out.println("Using config plugin: " + plugin.getClass().getName());

            File layersFile = jbossHome.resolve(Paths.get("modules", "layers.conf")).toFile();
            Properties layersProperties = new Properties();
            ArrayList<String> layers = new ArrayList<String>();
            if (layersFile.exists()) {
                try (FileInputStream is = new FileInputStream(layersFile)) {
                    layersProperties.load(is);
                }
                String layersValue = layersProperties.getProperty("layers");
                if (layersValue == null) {
                    layersValue = "";
                }
                for (String s : layersValue.split(",")) {
                    s = s.trim();
                    if (s.length() > 0) {
                        layers.add(s);
                    }
                }
            }

            // Lets validate that all the layer deps are already installed.
            applyLayerConfigChange(enable, plugin, layers);

            String layersValue = "";
            for (String layer : layers) {
                if (layersValue.length() != 0) {
                    layersValue += ",";
                }
                layersValue += layer;
            }
            layersProperties.put("layers", layersValue);
            System.out.println("\tWriting 'layers=" + layersValue + "' to: " + layersFile);
            try (FileOutputStream out = new FileOutputStream(layersFile)) {
                layersProperties.store(out, null);
            }

            SAXBuilder jdom = new SAXBuilder();
            for (Path p : standalonePaths) {
                Path path = jbossHome.resolve(p);

                System.out.println("\tEnabling configuration on " + path);
                Document doc = jdom.build(path.toUri().toURL());

                ConfigContext context = new ConfigContext(jbossHome, path, doc);
                plugin.applyStandaloneConfigChange(context, enable);

                XMLOutputter output = new XMLOutputter();
                output.setFormat(Format.getRawFormat());
                String newXML = output.outputString(doc);
                backup(path);
                writeFile(path, newXML, "UTF-8");
            }

            for (Path p : domainPaths) {
                Path path = jbossHome.resolve(p);

                System.out.println("\tEnabling configuration on " + path);
                Document doc = jdom.build(path.toUri().toURL());

                ConfigContext context = new ConfigContext(jbossHome, path, doc);
                plugin.applyDomainConfigChange(context, enable);

                XMLOutputter output = new XMLOutputter();
                output.setFormat(Format.getRawFormat());
                String newXML = output.outputString(doc);
                backup(path);
                writeFile(path, newXML, "UTF-8");
            }
        }
    }

    public static void applyLayerConfigChange(boolean enable, ConfigPlugin plugin, ArrayList<String> layers) {

        class LayerData implements Comparable<LayerData> {
            String name;
            int primaryPriority;
            int secondaryPriority;
            LayerConfig config;

            @Override
            public int compareTo(LayerData o) {
                if (o.primaryPriority != primaryPriority) {
                    return primaryPriority - o.primaryPriority;
                } else {
                    return secondaryPriority - o.secondaryPriority;
                }
            }
        }

        ArrayList<LayerConfig> configs = new ArrayList<LayerConfig>(plugin.getLayerConfigs());
        ArrayList<LayerData> workingList = new ArrayList<LayerData>();

        // Lets match up existing layers to the layer config..
        int secondaryCounter = 0;
        for (int i = 0; i < layers.size(); i++) {

            String name = layers.get(i);
            LayerData ld = new LayerData();
            ld.name = name;
            ld.primaryPriority = 0;
            ld.secondaryPriority = i;

            // Now lets see if we can match the layer to a
            // LayerConfig...
            boolean add = true;
            for (LayerConfig config : configs) {
                if (config.pattern.matcher(name).matches()) {
                    if (enable) {
                        ld.config = config;
                        if (config.type == LayerConfig.Type.INSTALLING) {
                            ld.name = config.name;
                        }
                        ld.primaryPriority = config.priority;
                        ld.secondaryPriority = secondaryCounter++;
                    } else {
                        if (config.type == LayerConfig.Type.INSTALLING) {
                            add = false;
                        }
                    }
                    break;
                }
            }
            if (add) {
                workingList.add(ld);
            }
        }

        // Lets find out which layers did not match..
        for (LayerData layerData : workingList) {
            if (layerData.config != null) {
                configs.remove(layerData.config);
            }
        }

        // Lets deal with the layers which have not been configured yet.
        for (LayerConfig config : configs) {
            switch (config.type) {
                case OPTIONAL:
                    // We wont add it.. if it was available,
                    // we would have already applied a priority to it.
                    break;

                case REQUIRED:
                    if (enable) {
                        throw new CommandException("Required layer has not yet been configured: " + config.name);
                    }
                    break;
                case INSTALLING:
                    // This is a layer we are installing.. so lets add it in.
                    if (enable) {
                        LayerData ld = new LayerData();
                        ld.name = config.name;
                        ld.config = config;
                        ld.primaryPriority = config.priority;
                        ld.secondaryPriority = secondaryCounter++;
                        workingList.add(ld);
                    }
                    break;
            }

        }

        // Now lets sort the layers by primary and secondary priority.
        Collections.sort(workingList);

        layers.clear();
        for (LayerData layerData : workingList) {
            layers.add(layerData.name);
        }
    }

    static Path getJBossHome() throws UnsupportedEncodingException {

        String jbossHome = System.getProperty("jboss.home");
        if (jbossHome == null) {
            jbossHome = System.getenv("JBOSS_HOME");
        }

        if (!Paths.get(jbossHome).toFile().exists())
            throw new CommandException("Cannot obtain JBOSS_HOME: " + jbossHome);

        Path standalonePath = Paths.get(jbossHome, "standalone", "configuration");
        Path domainPath = Paths.get(jbossHome, "domain", "configuration");

        if (!standalonePath.toFile().exists())
            throw new CommandException("Path to standalone configutration does not exist: " + standalonePath);

        if (!domainPath.toFile().exists())
            throw new CommandException("Path to domain configutration does not exist: " + standalonePath);

        return Paths.get(jbossHome);
    }

    public static Element createElementFromText(String xml) {
        SAXBuilder jdom = new SAXBuilder();
        Document doc;
        try {
            doc = jdom.build(new StringReader(xml));
        } catch (JDOMException | IOException ex) {
            throw new RuntimeException(ex);
        }
        return (Element) doc.getRootElement().clone();
    }

    public static Element loadElementFrom(URL resource) {
        try {
            byte[] data = loadBytesFromURL(resource);
            String xml = new String(data, "UTF-8");
            return createElementFromText(xml);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static byte[] loadBytesFromURL(URL resource) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        final byte[] buffer = new byte[10234];
        try (InputStream in = resource.openStream()) {
            while (true) {
                int count = in.read(buffer, 0, buffer.length);
                if (count < 0)
                    break;
                out.write(buffer, 0, count);
            }
        }
        return out.toByteArray();
    }

    @SuppressWarnings("unchecked")
    public static Element findElementWithAttributeValue(Element element, String name, Namespace ns, String attrName, String attrValue) {
        if (element.getName().equals(name) && element.getNamespace().equals(ns)) {
            Attribute attribute = element.getAttribute(attrName);
            if (attribute != null && attrValue.equals(attribute.getValue())) {
                return element;
            }
        }
        for (Element ch : (List<Element>) element.getChildren()) {
            Element result = findElementWithAttributeValue(ch, name, ns, attrName, attrValue);
            if (result != null) {
                return result;
            }
        }
        return null;
    }

    public static void assertExists(Element extensions, String message) {
        if (extensions == null) {
            throw new BadDocument(message);
        }
    }

    private static void backup(Path path) throws IOException {
        String name = path + ".bak";
        int counter = 2;
        while (Files.exists(Paths.get(name))) {
            name = path + ".bak" + counter;
            counter++;
        }
        Files.copy(path, Paths.get(name));
    }

    private static Path writeFile(Path path, String value, String encoding) throws IOException {
        byte[] bytes = value.getBytes(encoding);
        return Files.write(path, bytes, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE);
    }

    static LinkedHashMap<String, Element> mapByAttributeName(List<Element> elements, String attrName) {
        LinkedHashMap<String, Element> rc = new LinkedHashMap<String, Element>();
        for (Element element : elements) {
            Attribute attribute = element.getAttribute(attrName);
            if (attribute != null) {
                rc.put(attribute.getValue(), element);
            }
        }
        return rc;
    }

    @SuppressWarnings("unchecked")
    public static List<Element> findProfileElements(Document doc, Namespace ns) {
        List<Element> result = new ArrayList<>();
        Element profile = doc.getRootElement().getChild("profile", ns);
        if (profile != null) {
            result.add(profile);
        }
        Element profiles = doc.getRootElement().getChild("profiles", ns);
        if (profiles != null) {
            result.addAll(profiles.getChildren("profile", ns));
        }
        return result;
    }

    @SuppressWarnings("serial")
    static class BadDocument extends RuntimeException {
        BadDocument(String message) {
            super(message);
        }
    }

    @SuppressWarnings("serial")
    static class CommandException extends RuntimeException {
        CommandException(String message) {
            super(message);
        }
    }
}
