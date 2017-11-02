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
package org.wildfly.camel.test.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Properties;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.Namespace;
import org.jdom.input.SAXBuilder;
import org.jdom.xpath.XPath;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;


public class VersionsValidatorTest {

    HashMap<String, String> mapping = new LinkedHashMap<>();
    List<String> problems = new ArrayList<>();

    Element camelRoot;
    Element wfcRoot;
    Element wfRoot;

    @Before
    public void before() throws Exception {
        camelRoot = getRootNode("camel-parent");
        wfcRoot = getRootNode("wildfly-camel");
        wfRoot = getRootNode("wildfly-parent");

        mapping.put("version.camel.apns", "java-apns-version");
        mapping.put("version.camel.jgroups", "jgroups-version");
        mapping.put("version.camel.log4j2", "log4j2-version");
        mapping.put("version.camel.lucene3", "lucene3-version");
        mapping.put("version.camel.hadoop2", "hadoop2-version");
        mapping.put("version.camel.hadoop2.protobuf", "hadoop2-protobuf-version");
        mapping.put("version.camel.hbase", "hbase-version");
        mapping.put("version.camel.netty41", "netty-version");
        mapping.put("version.camel.qpid.proton", "qpid-proton-j-version");
        mapping.put("version.wildfly.arquillian", "version.org.wildfly.arquillian");
        mapping.put("version.wildfly.cxf", "version.org.apache.cxf");
        mapping.put("version.wildfly.fasterxml.jackson", "version.com.fasterxml.jackson");
        mapping.put("version.wildfly.infinispan", "version.org.infinispan");
    }

    @Test
    public void testVersions() throws Exception {

        XPath xpath = XPath.newInstance("/ns:project/ns:properties");
        xpath.addNamespace(Namespace.getNamespace("ns", "http://maven.apache.org/POM/4.0.0"));
        Element node = (Element) xpath.selectSingleNode(wfcRoot);
        for (Object child : node.getChildren()) {
            String wfcKey = ((Element) child).getName();
            String wfcVal = ((Element) child).getText();
            String targetVal = getTargetValue(wfcKey);
            if (targetVal != null) {
                if (!targetVal.equals(wfcVal) && !targetVal.startsWith(wfcVal + ".redhat")) {
                    problems.add(wfcKey + ": " + wfcVal + " => " + targetVal);
                }
            }
        }
        for (String line : problems) {
            System.err.println(line);
        }
        Assert.assertEquals("Mapping problems", 0, problems.size());
    }

    public String getTargetValue(String wfcKey) throws JDOMException {
        
        Element rootNode;
        if (wfcKey.startsWith("version.camel.")) {
            rootNode = camelRoot;
        } else if (wfcKey.startsWith("version.wildfly.")) {
            rootNode = wfRoot;
        } else {
            return null;
        }

        String targetKey = mapping.get(wfcKey);
        if (targetKey == null) {
            problems.add("Cannot find mapping for: " + wfcKey);
            return null;
        }
        XPath xpath = XPath.newInstance("/ns:project/ns:properties/ns:" + targetKey);
        xpath.addNamespace(Namespace.getNamespace("ns", "http://maven.apache.org/POM/4.0.0"));
        Element propNode = (Element) xpath.selectSingleNode(rootNode);
        if (propNode == null) {
            problems.add("Cannot obtain target property: " + targetKey);
            return null;
        }
        return propNode.getText();
    }

    private Element getRootNode(String pomKey) throws IOException, JDOMException {

        Properties props = new Properties();
        try (InputStream input = new FileInputStream("target/pom-paths.txt")) {
            props.load(input);
        }

        String pomPath = props.getProperty(pomKey);
        File xmlFile = new File(pomPath);

        SAXBuilder builder = new SAXBuilder();
        Document document = (Document) builder.build(xmlFile);
        return document.getRootElement();
    }
}