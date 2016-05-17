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

import static org.wildfly.extension.camel.config.WildFlyCamelConfigPlugin.NS_CAMEL;
import static org.wildfly.extension.camel.config.WildFlyCamelConfigPlugin.NS_DOMAINS;
import static org.wildfly.extension.camel.config.WildFlyCamelConfigPlugin.NS_DOMAIN_50;
import static org.wildfly.extension.camel.config.WildFlyCamelConfigPlugin.NS_SECURITY;

import java.io.File;
import java.io.FileOutputStream;
import java.net.URL;
import java.nio.file.Paths;
import java.util.List;

import org.jdom.Attribute;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.Namespace;
import org.jdom.input.SAXBuilder;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.wildfly.extension.camel.config.WildFlyCamelConfigPlugin;
import org.wildfly.extras.config.ConfigContext;
import org.wildfly.extras.config.ConfigException;
import org.wildfly.extras.config.ConfigPlugin;
import org.wildfly.extras.config.ConfigSupport;

public class StandaloneConfigTest extends ConfigTestSupport {

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Test
    public void testStandaloneConfig() throws Exception {
        URL resurl = StandaloneConfigTest.class.getResource("/standalone.xml");
        SAXBuilder jdom = new SAXBuilder();
        Document doc = jdom.build(resurl);

        ConfigPlugin plugin = new WildFlyCamelConfigPlugin();
        ConfigContext context = ConfigSupport.createContext(null, Paths.get(resurl.toURI()), doc);
        plugin.applyStandaloneConfigChange(context, true);

        // Verify extension
        assertElementWithAttributeValueNotNull(doc.getRootElement(), "extension", "module", "org.wildfly.extension.camel", NS_DOMAINS);

        // Verify system-properties
        Element element = ConfigSupport.findChildElement(doc.getRootElement(), "system-properties", NS_DOMAINS);
        Assert.assertNotNull("system-properties not null", element);
        assertElementWithAttributeValueNotNull(element, "property", "name", "hawtio.realm", NS_DOMAINS);

        // Verify camel
        List<Element> profiles = ConfigSupport.findProfileElements(doc, NS_DOMAINS);
        Assert.assertEquals("One profile", 1, profiles.size());
        assertElementNotNull(profiles.get(0), "subsystem", NS_CAMEL);

        // Verify hawtio-domain
        assertElementWithAttributeValueNotNull(doc.getRootElement(), "security-domain", "name", "hawtio-domain", NS_SECURITY);

        //outputDocumentContent(doc, System.out);
    }

    @Test
    public void testUnsupportedNamespaceVersion() throws Exception {
        URL resurl = StandaloneConfigTest.class.getResource("/standalone.xml");
        SAXBuilder jdom = new SAXBuilder();
        Document doc = jdom.build(resurl);

        doc.getRootElement().getChild("extensions", NS_DOMAIN_50).setNamespace(Namespace.getNamespace("urn:jboss:domain:99.99"));

        File modifiedConfig = new File("target/standalone-modified.xml");
        outputDocumentContent(doc, new FileOutputStream(modifiedConfig));

        jdom = new SAXBuilder();
        doc = jdom.build(modifiedConfig);

        ConfigContext context = ConfigSupport.createContext(null, modifiedConfig.toPath(), doc);
        ConfigPlugin plugin = new WildFlyCamelConfigPlugin();

        expectedException.expect(ConfigException.class);

        plugin.applyStandaloneConfigChange(context, true);
    }

    @Test
    public void testApplyConfigMultipleTimes() throws Exception {
        URL resurl = StandaloneConfigTest.class.getResource("/standalone.xml");
        SAXBuilder jdom = new SAXBuilder();
        Document doc = jdom.build(resurl);

        ConfigPlugin plugin = new WildFlyCamelConfigPlugin();

        ConfigContext context = ConfigSupport.createContext(null, Paths.get(resurl.toURI()), doc);

        for (int i = 0; i < 5; i++) {
            plugin.applyStandaloneConfigChange(context, true);
        }

        Assert.assertEquals(1, getElementCount(doc, "extension", null, new Attribute("module", "org.wildfly.extension.camel")));
        Assert.assertEquals(1, getElementCount(doc, "system-properties", null, null));
        Assert.assertEquals(1, getElementCount(doc, "security-domain", null, new Attribute("name", "hawtio-domain")));
        Assert.assertEquals(1, getElementCount(doc, "subsystem", NS_CAMEL, null));
    }

    @Test
    public void testRemoveConfig() throws Exception {
        URL resurl = StandaloneConfigTest.class.getResource("/standalone.xml");
        SAXBuilder jdom = new SAXBuilder();
        Document doc = jdom.build(resurl);

        File modifiedConfig = new File("target/standalone-modified.xml");
        outputDocumentContent(doc, new FileOutputStream(modifiedConfig));

        jdom = new SAXBuilder();
        doc = jdom.build(modifiedConfig);

        ConfigPlugin plugin = new WildFlyCamelConfigPlugin();

        ConfigContext context = ConfigSupport.createContext(null, Paths.get(resurl.toURI()), doc);
        plugin.applyStandaloneConfigChange(context, false);

        // Verify extension removed
        assertElementWithAttributeValueNull(doc.getRootElement(), "extension", "module", "org.wildfly.extension.camel", NS_DOMAINS);

        // Verify system-properties removed
        Element element = ConfigSupport.findChildElement(doc.getRootElement(), "system-properties", NS_DOMAINS);
        Assert.assertNotNull("system-properties not null", element);
        assertElementWithAttributeValueNull(element, "property", "name", "hawtio.realm", NS_DOMAINS);
        assertElementWithAttributeValueNull(element, "property", "name", "hawtio.offline", NS_DOMAINS);
        assertElementWithAttributeValueNull(element, "property", "name", "hawtio.authenticationEnabled", NS_DOMAINS);

        List<Element> profiles = ConfigSupport.findProfileElements(doc, NS_DOMAINS);

        // Verify camel removed
        assertElementNull(profiles.get(0), "subsystem", NS_CAMEL);

        // Verify hawtio-domain removed
        assertElementWithAttributeValueNull(doc.getRootElement(), "security-domain", "name", "hawtio-domain", NS_SECURITY);
    }
}