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

import java.net.URL;
import java.util.LinkedHashMap;
import java.util.List;

import de.pdark.decentxml.Document;
import de.pdark.decentxml.Element;
import de.pdark.decentxml.Text;

final class WildflyCamelConfigEditor implements ConfigEditor {

    @Override
    public void applyStandaloneConfigChange(boolean enable, Document doc) {
        updateExtension(enable, doc);
        updateSubsystem(enable, doc);
        updateWeldConfig(enable, doc);
        updateHawtIOSystemProperties(enable, doc);
        updateHawtIOSecurityDomain(enable, doc);
    }

    @Override
    public void applyDomainConfigChange(boolean enable, Document doc) {
        applyStandaloneConfigChange(enable, doc);
        updateServergroup(enable, doc);
    }

    private static void updateExtension(boolean enable, Document doc) {
        Element extensions = doc.getRootElement().getChild("extensions");
        ConfigSupport.assertExists(extensions, "Did not find the <extensions> element");
        Element element = ConfigSupport.findElementWithAttributeValue(extensions.getChildren("extension"), "module", "org.wildfly.extension.camel");
        if (enable && element == null) {
            extensions.addNodes(new Text("    "), new Element("extension").addAttribute("module", "org.wildfly.extension.camel"), new Text("\n    "));
        }
        if (!enable && element != null) {
            element.remove();
        }
    }

    private static void updateWeldConfig(boolean enable, Document doc) {
        List<Element> profiles = ConfigSupport.findProfileElements(doc);
        for (Element profile : profiles) {
            Element weld = ConfigSupport.findElementWithStartingAttributeValue(profile.getChildren("subsystem"), "xmlns", "urn:jboss:domain:weld:");
            ConfigSupport.assertExists(weld, "Did not find the weld subsystem");
            if (enable) {
                weld.setAttribute("require-bean-descriptor", "true");
            } else {
                weld.removeAttribute("require-bean-descriptor");
            }
        }
    }

    private static void updateSubsystem(boolean enable, Document doc) {
        List<Element> profiles = ConfigSupport.findProfileElements(doc);
        for (Element profile : profiles) {
            Element camel = ConfigSupport.findElementWithStartingAttributeValue(profile.getChildren("subsystem"), "xmlns", "urn:jboss:domain:camel:");
            if (enable && camel == null) {
                URL resource = WildflyCamelConfigEditor.class.getResource("camel-subsystem.xml");
                profile.addNodes(new Text("    "), ConfigSupport.loadElementFrom(resource), new Text("\n    "));
            }
            if (!enable && camel != null) {
                camel.remove();
            }
        }
    }

    private static void updateHawtIOSystemProperties(boolean enable, Document doc) {
        Element systemProperties = doc.getRootElement().getChild("system-properties");
        if (systemProperties == null) {
            systemProperties = new Element("system-properties");
            systemProperties.addNode(new Text("\n    "));

            int pos = doc.getRootElement().nodeIndexOf(doc.getRootElement().getChild("extensions"));
            doc.getRootElement().addNodes(pos + 1, new Text("    "), systemProperties, new Text("\n"));
        }

        LinkedHashMap<String, Element> propertiesByName = ConfigSupport.mapByAttributeName(systemProperties.getChildren(), "name");
        if (enable) {
            addProperty(systemProperties, propertiesByName, "hawtio.authenticationEnabled", "true");
            addProperty(systemProperties, propertiesByName, "hawtio.offline", "true");
            addProperty(systemProperties, propertiesByName, "hawtio.realm", "hawtio-domain");
            addProperty(systemProperties, propertiesByName, "hawtio.role", "admin");
        } else {
            rmProperty(propertiesByName, "hawtio.authenticationEnabled");
            rmProperty(propertiesByName, "hawtio.offline");
            rmProperty(propertiesByName, "hawtio.realm");
            rmProperty(propertiesByName, "hawtio.role");
        }
    }

    private static void updateServergroup(boolean enable, Document doc) {
        Element serverGroups = doc.getRootElement().getChild("server-groups");
        Element camel = ConfigSupport.findElementWithAttributeValue(serverGroups.getChildren("server-group"), "name", "camel-server-group");
        if (enable && camel == null) {
            URL resource = WildflyCamelConfigEditor.class.getResource("camel-servergroup.xml");
            serverGroups.addNodes(new Text("    "), ConfigSupport.loadElementFrom(resource), new Text("\n    "));
        }
        if (!enable && camel != null) {
            camel.remove();
        }
    }

    private static void addProperty(Element systemProperties, LinkedHashMap<String, Element> propertiesByName, String name, String value) {
        if (!propertiesByName.containsKey(name)) {
            systemProperties.addNodes(new Text("   "), new Element("property").setAttribute("name", name).setAttribute("value", value), new Text("\n    "));

        }
    }

    private static void rmProperty(LinkedHashMap<String, Element> propertiesByName, String name) {
        Element element = propertiesByName.get(name);
        if (element != null) {
            element.remove();
        }
    }

    private static void updateHawtIOSecurityDomain(boolean enable, Document doc) {
        List<Element> profiles = ConfigSupport.findProfileElements(doc);
        for (Element profile : profiles) {
            Element security = ConfigSupport.findElementWithStartingAttributeValue(profile.getChildren("subsystem"), "xmlns", "urn:jboss:domain:security:");
            ConfigSupport.assertExists(security, "Did not find the security subsystem");
            Element domains = security.getChild("security-domains");
            ConfigSupport.assertExists(domains, "Did not find the <security-domains> element");
            Element hawtioDomain = ConfigSupport.findElementWithAttributeValue(domains.getChildren("security-domain"), "name", "hawtio-domain");
            if (enable && hawtioDomain == null) {
                URL resource = WildflyCamelConfigEditor.class.getResource("hawtio-security-domain.xml");
                domains.addNodes(new Text("    "), ConfigSupport.loadElementFrom(resource), new Text("\n            "));
            }
            if (!enable && hawtioDomain != null) {
                hawtioDomain.remove();
            }
        }
    }
}
