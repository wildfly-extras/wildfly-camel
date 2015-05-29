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
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;

import de.pdark.decentxml.Attribute;
import de.pdark.decentxml.Document;
import de.pdark.decentxml.Element;
import de.pdark.decentxml.XMLParser;
import de.pdark.decentxml.XMLStringSource;

public class ConfigSupport {

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

    static String getJBossHome() throws UnsupportedEncodingException {
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

        String standalonePath = jbossHome + "/standalone/configuration";
        String domainPath = jbossHome + "/domain/configuration";
        File stanaloneFile = new File(standalonePath);
        File domainFile = new File(domainPath);

        if (!stanaloneFile.exists() || !domainFile.exists()) {
            throw new CommandException("\t The execution is not correct. This jar should be placed inside of ${JBOSS_HOME}/bin");
        }
        return jbossHome;
    }

    static final List<String> standalonePaths = new ArrayList<String>();
    static final List<String> domainPaths = new ArrayList<String>();

    static {
        standalonePaths.add("/standalone/configuration/standalone.xml");
        standalonePaths.add("/standalone/configuration/standalone-full.xml");
        standalonePaths.add("/standalone/configuration/standalone-full-ha.xml");
        standalonePaths.add("/standalone/configuration/standalone-ha.xml");
        domainPaths.add("/domain/configuration/domain.xml");
    }

    public static void applyConfigChange(String jbossHome, boolean enable, ConfigEditor editor) throws Exception {

        XMLParser parser = new XMLParser();
        for (String p : standalonePaths) {
            String path = jbossHome + p;

            System.out.println("\tEnabling configuration on " + path);
            String xml = readFile(path, "UTF-8");
            Document doc = parser.parse(new XMLStringSource(xml));

            editor.applyStandaloneConfigChange(enable, doc);

            String newXML = doc.toXML();
            if (!newXML.equals(xml)) {
                backup(path, xml);
                writeFile(path, newXML, "UTF-8");
            }

        }

        for (String p : domainPaths) {
            String path = jbossHome + p;

            System.out.println("\tEnabling configuration on " + path);
            String xml = readFile(path, "UTF-8");
            Document doc = parser.parse(new XMLStringSource(xml));

            editor.applyDomainConfigChange(enable, doc);

            String newXML = doc.toXML();
            if (!newXML.equals(xml)) {
                backup(path, xml);
                writeFile(path, newXML, "UTF-8");
            }
        }

    }

    static Element createElementFromText(String xml) {
        XMLParser parser = new XMLParser();
        Document doc = parser.parse(new XMLStringSource(xml));
        return doc.getRootElement();
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

    static byte[] loadBytesFromURL(URL resource) throws IOException {
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

    static Element findElementWithAttributeValue(Collection<Element> elements, String attrName, String attrValue) {
        if (elements == null)
            return null;
        for (Element element : elements) {
            Attribute attribute = element.getAttribute(attrName);
            if (attribute != null) {
                if (attrValue.equals(attribute.getValue())) {
                    return element;
                }
            }
        }
        return null;
    }

    static Element findElementWithStartingAttributeValue(Collection<Element> elements, String attrName, String attrValue) {
        if (elements == null)
            return null;
        for (Element element : elements) {
            Attribute attribute = element.getAttribute(attrName);
            if (attribute != null) {
                if (attribute.getValue().startsWith(attrValue)) {
                    return element;
                }
            }
        }
        return null;
    }

    static void assertExists(Element extensions, String message) {
        if (extensions == null) {
            throw new BadDocument(message);
        }
    }

    static void backup(String path, String xml) throws IOException {
        String name = path + ".bak";
        int counter = 2;
        while (Files.exists(Paths.get(name))) {
            name = path + ".bak" + counter;
            counter++;
        }
        writeFile(name, xml, "UTF-8");
    }

    static String readFile(String path, String encoding) throws IOException {
        byte[] encoded = Files.readAllBytes(Paths.get(path));
        return new String(encoded, encoding);
    }

    static Path writeFile(String path, String value, String encoding) throws IOException {
        byte[] bytes = value.getBytes(encoding);
        return Files.write(Paths.get(path), bytes, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE);
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

    static List<Element> findProfileElements(Document doc) {
        Element profile = doc.getRootElement().getChild("profile");
        if (profile != null) {
            return Collections.singletonList(profile);
        }

        Element profiles = doc.getRootElement().getChild("profiles");
        if (profiles != null) {
            return profiles.getChildren("profile");
        }
        assertExists(null, "Did not find the <profile> element");
        return null;
    }

}
