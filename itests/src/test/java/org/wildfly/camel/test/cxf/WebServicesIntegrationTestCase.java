/*
 * JBoss, Home of Professional Open Source
 * Copyright 2009, Red Hat Middleware LLC, and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.wildfly.camel.test.cxf;

import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import javax.xml.namespace.QName;
import javax.xml.ws.Service;

import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.osgi.metadata.ManifestBuilder;
import org.jboss.osgi.provision.XResourceProvisioner;
import org.jboss.osgi.resolver.XEnvironment;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.osgi.framework.namespace.IdentityNamespace;
import org.wildfly.camel.CamelContextFactory;
import org.wildfly.camel.CamelContextRegistry;
import org.wildfly.camel.test.ProvisionerSupport;
import org.wildfly.camel.test.cxf.subA.Endpoint;
import org.wildfly.camel.test.cxf.subA.EndpointImpl;

/**
 * TODO
 *
 * @author thomas.diesler@jboss.com
 * @since 11-Jun-2013
 */
@RunWith(Arquillian.class)
public class WebServicesIntegrationTestCase {

    static final String SIMPLE_WAR = "simple.war";

    @ArquillianResource
    Deployer deployer;

    @ArquillianResource
    CamelContextFactory contextFactory;

    @ArquillianResource
    CamelContextRegistry contextRegistry;

    @ArquillianResource
    XResourceProvisioner provisioner;

    @ArquillianResource
    XEnvironment environment;

    @ArquillianResource
    ManagementClient managementClient;

    static List<String> runtimeNames;

    @Deployment
    public static JavaArchive deployment() {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "cxf-integration-tests");
        archive.addClasses(Endpoint.class, ProvisionerSupport.class);
        archive.addAsResource("repository/camel.cxf.feature.xml");
        archive.setManifest(new Asset() {
            @Override
            public InputStream openStream() {
                ManifestBuilder builder = ManifestBuilder.newInstance();
                builder.addManifestHeader("Dependencies", "org.apache.camel,org.wildfly.camel,org.jboss.as.controller-client,org.jboss.osgi.provision");
                return builder.openStream();
            }
        });
        return archive;
    }

    @Test
    @InSequence(Integer.MIN_VALUE)
    public void installCamelFeatures() throws Exception {
        ModelControllerClient controllerClient = managementClient.getControllerClient();
        ProvisionerSupport provisionerSupport = new ProvisionerSupport(provisioner, controllerClient);
        runtimeNames = provisionerSupport.installCapability(environment, IdentityNamespace.IDENTITY_NAMESPACE, "camel.cxf.feature");
    }

    @Test
    @InSequence(Integer.MAX_VALUE)
    public void uninstallCamelFeatures() throws Exception {
        ModelControllerClient controllerClient = managementClient.getControllerClient();
        ProvisionerSupport provisionerSupport = new ProvisionerSupport(provisioner, controllerClient);
        provisionerSupport.uninstallCapabilities(runtimeNames);
    }

    @Test
    public void testSimpleWar() throws Exception {
        deployer.deploy(SIMPLE_WAR);
        try {
            QName serviceName = new QName("http://wildfly.camel.test.cxf", "EndpointService");
            Service service = Service.create(getWsdl("/simple"), serviceName);
            Endpoint port = service.getPort(Endpoint.class);
            Assert.assertEquals("Foo", port.echo("Foo"));
        } finally {
            deployer.undeploy(SIMPLE_WAR);
        }
    }

    private URL getWsdl(String contextPath) throws MalformedURLException {
        return new URL(managementClient.getWebUri() + contextPath + "/EndpointService?wsdl");
    }

    @Deployment(name = SIMPLE_WAR, managed = false, testable = false)
    public static Archive<?> getSimpleWar() {
        final WebArchive archive = ShrinkWrap.create(WebArchive.class, SIMPLE_WAR);
        archive.addClasses(Endpoint.class, EndpointImpl.class);
        return archive;
    }
}
