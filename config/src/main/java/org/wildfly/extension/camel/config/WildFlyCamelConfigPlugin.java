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
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;

import org.jdom.Element;
import org.jdom.Namespace;
import org.jdom.Text;

public final class WildFlyCamelConfigPlugin implements ConfigPlugin {

    public static LayerConfig FUSE_LAYER_CONFIG = new LayerConfig("fuse", LayerConfig.Type.INSTALLING, -10);
    
    public static final Namespace NS_DOMAIN = Namespace.getNamespace("urn:jboss:domain:3.0");
    public static final Namespace NS_CAMEL = Namespace.getNamespace("urn:jboss:domain:camel:1.0");
    public static final Namespace NS_LOGGING = Namespace.getNamespace("urn:jboss:domain:logging:1.5");
    public static final Namespace NS_SECURITY = Namespace.getNamespace("urn:jboss:domain:security:1.2");
    public static final Namespace NS_WELD = Namespace.getNamespace("urn:jboss:domain:weld:2.0");

    @Override
    public String getConfigName() {
        return "camel";
    }

    @Override
    public List<LayerConfig> getLayerConfigs() {
        return Arrays.asList(FUSE_LAYER_CONFIG);
    }

    @Override
    public void applyStandaloneConfigChange(ConfigContext context, boolean enable) {
        updateExtension(context, enable);
        updateSystemProperties(context, enable);
        updateSubsystem(context, enable);
        updateWeldConfig(context, enable);
        updateSecurityDomain(context, enable);
    }

    @Override
    public void applyDomainConfigChange(ConfigContext context, boolean enable) {
        applyStandaloneConfigChange(context, enable);
        updateServergroup(enable, context);
    }

    private static void updateExtension(ConfigContext context, boolean enable) {
        Element extensions = context.getDocument().getRootElement().getChild("extensions", NS_DOMAIN);
        ConfigSupport.assertExists(extensions, "Did not find the <extensions> element");
        Element element = ConfigSupport.findElementWithAttributeValue(extensions, "extension", NS_DOMAIN, "module", "org.wildfly.extension.camel");
        if (enable && element == null) {
            extensions.addContent(new Text("    "));
            extensions.addContent(new Element("extension", NS_DOMAIN).setAttribute("module", "org.wildfly.extension.camel"));
            extensions.addContent(new Text("\n    "));
        }
        if (!enable && element != null) {
            element.getParentElement().removeContent(element);
        }
    }

    @SuppressWarnings("unchecked")
    private static void updateSystemProperties(ConfigContext context, boolean enable) {
        Element rootElement = context.getDocument().getRootElement();
        Element element = rootElement.getChild("system-properties", NS_DOMAIN);
        if (element == null) {
            element = new Element("system-properties", NS_DOMAIN);
            element.addContent(new Text("\n    "));

            int pos = rootElement.indexOf(rootElement.getChild("extensions", NS_DOMAIN));
            rootElement.addContent(pos + 1, new Text("    "));
            rootElement.addContent(pos + 1, element);
            rootElement.addContent(pos + 1, new Text("\n"));
        }

        LinkedHashMap<String, Element> propertiesByName = ConfigSupport.mapByAttributeName(element.getChildren(), "name");
        if (enable) {
            addProperty(element, propertiesByName, "hawtio.authenticationEnabled", "true");
            addProperty(element, propertiesByName, "hawtio.offline", "true");
            addProperty(element, propertiesByName, "hawtio.realm", "hawtio-domain");
        } else {
            rmProperty(propertiesByName, "hawtio.authenticationEnabled");
            rmProperty(propertiesByName, "hawtio.offline");
            rmProperty(propertiesByName, "hawtio.realm");
        }
    }

    private static void updateSubsystem(ConfigContext context, boolean enable) {
        List<Element> profiles = ConfigSupport.findProfileElements(context.getDocument(), NS_DOMAIN);
        for (Element profile : profiles) {
            Element element = profile.getChild("subsystem", NS_CAMEL);
            if (enable && element == null) {
                URL resource = WildFlyCamelConfigPlugin.class.getResource("/camel-subsystem.xml");
                profile.addContent(new Text("    "));
                profile.addContent(ConfigSupport.loadElementFrom(resource));
                profile.addContent(new Text("\n    "));
            }
            if (!enable && element != null) {
                element.getParentElement().removeContent(element);
            }
        }
    }

    private static void updateWeldConfig(ConfigContext context, boolean enable) {
        List<Element> profiles = ConfigSupport.findProfileElements(context.getDocument(), NS_DOMAIN);
        for (Element profile : profiles) {
            Element weld = profile.getChild("subsystem", NS_WELD);
            ConfigSupport.assertExists(weld, "Did not find the weld subsystem");
            if (enable) {
                weld.setAttribute("require-bean-descriptor", "true");
            } else {
                weld.removeAttribute("require-bean-descriptor");
            }
        }
    }

    private static void updateSecurityDomain(ConfigContext context, boolean enable) {
        List<Element> profiles = ConfigSupport.findProfileElements(context.getDocument(), NS_DOMAIN);
        for (Element profile : profiles) {
            Element security = profile.getChild("subsystem", NS_SECURITY);
            ConfigSupport.assertExists(security, "Did not find the security subsystem");
            Element domains = security.getChild("security-domains", NS_SECURITY);
            ConfigSupport.assertExists(domains, "Did not find the <security-domains> element");
            Element domain = ConfigSupport.findElementWithAttributeValue(domains, "security-domain", NS_SECURITY, "name", "hawtio-domain");
            if (enable && domain == null) {
                URL resource = WildFlyCamelConfigPlugin.class.getResource("/hawtio-security-domain.xml");
                domains.addContent(new Text("    "));
                domains.addContent(ConfigSupport.loadElementFrom(resource));
                domains.addContent(new Text("\n            "));
            }
            if (!enable && domain != null) {
                domain.getParentElement().removeContent(domain);
            }
        }
    }

    private static void updateServergroup(boolean enable, ConfigContext context) {
        Element serverGroups = context.getDocument().getRootElement().getChild("server-groups", NS_DOMAIN);
        Element camel = ConfigSupport.findElementWithAttributeValue(serverGroups, "server-group", NS_DOMAIN, "name", "camel-server-group");
        if (enable && camel == null) {
            URL resource = WildFlyCamelConfigPlugin.class.getResource("/camel-servergroup.xml");
            serverGroups.addContent(new Text("    "));
            serverGroups.addContent(ConfigSupport.loadElementFrom(resource));
            serverGroups.addContent(new Text("\n    "));
        }
        if (!enable && camel != null) {
            camel.getParentElement().removeContent(camel);
        }
    }

    private static void addProperty(Element systemProperties, LinkedHashMap<String, Element> propertiesByName, String name, String value) {
        if (!propertiesByName.containsKey(name)) {
            systemProperties.addContent(new Text("   "));
            systemProperties.addContent(new Element("property", NS_DOMAIN).setAttribute("name", name).setAttribute("value", value));
            systemProperties.addContent(new Text("\n    "));
        }
    }

    private static void rmProperty(LinkedHashMap<String, Element> propertiesByName, String name) {
        Element element = propertiesByName.get(name);
        if (element != null) {
            element.getParentElement().removeContent(element);
        }
    }
}
