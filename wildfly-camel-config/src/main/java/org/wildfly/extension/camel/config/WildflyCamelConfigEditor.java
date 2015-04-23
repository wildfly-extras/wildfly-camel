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

import java.util.LinkedHashMap;

import de.pdark.decentxml.*;

/**
 */
public class WildflyCamelConfigEditor extends AbstractConfigEditor {

    @Override
    protected void enableStandaloneConfig(Document doc) {
        addCamelExtension(doc);
        addCamelSubsystem(doc);
        enableWeldConfig(doc);
        addHawtIOSystemProperties(doc);
        addHawtIOSecurityDomain(doc);
    }

    @Override
    protected void disableStandaloneConfig(Document doc) {
        rmCamelExtension(doc);
        rmCamelSubsystem(doc);
        disableWeldConfig(doc);
        rmHawtIOSystemProperties(doc);
        rmHawtIOSecurityDomain(doc);
    }

    @Override
    protected void enableDomainConfig(Document doc) {

    }

    @Override
    protected void disableDomainConfig(Document doc) {

    }

    protected void addCamelExtension(Document doc) {
        Element extensions = doc.getRootElement().getChild("extensions");
        assertExists(extensions, "Did not find the <extensions> element");
        Element element = findElementWithAttributeValue(extensions.getChildren("element"), "module", "org.wildfly.extension.camel");
        if( element== null ) {
            element = new Element ("element");
            element.addAttribute("module", "org.wildfly.extension.camel");
            extensions.addNodes(
                    new Text("    "),
                    element,
                    new Text("\n    ")
            );
        }
    }

    protected void rmCamelExtension(Document doc) {
        Element extensions = doc.getRootElement().getChild("extensions");
        assertExists(extensions, "Did not find the <extensions> element");
        Element element = findElementWithAttributeValue(extensions.getChildren("element"), "module", "org.wildfly.extension.camel");
        element.remove();
    }

    protected void enableWeldConfig(Document doc) {
        Element profile = doc.getRootElement().getChild("profile");
        assertExists(profile, "Did not find the <profile> element");
        Element weld = findElementWithStartingAttributeValue(profile.getChildren("subsystem"), "xmlns", "urn:jboss:domain:weld:");
        assertExists(weld, "Did not find the weld subsystem");
        weld.setAttribute("require-bean-descriptor", "true");
    }

    protected void disableWeldConfig(Document doc) {
        Element profile = doc.getRootElement().getChild("profile");
        assertExists(profile, "Did not find the <profile> element");
        Element weld = findElementWithStartingAttributeValue(profile.getChildren("subsystem"), "xmlns", "urn:jboss:domain:weld:");
        assertExists(weld, "Did not find the weld subsystem");
        weld.removeAttribute("require-bean-descriptor");
    }

    protected void addCamelSubsystem(Document doc) {
        Element profile = doc.getRootElement().getChild("profile");
        assertExists(profile, "Did not find the <profile> element");
        Element camel = findElementWithStartingAttributeValue(profile.getChildren("subsystem"), "xmlns", "urn:jboss:domain:camel:");
        if( camel == null ) {
            camel = loadElementFrom(getClass().getResource("camel-subsystem.xml"));
            profile.addNodes(
                    new Text("    "),
                    camel,
                    new Text("\n    ")
            );
        }

    }

    protected void rmCamelSubsystem(Document doc) {
        Element profile = doc.getRootElement().getChild("profile");
        assertExists(profile, "Did not find the <profile> element");
        Element camel = findElementWithStartingAttributeValue(profile.getChildren("subsystem"), "xmlns", "urn:jboss:domain:camel:");
        camel.remove();
    }

    protected void addHawtIOSystemProperties(Document doc) {
        Element systemProperties = doc.getRootElement().getChild("system-properties");
        if( systemProperties == null ) {
            systemProperties = new Element ("system-properties");
            systemProperties.addNode(new Text("\n    "));
            doc.getRootElement().addNodes(
                    new Text("    "),
                    systemProperties,
                    new Text("\n")
            );
        }

        LinkedHashMap<String, Element> propertiesByName = mapByAttributeName(systemProperties.getChildren(), "name");
        addProperty(systemProperties, propertiesByName, "hawtio.authenticationEnabled", "true");
        addProperty(systemProperties, propertiesByName, "hawtio.offline", "true");
        addProperty(systemProperties, propertiesByName, "hawtio.realm", "hawtio-domain");
        addProperty(systemProperties, propertiesByName, "hawtio.role", "admin");
    }

    protected void rmHawtIOSystemProperties(Document doc) {
        Element systemProperties = doc.getRootElement().getChild("system-properties");
        if( systemProperties == null ) {
            return;
        }

        LinkedHashMap<String, Element> propertiesByName = mapByAttributeName(systemProperties.getChildren(), "name");
        rmProperty(propertiesByName, "hawtio.authenticationEnabled");
        rmProperty(propertiesByName, "hawtio.offline");
        rmProperty(propertiesByName, "hawtio.realm");
        rmProperty(propertiesByName, "hawtio.role");
    }

    private void addProperty(Element systemProperties, LinkedHashMap<String, Element> propertiesByName, String name, String value) {
        if( !propertiesByName.containsKey(name) ) {
            systemProperties.addNodes(
                    new Text("   "),
                    new Element("property").
                            setAttribute("name", name).
                            setAttribute("value", value),
                    new Text("\n    "));

        }
    }

    private void rmProperty(LinkedHashMap<String, Element> propertiesByName, String name) {
        Element element = propertiesByName.get(name);
        if( element!=null ) {
            element.remove();
        }
    }

    protected void addHawtIOSecurityDomain(Document doc) {
        Element profile = doc.getRootElement().getChild("profile");
        assertExists(profile, "Did not find the <profile> element");
        Element security = findElementWithStartingAttributeValue(profile.getChildren("subsystem"), "xmlns", "urn:jboss:domain:security:");
        assertExists(security, "Did not find the security subsystem");
        Element domains = security.getChild("security-domains");
        assertExists(domains, "Did not find the <security-domains> element");
        Element hawtioDomain = findElementWithAttributeValue(domains.getChildren("security-domain"), "name", "hawtio-domain");
        if( hawtioDomain == null ) {
            hawtioDomain = loadElementFrom(getClass().getResource("hawtio-security-domain.xml"));
            domains.addNodes(
                    new Text("    "),
                    hawtioDomain,
                    new Text("\n            ")
            );
        }


    }

    protected void rmHawtIOSecurityDomain(Document doc) {
        Element profile = doc.getRootElement().getChild("profile");
        assertExists(profile, "Did not find the <profile> element");
        Element security = findElementWithStartingAttributeValue(profile.getChildren("subsystem"), "xmlns", "urn:jboss:domain:security:");
        assertExists(security, "Did not find the security subsystem");
        Element domains = security.getChild("security-domains");
        assertExists(domains, "Did not find the <security-domains> element");
        Element hawtioDomain = findElementWithAttributeValue(domains.getChildren("security-domain"), "name", "hawtio-domain");
        hawtioDomain.remove();
    }

}
