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
import static org.wildfly.extension.camel.config.WildFlyCamelConfigPlugin.NS_DOMAIN;
import static org.wildfly.extension.camel.config.WildFlyCamelConfigPlugin.NS_SECURITY;
import static org.wildfly.extension.camel.config.WildFlyCamelConfigPlugin.NS_WELD;

import java.net.URL;
import java.nio.file.Paths;
import java.util.List;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.junit.Assert;
import org.junit.Test;
import org.wildfly.extension.camel.config.WildFlyCamelConfigPlugin;
import org.wildfly.extras.config.ConfigContext;
import org.wildfly.extras.config.ConfigPlugin;
import org.wildfly.extras.config.ConfigSupport;

public class StandaloneConfigTest {

    @Test
    public void testStandaloneConfig() throws Exception {

        URL resurl = StandaloneConfigTest.class.getResource("/standalone.xml");
        SAXBuilder jdom = new SAXBuilder();
        Document doc = jdom.build(resurl);

        ConfigContext context = ConfigSupport.createContext(null, Paths.get(resurl.toURI()), doc);
        ConfigPlugin plugin = new WildFlyCamelConfigPlugin();
        plugin.applyStandaloneConfigChange(context, true);

        // Verify extension
        Element element = ConfigSupport.findElementWithAttributeValue(doc.getRootElement(), "extension", NS_DOMAIN, "module", "org.wildfly.extension.camel");
        Assert.assertNotNull("Extension not null", element);

        // Verify system-properties
        element = doc.getRootElement().getChild("system-properties", NS_DOMAIN);
        Assert.assertNotNull("system-properties not null", element);
        element = ConfigSupport.findElementWithAttributeValue(element, "property", NS_DOMAIN, "name", "hawtio.realm");
        Assert.assertNotNull("property not null", element);

        // Verify weld
        List<Element> profiles = ConfigSupport.findProfileElements(doc, NS_DOMAIN);
        Assert.assertEquals("One profile", 1, profiles.size());
        element = profiles.get(0).getChild("subsystem", NS_WELD);
        Assert.assertNotNull("weld not null", element);
        Assert.assertEquals("true", element.getAttribute("require-bean-descriptor").getValue());

        // Verify camel
        element = profiles.get(0).getChild("subsystem", NS_CAMEL);
        Assert.assertNotNull("camel not null", element);

        // Verify hawtio-domain
        element = ConfigSupport.findElementWithAttributeValue(doc.getRootElement(), "security-domain", NS_SECURITY, "name", "hawtio-domain");
        Assert.assertNotNull("hawtio-domain not null", element);

        XMLOutputter output = new XMLOutputter();
        output.setFormat(Format.getRawFormat());
        //System.out.println(output.outputString(doc));
    }
}
