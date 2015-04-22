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

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

/**
 * Executes the xslt fuse integration transformations on the EAP configuration
 * files
 *
 * @author David Virgil Naranjo 2015 Red Hat Inc.
 */
public class XSLTransformer {

    public static final String STANDALONE_XSLT_ENABLE_TEMPLATE = "/xsl/standalone_enable.xsl";
    public static final String DOMAIN_XSLT_ENABLE_TEMPLATE = "/xsl/domain_enable.xsl";

    public static final String STANDALONE_XSLT_DISABLE_TEMPLATE = "/xsl/standalone_disable.xsl";
    public static final String DOMAIN_XSLT_DISABLE_TEMPLATE = "/xsl/domain_disable.xsl";

    public static final List<String> standalonePaths = new ArrayList<String>();
    public static final List<String> domainPaths = new ArrayList<String>();

    static {
        standalonePaths.add("/standalone/configuration/standalone.xml");
        standalonePaths.add("/standalone/configuration/standalone-full.xml");
        standalonePaths.add("/standalone/configuration/standalone-full-ha.xml");
        standalonePaths.add("/standalone/configuration/standalone-ha.xml");
        domainPaths.add("/domain/configuration/domain.xml");
    }

    /**
     * Apply xslt.
     *
     * @param enable
     *            the enable
     * @param jbossHome
     *            the jboss home
     * @throws Exception
     *             the exception
     */
    public void applyXSLT(boolean enable, String jbossHome) throws Exception {

        if (enable) {
            System.out.println("\tApplying XSLT Enable templates to " + jbossHome);
            for (String path : standalonePaths) {
                applyTemplate(jbossHome + path, STANDALONE_XSLT_ENABLE_TEMPLATE);
            }
            for (String path : domainPaths) {
                applyTemplate(jbossHome + path, DOMAIN_XSLT_ENABLE_TEMPLATE);
            }
        } else {
            System.out.println("\t Applying XSLT Disable templates to " + jbossHome);
            for (String path : standalonePaths) {
                applyTemplate(jbossHome + path, STANDALONE_XSLT_DISABLE_TEMPLATE);
            }
            for (String path : domainPaths) {
                applyTemplate(jbossHome + path, DOMAIN_XSLT_DISABLE_TEMPLATE);
            }
        }
    }

    /**
     * Apply template.
     *
     * @param inputPath
     *            the input path
     * @param template
     *            the template
     * @throws Exception
     *             the exception
     */
    private void applyTemplate(String inputPath, String template) throws Exception {
        System.out.println("\tApplying XSLT Tranformation to: " + inputPath);

        FileReader fr = null;
        FileWriter fw = null;
        File result = null;
        try {
            File source = new File(inputPath);
            result = File.createTempFile("fuse", "xlt");
            TransformerFactory factory = TransformerFactory.newInstance();
            InputStream is_template = XSLTransformer.class.getResourceAsStream(template);
            if (is_template == null) {
                System.out.println("Is=" + is_template);
            }
            Source templateSource = new StreamSource(is_template);
            Transformer t = factory.newTransformer(templateSource);
            Source s = new StreamSource(source);
            Result r = new StreamResult(result);
            t.transform(s, r);
            fr = new FileReader(result);
            fw = new FileWriter(source);
            int c = fr.read();
            while (c != -1) {
                fw.write(c);
                c = fr.read(); // Add this line
            }

        } catch (TransformerConfigurationException e) {
            throw new Exception("Unable to apply transformation: " + template + " to input: " + inputPath, e);
        } catch (TransformerException e) {
            throw new Exception("Unable to apply transformation: " + template + " to input: " + inputPath, e);
        } catch (IOException e) {
            throw new Exception("Unable to apply transformation: " + template + " to input: " + inputPath, e);
        } finally {
            if (fr != null)
                fr.close();
            if (fw != null)
                fw.close();
            if (result != null) {
                result.deleteOnExit();
            }
        }

    }
}
