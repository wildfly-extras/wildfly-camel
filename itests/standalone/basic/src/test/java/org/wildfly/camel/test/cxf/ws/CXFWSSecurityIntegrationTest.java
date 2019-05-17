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

import java.net.MalformedURLException;
import java.net.URL;

import javax.xml.namespace.QName;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.Service;
import javax.xml.ws.soap.SOAPFaultException;

import org.apache.cxf.rt.security.SecurityConstants;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.camel.test.common.types.Endpoint;
import org.wildfly.camel.test.cxf.ws.subA.POJOEndpointAuthorizationInterceptor;
import org.wildfly.camel.test.cxf.ws.subA.UsernameTokenEndpointImpl;

/**
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 */
@RunAsClient
@RunWith(Arquillian.class)
@ServerSetup({ CXFWSSecureProducerIntegrationTest.SecurityDomainSetup.class })
public class CXFWSSecurityIntegrationTest {

    public static final String APP_NAME = "CXFWSPolicyIntegrationTest";

    @Deployment
    public static Archive<?> deployment() {
        final WebArchive archive = ShrinkWrap.create(WebArchive.class, APP_NAME + ".war")
                .addClasses(Endpoint.class, UsernameTokenEndpointImpl.class, POJOEndpointAuthorizationInterceptor.class)
                .addAsWebInfResource("cxf/ws-security/EndpointService-with-username-token-policy.wsdl",
                        "EndpointService.wsdl")
                .addAsManifestResource(new StringAsset("Dependencies: org.apache.cxf\n"), "MANIFEST.MF")
                .addAsWebInfResource("cxf/ws-security/jaxws-endpoint-config.xml", "jaxws-endpoint-config.xml")
                .addAsWebInfResource(
                        new StringAsset(
                                "<jboss-web><security-domain>cxf-security-domain</security-domain></jboss-web>"),
                        "jboss-web.xml")
                .addAsResource("cxf/secure/cxf-roles.properties", "cxf-roles.properties")
                .addAsResource("cxf/secure/cxf-users.properties", "cxf-users.properties");
        return archive;
    }

    @Test
    public void goodUser() throws Exception {
        QName serviceName = new QName("http://wildfly.camel.test.cxf", "EndpointService");
        Service service = Service.create(getWsdl("/"+ APP_NAME), serviceName);
        Endpoint port = service.getPort(Endpoint.class);
        BindingProvider prov = (BindingProvider)port;
        prov.getRequestContext().put(SecurityConstants.USERNAME, "cxfuser");
        prov.getRequestContext().put(SecurityConstants.PASSWORD, "cxfpassword");
        Assert.assertEquals("Hello Foo", port.echo("Foo"));
    }

    @Test
    public void goodUserBadPassword() throws Exception {
        QName serviceName = new QName("http://wildfly.camel.test.cxf", "EndpointService");
        Service service = Service.create(getWsdl("/"+ APP_NAME), serviceName);
        Endpoint port = service.getPort(Endpoint.class);
        BindingProvider prov = (BindingProvider)port;
        prov.getRequestContext().put(SecurityConstants.USERNAME, "cxfuser");
        prov.getRequestContext().put(SecurityConstants.PASSWORD, "bad");
        try {
            port.echo("Foo");
            Assert.fail("Expected " + SOAPFaultException.class.getName());
        } catch (SOAPFaultException e) {
            Assert.assertEquals("JBWS024057: Failed Authentication : Subject has not been created", e.getFault().getFaultString());
        }
    }

    @Test
    public void anonymous() throws Exception {
        QName serviceName = new QName("http://wildfly.camel.test.cxf", "EndpointService");
        Service service = Service.create(getWsdl("/" + APP_NAME), serviceName);
        Endpoint port = service.getPort(Endpoint.class);
        try {
            port.echo("Foo");
            Assert.fail("Expected " + SOAPFaultException.class.getName());
        } catch (SOAPFaultException e) {
            Assert.assertEquals("No username available", e.getFault().getFaultString());
        }
    }

    private String getEndpointAddress(String contextPath) throws MalformedURLException {
        return "http://localhost:8080" + contextPath + "/EndpointService";
    }

    private URL getWsdl(String contextPath) throws MalformedURLException {
        return new URL(getEndpointAddress(contextPath) + "?wsdl");
    }

}
