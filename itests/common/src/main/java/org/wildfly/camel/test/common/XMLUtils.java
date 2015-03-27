/*
 * #%L
 * Wildfly Camel :: Testsuite
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

package org.wildfly.camel.test.common;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;

import org.jdom.Document;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

/**
 * Colection of XML utilities
 *
 * @author Thomas.Diesler@jboss.com
 * @since 27-Mar-2015
 */
public final class XMLUtils {

    // hide ctor
    private XMLUtils() {
    }

    public static String compactXML(String xmlinput) throws Exception {
        return compactXML(new ByteArrayInputStream(xmlinput.getBytes()));
    }

    public static String compactXML(InputStream input) throws Exception {

        Document doc = new SAXBuilder().build(input);

        XMLOutputter xo = new XMLOutputter();
        xo.setFormat(Format.getCompactFormat().setOmitDeclaration(true));

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        xo.output(doc, baos);

        return new String(baos.toByteArray()).trim();
    }
}
