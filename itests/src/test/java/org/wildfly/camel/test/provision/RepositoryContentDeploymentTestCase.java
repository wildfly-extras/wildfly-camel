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
package org.wildfly.camel.test.provision;

import java.io.InputStream;
import java.net.URL;
import java.util.Collection;

import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.client.helpers.standalone.ServerDeploymentHelper;
import org.jboss.osgi.metadata.ManifestBuilder;
import org.jboss.osgi.provision.XResourceProvisioner;
import org.jboss.osgi.repository.XPersistentRepository;
import org.jboss.osgi.resolver.XCapability;
import org.jboss.osgi.resolver.XRequirement;
import org.jboss.osgi.resolver.XRequirementBuilder;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.osgi.framework.namespace.IdentityNamespace;
import org.osgi.resource.Capability;
import org.wildfly.camel.CamelConstants;
import org.wildfly.camel.test.ProvisionerSupport;

/**
 * Test repository content deployment.
 *
 * @author thomas.diesler@jboss.com
 * @since 14-Jun-2013
 */
@RunWith(Arquillian.class)
public class RepositoryContentDeploymentTestCase {

    static final String REPOSITORY_CONTENT_JAR = "repository-content.jar";

    @ArquillianResource
    Deployer deployer;

    @ArquillianResource
    ManagementClient managementClient;

    @ArquillianResource
    XResourceProvisioner provsioner;

    @Deployment
    public static JavaArchive createdeployment() {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "repository-content-deployment-tests");
        archive.addClasses(ProvisionerSupport.class);
        archive.addAsResource("repository/felix.configadmin.feature.xml");
        archive.setManifest(new Asset() {
            @Override
            public InputStream openStream() {
                ManifestBuilder builder = ManifestBuilder.newInstance();
                builder.addManifestHeader("Dependencies", "org.jboss.as.controller-client,org.jboss.osgi.provision");
                return builder.openStream();
            }
        });
        return archive;
    }

    @Test
    public void testRepositoryContentXML() throws Exception {

        // Verify that we have no providers for the deployed feature
        XPersistentRepository repository = provsioner.getRepository();
        XRequirement req = XRequirementBuilder.create(IdentityNamespace.IDENTITY_NAMESPACE, "felix.configadmin.feature").getRequirement();
        Assert.assertTrue("No providers", repository.findProviders(req).isEmpty());

        URL resourceUrl = getClass().getResource("/repository/felix.configadmin.feature.xml");
        ServerDeploymentHelper server = new ServerDeploymentHelper(managementClient.getControllerClient());
        String runtimeName = server.deploy("felix.configadmin.feature" + CamelConstants.REPOSITORY_CONTENT_FILE_SUFFIX, resourceUrl.openStream());
        Collection<Capability> caps;
        try {
            caps = repository.findProviders(req);
            Assert.assertEquals("One provider", 1, caps.size());
        } finally {
            server.undeploy(runtimeName);
        }

        // Remove the resource from the repository
        XCapability cap = (XCapability) caps.iterator().next();
        repository.getRepositoryStorage().removeResource(cap.getResource());
        Assert.assertTrue("No providers", repository.findProviders(req).isEmpty());
    }

    @Test
    public void testRepositoryContentJar() throws Exception {

        // Verify that we have no providers for the deployed feature
        XPersistentRepository repository = provsioner.getRepository();
        XRequirement req = XRequirementBuilder.create(IdentityNamespace.IDENTITY_NAMESPACE, "felix.configadmin.feature").getRequirement();
        Assert.assertTrue("No providers", repository.findProviders(req).isEmpty());

        deployer.deploy(REPOSITORY_CONTENT_JAR);
        Collection<Capability> caps;
        try {
            caps = repository.findProviders(req);
            Assert.assertEquals("One provider", 1, caps.size());
        } finally {
            deployer.undeploy(REPOSITORY_CONTENT_JAR);
        }

        // Remove the resource from the repository
        XCapability cap = (XCapability) caps.iterator().next();
        repository.getRepositoryStorage().removeResource(cap.getResource());
        Assert.assertTrue("No providers", repository.findProviders(req).isEmpty());
    }

    @Deployment(name = REPOSITORY_CONTENT_JAR, managed = false, testable = false)
    public static JavaArchive getBundle() {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, REPOSITORY_CONTENT_JAR);
        archive.addAsResource("repository/felix.configadmin.feature.xml", CamelConstants.REPOSITORY_CONTENT_FILE_NAME);
        return archive;
    }
}
