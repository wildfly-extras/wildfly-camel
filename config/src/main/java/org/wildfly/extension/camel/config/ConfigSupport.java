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
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import org.jdom.Attribute;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.Namespace;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

public class ConfigSupport {

    static final List<Path> standalonePaths = new ArrayList<>();
    static final List<Path> domainPaths = new ArrayList<>();

    static {
        standalonePaths.add(Paths.get("standalone", "configuration", "standalone.xml"));
        standalonePaths.add(Paths.get("standalone", "configuration", "standalone-full.xml"));
        standalonePaths.add(Paths.get("standalone", "configuration", "standalone-full-ha.xml"));
        standalonePaths.add(Paths.get("standalone", "configuration", "standalone-ha.xml"));
        domainPaths.add(Paths.get("domain", "configuration", "domain.xml"));
    }

    static void applyConfigChange(Path jbossHome, boolean enable, ConfigEditor editor) throws Exception {

        SAXBuilder jdom = new SAXBuilder();
        for (Path p : standalonePaths) {
            Path path = jbossHome.resolve(p);

            System.out.println("\tEnabling configuration on " + path);
            Document doc = jdom.build(path.toUri().toURL());

            editor.applyStandaloneConfigChange(enable, doc);

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

            editor.applyDomainConfigChange(enable, doc);

            XMLOutputter output = new XMLOutputter();
            output.setFormat(Format.getRawFormat());
            String newXML = output.outputString(doc);
            backup(path);
            writeFile(path, newXML, "UTF-8");
        }
    }

    static Path getJBossHome() throws UnsupportedEncodingException {
        String jbossHome = System.getProperty("jboss.home");
        if (jbossHome == null) {
            String path = ConfigMain.class.getProtectionDomain().getCodeSource().getLocation().getPath();
            String decodedPath = URLDecoder.decode(path, "UTF-8");
            String containingFolder = decodedPath.substring(0, decodedPath.lastIndexOf(File.separator));
            if (containingFolder.endsWith("bin")) {
                jbossHome = containingFolder.substring(0, containingFolder.lastIndexOf(File.separator));
            } else {
                throw new CommandException("The execution is not correct. This jar should be placed inside of ${JBOSS_HOME}/bin");
            }
        }

        Path standalonePath = Paths.get(jbossHome, "standalone", "configuration");
        Path domainPath = Paths.get(jbossHome, "domain", "configuration");

        if (!standalonePath.toFile().exists())
            throw new CommandException("Path to standalone configutration does not exist: " + standalonePath);

        if (!domainPath.toFile().exists())
            throw new CommandException("Path to domain configutration does not exist: " + standalonePath);

        return Paths.get(jbossHome);
    }

    private static Element createElementFromText(String xml) {
        SAXBuilder jdom = new SAXBuilder();
        Document doc;
        try {
            doc = jdom.build(new StringReader(xml));
        } catch (JDOMException | IOException ex) {
            throw new RuntimeException(ex);
        }
        return (Element) doc.getRootElement().clone();
    }

    static Element loadElementFrom(URL resource) {
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

    static void assertExists(Element extensions, String message) {
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
