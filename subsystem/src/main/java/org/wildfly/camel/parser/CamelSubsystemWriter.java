/*
 * #%L
 * Wildfly Camel Subsystem
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

package org.wildfly.camel.parser;

import java.util.TreeSet;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;

import org.jboss.as.controller.persistence.SubsystemMarshallingContext;
import org.jboss.dmr.ModelNode;
import org.jboss.staxmapper.XMLElementWriter;
import org.jboss.staxmapper.XMLExtendedStreamWriter;
import org.wildfly.camel.parser.Namespace10.Attribute;
import org.wildfly.camel.parser.Namespace10.Element;

/**
 * Write Camel subsystem configuration.
 *
 * @author Thomas.Diesler@jboss.com
 * @since 23-Aug-2013
 */
final class CamelSubsystemWriter implements XMLStreamConstants, XMLElementWriter<SubsystemMarshallingContext> {

    static CamelSubsystemWriter INSTANCE = new CamelSubsystemWriter();

    // hide ctor
    private CamelSubsystemWriter() {
    }

    @Override
    public void writeContent(XMLExtendedStreamWriter writer, SubsystemMarshallingContext context) throws XMLStreamException {
        context.startSubsystemElement(Namespace.CURRENT.getUriString(), false);
        ModelNode node = context.getModelNode();

        if (node.hasDefined(ModelConstants.CONTEXT)) {
            ModelNode properties = node.get(ModelConstants.CONTEXT);
            for (String key : new TreeSet<String>(properties.keys())) {
                String val = properties.get(key).get(ModelConstants.VALUE).asString();
                writer.writeStartElement(Element.CAMEL_CONTEXT.getLocalName());
                writer.writeAttribute(Attribute.ID.getLocalName(), key);
                writer.writeCharacters(val);
                writer.writeEndElement();
            }
        }

        writer.writeEndElement();
    }
}
