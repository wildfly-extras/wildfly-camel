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
package org.wildfly.camel.test.cxf.ws;

import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.ws.Service;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.cxf.binding.soap.SoapHeader;
import org.apache.cxf.headers.Header;
import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.wildfly.camel.test.cxf.ws.subA.Endpoint;
import org.wildfly.camel.test.cxf.ws.subA.EndpointImpl;
import org.xml.sax.InputSource;

/**
 * Test WebService endpoint access with the cxf component.
 *
 * @author thomas.diesler@jboss.com
 * @since 11-Jun-2013
 */
@RunWith(Arquillian.class)
public class WebServiceProducerIntegrationTest {

    static final String SIMPLE_WAR = "simple.war";

    @ArquillianResource
    Deployer deployer;

    @Deployment
    public static JavaArchive deployment() {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "cxf-ws-producer-tests");
        archive.addClasses(Endpoint.class);
        return archive;
    }

    @Test
    public void testSimpleWar() throws Exception {
        deployer.deploy(SIMPLE_WAR);
        try {
            QName serviceName = new QName("http://wildfly.camel.test.cxf", "EndpointService");
            Service service = Service.create(getWsdl("/simple"), serviceName);
            Endpoint port = service.getPort(Endpoint.class);
            Assert.assertEquals("Hello Foo", port.echo("Foo"));
        } finally {
            deployer.undeploy(SIMPLE_WAR);
        }
    }

    @Test
    public void testCxfProducer() throws Exception {
        deployer.deploy(SIMPLE_WAR);
        try {
            CamelContext camelctx = new DefaultCamelContext();
            camelctx.addRoutes(new RouteBuilder() {
                @Override
                public void configure() throws Exception {
                    from("direct:start").to("cxf://" + getEndpointAddress("/simple") + "?serviceClass=" + Endpoint.class.getName());
                }
            });

            camelctx.start();
            try {
                ProducerTemplate producer = camelctx.createProducerTemplate();
                String result = producer.requestBody("direct:start", "Kermit", String.class);
                Assert.assertEquals("Hello Kermit", result);
            } finally {
                camelctx.stop();
            }
        } finally {
            deployer.undeploy(SIMPLE_WAR);
        }
    }

    @Test
    public void testCxfSoapHeader() throws Exception {
        deployer.deploy(SIMPLE_WAR);
        try {
            CamelContext camelctx = new DefaultCamelContext();
            camelctx.addRoutes(new RouteBuilder() {
                @Override
                public void configure() throws Exception {
                    from("direct:start")
                    .process(new Processor() {
                        @Override
                        public void process(Exchange exchange) throws Exception {
                            List<SoapHeader> soapHeaders = new ArrayList<SoapHeader>();

                            String headerXml = "<?xml version=\"1.0\" encoding=\"utf-8\"?><input xmlns=\"http://wildfly.camel.test.cxf\">Kermit</input>";

                            SoapHeader soapHeader = new SoapHeader(new QName("http://wildfly.camel.test.cxf", "input"), getSoapHeaderElement(headerXml));
                            soapHeader.setDirection(Header.Direction.DIRECTION_IN);
                            soapHeaders.add(soapHeader);

                            exchange.getOut().setHeader(Header.HEADER_LIST, soapHeaders);
                        }
                    })
                    .to("cxf://" + getEndpointAddress("/simple") + "?serviceClass=" + Endpoint.class.getName());
                }
            });

            camelctx.start();
            try {
                ProducerTemplate producer = camelctx.createProducerTemplate();
                String result = producer.requestBody("direct:start", null, String.class);
                Assert.assertEquals("Hello Kermit", result);
            } finally {
                camelctx.stop();
            }
        } finally {
            deployer.undeploy(SIMPLE_WAR);
        }
    }

    private Element getSoapHeaderElement(String headerXml) throws Exception {
        DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        StringReader stringReader = new StringReader(headerXml);
        InputSource inputSource = new InputSource(stringReader);
        Document parsedHeader = documentBuilderFactory.newDocumentBuilder().parse(inputSource);
        return parsedHeader.getDocumentElement();
    }

    private String getEndpointAddress(String contextPath) throws MalformedURLException {
        return "http://localhost:8080" + contextPath + "/EndpointService";
    }

    private URL getWsdl(String contextPath) throws MalformedURLException {
        return new URL(getEndpointAddress(contextPath) + "?wsdl");
    }

    @Deployment(name = SIMPLE_WAR, managed = false, testable = false)
    public static Archive<?> getSimpleWar() {
        final WebArchive archive = ShrinkWrap.create(WebArchive.class, SIMPLE_WAR);
        archive.addClasses(Endpoint.class, EndpointImpl.class);
        return archive;
    }
}
