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
package org.wildfly.camel.test.cxf;

import java.net.MalformedURLException;
import java.net.URL;

import javax.xml.namespace.QName;
import javax.xml.ws.Service;

import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.impl.DefaultCamelContext;
import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.camel.test.ProvisionerSupport;
import org.wildfly.camel.test.cxf.subA.Endpoint;
import org.wildfly.camel.test.cxf.subA.EndpointImpl;

/**
 * Test WebService endpoint access with the cxf component.
 *
 * @author thomas.diesler@jboss.com
 * @since 11-Jun-2013
 */
@RunWith(Arquillian.class)
public class WebServicesIntegrationTest {

    static final String SIMPLE_WAR = "simple.war";

    @ArquillianResource
    Deployer deployer;

    @ArquillianResource
    ManagementClient managementClient;

    @Deployment
    public static JavaArchive deployment() {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "cxf-integration-tests");
        archive.addClasses(ProvisionerSupport.class, Endpoint.class);
        return archive;
    }

    @Test
    public void testSimpleWar() throws Exception {
        deployer.deploy(SIMPLE_WAR);
        try {
            // [FIXME #283] Usage of camel-cxf depends on TCCL
            Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
            
            QName serviceName = new QName("http://wildfly.camel.test.cxf", "EndpointService");
            Service service = Service.create(getWsdl("/simple"), serviceName);
            Endpoint port = service.getPort(Endpoint.class);
            Assert.assertEquals("Hello Foo", port.echo("Foo"));
        } finally {
            deployer.undeploy(SIMPLE_WAR);
        }
    }

    @Test
    public void testEndpointRoute() throws Exception {
        deployer.deploy(SIMPLE_WAR);
        try {
            // [FIXME #283] Usage of camel-cxf depends on TCCL
            Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
            
            CamelContext camelctx = new DefaultCamelContext();
            camelctx.addRoutes(new RouteBuilder() {
                @Override
                public void configure() throws Exception {
                    from("direct:start").
                    to("cxf://" + getEndpointAddress("/simple") + "?serviceClass=" + Endpoint.class.getName());
                }
            });
            camelctx.start();

            ProducerTemplate producer = camelctx.createProducerTemplate();
            String result = producer.requestBody("direct:start", "Kermit", String.class);
            Assert.assertEquals("Hello Kermit", result);
        } finally {
            deployer.undeploy(SIMPLE_WAR);
        }
    }

    private String getEndpointAddress(String contextPath) throws MalformedURLException {
        return managementClient.getWebUri() + contextPath + "/EndpointService";
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
