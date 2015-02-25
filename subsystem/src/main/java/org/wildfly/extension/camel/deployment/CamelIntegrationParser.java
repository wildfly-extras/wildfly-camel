/*
 * #%L
 * Wildfly Camel :: Subsystem
 * %%
 * Copyright (C) 2013 - 2014 RedHat
 * %%
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
 * #L%
 */


package org.wildfly.extension.camel.deployment;

import org.jboss.as.ee.structure.JBossDescriptorPropertyReplacement;
import org.jboss.as.server.deployment.AttachmentKey;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.jbossallxml.JBossAllXMLParser;
import org.jboss.metadata.property.PropertyReplacer;
import org.jboss.staxmapper.XMLExtendedStreamReader;

import javax.xml.namespace.QName;
import javax.xml.stream.Location;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import java.util.HashMap;
import java.util.Map;

/**
 * A JBossAllXMLParser which parses CamelDeploymentSettings
 *
 * @author Thomas.Diesler@jboss.com
 * @since 14-Jun-2013
 */
public class CamelIntegrationParser implements JBossAllXMLParser<CamelDeploymentSettings> {

    public static final AttachmentKey<CamelDeploymentSettings> ATTACHMENT_KEY = AttachmentKey.create(CamelDeploymentSettings.class);
    public static final String NAMESPACE_1_0 = "urn:jboss:jboss-camel:1.0";
    public static final QName ROOT_ELEMENT = new QName(NAMESPACE_1_0, "jboss-camel");

    private static final String APACHE_CAMEL_COMPONENT_MODULE = "org.apache.camel.component";
    private static final String CAMEL_COMPONENT_PREFIX = "camel-";

    @Override
    public CamelDeploymentSettings parse(final XMLExtendedStreamReader reader, final DeploymentUnit deploymentUnit) throws XMLStreamException {
        return parser(reader, JBossDescriptorPropertyReplacement.propertyReplacer(deploymentUnit));
    }

    static public CamelDeploymentSettings parser(XMLExtendedStreamReader reader, PropertyReplacer replacer) throws XMLStreamException {
        CamelDeploymentSettings result = new CamelDeploymentSettings();
        parseCamelIntegrationElement(reader, result, replacer);
        return result;
    }

    enum Element {

        CAMEL_INTEGRATION(ROOT_ELEMENT),
        COMPONENT(new QName(NAMESPACE_1_0, "component")),
        COMPONENT_MODULE(new QName(NAMESPACE_1_0, "component-module")),
        // default unknown element
        UNKNOWN(null);

        private static final Map<QName, Element> elements;

        static {
            Map<QName, Element> elementsMap = new HashMap<QName, Element>();
            for (Element element : Element.values()) {
                if( element!=UNKNOWN ) {
                    elementsMap.put(element.getQName(), element);
                }
            }
            elements = elementsMap;
        }

        private final QName qname;

        Element(QName qname) {
            this.qname = qname;
        }

        public QName getQName() {
            return qname;
        }

        static Element of(QName qName) {
            QName name;
            if (qName.getNamespaceURI().equals("")) {
                name = new QName(NAMESPACE_1_0, qName.getLocalPart());
            } else {
                name = qName;
            }
            final Element element = elements.get(name);
            return element == null ? UNKNOWN : element;
        }
    }


    enum Version {
        JBOSS_JPA_1_0,
        UNKNOWN
    }

    enum Attribute {
        NAME(new QName("name")),
        ENABLED(new QName("enabled")),
        // default unknown attribute
        UNKNOWN(null);

        private static final Map<QName, Attribute> attributes;

        static {
            Map<QName, Attribute> attributesMap = new HashMap<QName, Attribute>();
            for (Attribute element : Attribute.values()) {
                if( element!=UNKNOWN ) {
                    attributesMap.put(element.getQName(), element);
                }
            }
            attributes = attributesMap;
        }

        private final QName qname;

        Attribute(QName qname) {
            this.qname = qname;
        }

        public QName getQName() {
            return qname;
        }

        static Attribute of(QName qName) {
            final Attribute attribute = attributes.get(qName);
            return attribute == null ? UNKNOWN : attribute;
        }
    }

