/*
 * #%L
 * Wildfly Camel :: Testsuite
 * %%
 * Copyright (C) 2013 - 2017 RedHat
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
package org.wildfly.camel.test.fop;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URI;
import java.nio.file.Paths;

import javax.naming.InitialContext;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.fop.apps.FopFactory;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.util.PDFTextStripper;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.gravia.resource.ManifestBuilder;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.extension.camel.CamelAware;

@CamelAware
@RunWith(Arquillian.class)
public class FopIntegrationTest {

    @ArquillianResource
    private InitialContext initialContext;

    @Deployment
    public static JavaArchive createDeployment() {
        return ShrinkWrap.create(JavaArchive.class, "camel-fop-tests.jar")
            .addAsResource("fop/data.xml", "data.xml")
            .addAsResource("fop/template.xsl", "template.xsl")
            .addAsResource("fop/factory.xml", "factory.xml")
            .setManifest(() -> {
                ManifestBuilder builder = new ManifestBuilder();
                builder.addManifestHeader("Dependencies", "org.apache.pdfbox");
                return builder.openStream();
            });
    }

    @Test
    public void testFopComponent() throws Exception {
        CamelContext camelctx = new DefaultCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:start")
                .to("xslt:template.xsl")
                .setHeader("foo", constant("bar"))
                .to("fop:pdf")
                .setHeader(Exchange.FILE_NAME, constant("resultA.pdf"))
                .to("file:{{jboss.server.data.dir}}/fop")
                .to("mock:result");
            }
        });

        MockEndpoint mockEndpoint = camelctx.getEndpoint("mock:result", MockEndpoint.class);
        mockEndpoint.expectedMessageCount(1);

        camelctx.start();
        try {
            ProducerTemplate template = camelctx.createProducerTemplate();
            template.sendBody("direct:start", FopIntegrationTest.class.getResourceAsStream("/data.xml"));

            mockEndpoint.assertIsSatisfied();

            String dataDir = System.getProperty("jboss.server.data.dir");
            PDDocument document = PDDocument.load(Paths.get(dataDir, "fop", "resultA.pdf").toFile());
            String pdfText = extractTextFromDocument(document);
            Assert.assertTrue(pdfText.contains("Project"));
            Assert.assertTrue(pdfText.contains("John Doe"));
        } finally {
            camelctx.stop();
        }
    }

    @Test
    public void testFopComponentWithCustomFactory() throws Exception {
        FopFactory fopFactory = FopFactory.newInstance(new URI("/"), FopIntegrationTest.class.getResourceAsStream("/factory.xml"));
        initialContext.bind("fopFactory", fopFactory);

        CamelContext camelctx = new DefaultCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:start")
                .to("xslt:template.xsl")
                .setHeader("foo", constant("bar"))
                .to("fop:pdf?fopFactory=#fopFactory")
                .setHeader(Exchange.FILE_NAME, constant("resultB.pdf"))
                .to("file:{{jboss.server.data.dir}}/fop")
                .to("mock:result");
            }
        });

        MockEndpoint mockEndpoint = camelctx.getEndpoint("mock:result", MockEndpoint.class);
        mockEndpoint.expectedMessageCount(1);

        camelctx.start();
        try {
            ProducerTemplate template = camelctx.createProducerTemplate();
            template.sendBody("direct:start", FopIntegrationTest.class.getResourceAsStream("/data.xml"));

            mockEndpoint.assertIsSatisfied();

            String dataDir = System.getProperty("jboss.server.data.dir");
            PDDocument document = PDDocument.load(Paths.get(dataDir, "fop", "resultB.pdf").toFile());
            String pdfText = extractTextFromDocument(document);
            Assert.assertTrue(pdfText.contains("Project"));
            Assert.assertTrue(pdfText.contains("John Doe"));
        } finally {
            camelctx.stop();
            initialContext.unbind("fopFactory");
        }
    }

    private String extractTextFromDocument(PDDocument document) throws IOException {
        Writer output = new StringWriter();
        PDFTextStripper stripper = new PDFTextStripper();
        stripper.writeText(document, output);
        return output.toString().trim();
    }
}
