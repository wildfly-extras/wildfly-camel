package org.wildfly.camel.test.config;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Iterator;

import org.jdom.Attribute;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.Namespace;
import org.jdom.filter.ElementFilter;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.junit.Assert;
import org.wildfly.extras.config.ConfigSupport;

public class ConfigTestSupport {

    public void assertElementNull(Element parent, String name, Namespace... supportedNamespaces) {
        Element element = findChildElement(parent, name, supportedNamespaces);
        Assert.assertNull("Element '" + name + "'" , element);
    }

    public void assertElementNotNull(Element parent, String name, Namespace... supportedNamespaces) {
        Element element = findChildElement(parent, name, supportedNamespaces);
        Assert.assertNotNull("Element '" + name + "'" , element);
    }

    public void assertElementWithAttributeValueNull(Element parent, String name, String attrName, String attrValue, Namespace... supportedNamespaces) {
        Element element = findElementWithAttribute(parent, name, attrName, attrValue, supportedNamespaces);
        Assert.assertNull("Element '" + name + "'" , element);
    }

    public void assertElementWithAttributeValueNotNull(Element parent, String name, String attrName, String attrValue, Namespace... supportedNamespaces) {
        Element element = findElementWithAttribute(parent, name, attrName, attrValue, supportedNamespaces);
        Assert.assertNotNull("Element '" + name + "'" , element);
    }

    private Element findElementWithAttribute(Element parent, String name, String attrName, String attrValue, Namespace... supportedNamespaces) {
        return ConfigSupport.findElementWithAttributeValue(parent, name, attrName, attrValue, supportedNamespaces);
    }

    private Element findChildElement(Element parent, String name, Namespace... supportedNamespaces) {
        return ConfigSupport.findChildElement(parent, name, supportedNamespaces);
    }

    @SuppressWarnings("unchecked")
    public int getElementCount(Document document, String elementName, Namespace namespace, Attribute attribute) {
        int elementCount = 0;
        ElementFilter filter;

        if (namespace == null) {
            filter = new ElementFilter(elementName);
        } else {
            filter = new ElementFilter(elementName, namespace);
        }

        Iterator<Element> elements = document.getDescendants(filter);
        while (elements.hasNext()) {
            Element element = elements.next();
            if (attribute != null) {
                Attribute matchedAttribute = element.getAttribute(attribute.getName());
                if (matchedAttribute != null && matchedAttribute.getValue().equals(attribute.getValue())) {
                    elementCount += 1;
                }
            } else {
                elementCount += 1;
            }
        }

        return elementCount;
    }

    public void outputDocumentContent(Document document, OutputStream stream) throws IOException {
        XMLOutputter output = new XMLOutputter();
        output.setFormat(Format.getRawFormat());
        output.output(document, stream);
    }
}