    private static void parseCamelIntegrationElement(XMLExtendedStreamReader reader, CamelDeploymentSettings result, PropertyReplacer propertyReplacer) throws XMLStreamException {
        // We should get called when we are at the root element.
        final Element rootElement = Element.of(reader.getName());
        switch (rootElement) {
            case CAMEL_INTEGRATION:
                final String value = getAttributeValue(reader, Attribute.ENABLED, propertyReplacer);
                if (value == null || value.isEmpty()) {
                    result.setEnabled(true);
                } else {
                    result.setEnabled(Boolean.valueOf(value) == Boolean.TRUE);
                }
                break;
            default:
                throw unexpectedContent(reader);
        }


        while (reader.hasNext()) {
            switch (reader.nextTag()) {
                case XMLStreamConstants.END_ELEMENT: {
                    return;
                }
                case XMLStreamConstants.START_ELEMENT: {
                    final Element element = Element.of(reader.getName());
                    switch (element) {
                        case COMPONENT:
                            parseComponentElement(reader, result, propertyReplacer);
                            break;
                        case COMPONENT_MODULE:
                            parseComponentModuleElement(reader, result, propertyReplacer);
                            break;
                        default:
                            throw unexpectedContent(reader);
                    }
                    break;
                }
                default: {
                    throw unexpectedContent(reader);
                }
            }
        }

        throw endOfDocument(reader.getLocation());
    }

    private static void parseComponentElement(XMLExtendedStreamReader reader, CamelDeploymentSettings result, PropertyReplacer propertyReplacer) throws XMLStreamException {
        String value = getAttributeValue(reader, Attribute.NAME, propertyReplacer);
        if (value != null && !value.isEmpty()) {
            if( value.startsWith(CAMEL_COMPONENT_PREFIX) ) {
                value = value.substring(CAMEL_COMPONENT_PREFIX.length());
            }
            value = APACHE_CAMEL_COMPONENT_MODULE + "." + value;
            result.addModule(value);
        }
        switch (reader.nextTag()) {
            case XMLStreamConstants.END_ELEMENT: {
                return;
            }
            default: {
                throw unexpectedContent(reader);
            }
        }
    }

    private static void parseComponentModuleElement(XMLExtendedStreamReader reader, CamelDeploymentSettings result, PropertyReplacer propertyReplacer) throws XMLStreamException {
        final String value = getAttributeValue(reader, Attribute.NAME, propertyReplacer);
        if (value != null && !value.isEmpty()) {
            result.addModule(value);
        }
        switch (reader.nextTag()) {
            case XMLStreamConstants.END_ELEMENT: {
                return;
            }
            default: {
                throw unexpectedContent(reader);
            }
        }
    }

    private static XMLStreamException unexpectedContent(final XMLStreamReader reader) {
        final String kind;
        switch (reader.getEventType()) {
            case XMLStreamConstants.ATTRIBUTE:
                kind = "attribute";
                break;
            case XMLStreamConstants.CDATA:
                kind = "cdata";
                break;
            case XMLStreamConstants.CHARACTERS:
                kind = "characters";
                break;
            case XMLStreamConstants.COMMENT:
                kind = "comment";
                break;
            case XMLStreamConstants.DTD:
                kind = "dtd";
                break;
            case XMLStreamConstants.END_DOCUMENT:
                kind = "document end";
                break;
            case XMLStreamConstants.END_ELEMENT:
                kind = "element end";
                break;
            case XMLStreamConstants.ENTITY_DECLARATION:
                kind = "entity declaration";
                break;
            case XMLStreamConstants.ENTITY_REFERENCE:
                kind = "entity ref";
                break;
            case XMLStreamConstants.NAMESPACE:
                kind = "namespace";
                break;
            case XMLStreamConstants.NOTATION_DECLARATION:
                kind = "notation declaration";
                break;
            case XMLStreamConstants.PROCESSING_INSTRUCTION:
                kind = "processing instruction";
                break;
            case XMLStreamConstants.SPACE:
                kind = "whitespace";
                break;
            case XMLStreamConstants.START_DOCUMENT:
                kind = "document start";
                break;
            case XMLStreamConstants.START_ELEMENT:
                kind = "element start";
                break;
            default:
                kind = "unknown";
                break;
        }

        return new XMLStreamException("Unexpected content kind: "+kind+" at "+reader.getLocation());
//        return ServerLogger.ROOT_LOGGER.unexpectedContent(kind, (reader.hasName() ? reader.getName() : null),
//                (reader.hasText() ? reader.getText() : null), reader.getLocation());
    }

    private static String getAttributeValue(final XMLStreamReader reader, Attribute attribute, final PropertyReplacer propertyReplacer) throws XMLStreamException {
        // return propertyReplacer.replaceProperties(reader.getAttributeValue(null, attribute.getQName().getLocalPart()));
        return reader.getAttributeValue(null, attribute.getQName().getLocalPart());
    }

    private static XMLStreamException endOfDocument(final Location location) {
        return new XMLStreamException("Expected end of document "+location);
//        return ServerLogger.ROOT_LOGGER.unexpectedEndOfDocument(location);
    }


}