/*
 * Copyright 2015 JBoss Inc
 *
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
 */
package org.wildfly.extension.camel.config;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;

import de.pdark.decentxml.Attribute;
import de.pdark.decentxml.Document;
import de.pdark.decentxml.Element;
import de.pdark.decentxml.Text;
import de.pdark.decentxml.XMLParser;
import de.pdark.decentxml.XMLStringSource;


/**
 * Executes the xslt fuse integration transformations on the EAP configuration
 * files
 *
 * @author David Virgil Naranjo 2015 Red Hat Inc.
 */
abstract public class AbstractConfigEditor {

    public static final List<String> standalonePaths = new ArrayList<String>();
    public static final List<String> domainPaths = new ArrayList<String>();

    static {
        standalonePaths.add("/standalone/configuration/standalone.xml");
        standalonePaths.add("/standalone/configuration/standalone-full.xml");
        standalonePaths.add("/standalone/configuration/standalone-full-ha.xml");
        standalonePaths.add("/standalone/configuration/standalone-ha.xml");
        domainPaths.add("/domain/configuration/domain.xml");
    }

    /**
     * Apply xslt.
     *
     * @param enable
     *            the enable
     * @param jbossHome
     *            the jboss home
     * @throws Exception
     *             the exception
     */
    public void applyConfigChange(boolean enable, String jbossHome) throws Exception {

        if (enable) {
            for (String path : standalonePaths) {
                enableStandaloneConfig(jbossHome + path);
            }
            for (String path : domainPaths) {
                enableDomainConfig(jbossHome + path);
            }
        } else {
            for (String path : standalonePaths) {
                disableStandaloneConfig(jbossHome + path);
            }
            for (String path : domainPaths) {
                disableDomainConfig(jbossHome + path);
            }
        }
    }

    protected abstract void disableStandaloneConfig(Document doc);
    protected abstract void disableDomainConfig(Document doc);
    protected abstract void enableDomainConfig(Document doc);
    protected abstract void enableStandaloneConfig(Document doc);

    protected void enableStandaloneConfig(String path) throws IOException {
        System.out.println("\tEnabling configuration on " + path);
        String xml = readFile(path, "UTF-8");
        XMLParser parser = new XMLParser ();
        Document doc = parser.parse(new XMLStringSource(xml));
        enableStandaloneConfig(doc);
        backup(path, xml);
        writeFile(path, doc.toXML(), "UTF-8");
    }

    protected void disableStandaloneConfig(String path) throws IOException {
        System.out.println("\tDisabling configuration on " + path);
        String xml = readFile(path, "UTF-8");
        XMLParser parser = new XMLParser ();
        Document doc = parser.parse(new XMLStringSource(xml));
        disableStandaloneConfig(doc);
        backup(path, xml);
        writeFile(path, doc.toXML(), "UTF-8");
    }

    protected void enableDomainConfig(String path) throws IOException {
        System.out.println("\tEnabling configuration on " + path);
        String xml = readFile(path, "UTF-8");
        XMLParser parser = new XMLParser ();
        Document doc = parser.parse(new XMLStringSource(xml));
        enableDomainConfig(doc);
        backup(path, xml);
        writeFile(path, doc.toXML(), "UTF-8");
    }

    protected void backup(String path, String xml) throws IOException {
        String name = path+".bak";
        int counter = 2;
        while( Files.exists(Paths.get(name) ) ) {
            name = path+".bak"+counter;
            counter++;
        }
        writeFile(name, xml, "UTF-8");
    }

    protected void disableDomainConfig(String path) throws IOException {
        System.out.println("\tDisabling configuration on " + path);
        String xml = readFile(path, "UTF-8");
        XMLParser parser = new XMLParser ();
        Document doc = parser.parse(new XMLStringSource(xml));
        disableDomainConfig(doc);
        backup(path, xml);
        writeFile(path, doc.toXML(), "UTF-8");
    }

    protected static Element loadElementFrom(URL resource) {
        try {
            byte []data = loadBytesFromURL(resource);
            String xml = new String(data, "UTF-8");
            XMLParser parser = new XMLParser ();
            Document doc = parser.parse(new XMLStringSource(xml));
            return doc.getRootElement();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected static byte[] loadBytesFromURL(URL resource) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        final byte[] buffer = new byte[10234];
        try (InputStream in = resource.openStream()) {
            while ( true ) {
                int count = in.read(buffer, 0, buffer.length);
                if (count < 0)
                    break;
                out.write(buffer, 0, count);
            }
        }
        return out.toByteArray();
    }

    protected static Element findElementWithAttributeValue(Collection<Element> elements, String attrName, String attrValue) {
        if( elements == null )
            return null;
        for (Element element : elements) {
            Attribute attribute = element.getAttribute(attrName);
            if( attribute!=null ) {
                if( attrValue.equals(attribute.getValue()) ) {
                    return element;
                }
            }
        }
        return null;
    }

    protected static Element findElementWithStartingAttributeValue(Collection<Element> elements, String attrName, String attrValue) {
        if( elements == null )
            return null;
        for (Element element : elements) {
            Attribute attribute = element.getAttribute(attrName);
            if( attribute!=null ) {
                if( attribute.getValue().startsWith(attrValue) ) {
                    return element;
                }
            }
        }
        return null;
    }

    protected static void assertExists(Element extensions, String message) {
        if( extensions==null ) {
            throw new BadDocument(message);
        }
    }

    public static class BadDocument extends RuntimeException {
        public BadDocument(String message) {
            super(message);
        }
    }

    protected static String readFile(String path, String encoding) throws IOException {
        byte[] encoded = Files.readAllBytes(Paths.get(path));
        return new String(encoded, encoding);
    }

    protected static Path writeFile(String path, String value, String encoding) throws IOException {
        byte[] bytes = value.getBytes(encoding);
        return Files.write(Paths.get(path), bytes, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE);
    }

    protected static LinkedHashMap<String, Element> mapByAttributeName(List<Element> elements, String attrName) {
        LinkedHashMap<String, Element> rc = new LinkedHashMap<String, Element>();
        for (Element element : elements) {
            Attribute attribute = element.getAttribute(attrName);
            if( attribute!=null ) {
                rc.put(attribute.getValue(), element);
            }
        }
        return rc;
    }


}
