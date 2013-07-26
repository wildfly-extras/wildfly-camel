/*
 * #%L
 * Wildfly Camel Testsuite
 * %%
 * Copyright (C) 2013 JBoss by Red Hat
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as 
 * published by the Free Software Foundation, either version 2.1 of the 
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public 
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-2.1.html>.
 * #L%
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
import org.jboss.gravia.provision.Provisioner;
import org.jboss.gravia.repository.Repository;
import org.jboss.gravia.repository.RepositoryStorage;
import org.jboss.gravia.resource.Capability;
import org.jboss.gravia.resource.DefaultRequirementBuilder;
import org.jboss.gravia.resource.IdentityNamespace;
import org.jboss.gravia.resource.Requirement;
import org.jboss.gravia.resource.ManifestBuilder;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
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
    Provisioner provsioner;

    @Deployment
    public static JavaArchive createdeployment() {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "repository-content-deployment-tests");
        archive.addClasses(ProvisionerSupport.class);
        archive.addAsResource("repository/acme.foo.feature.xml");
        archive.setManifest(new Asset() {
            @Override
            public InputStream openStream() {
                ManifestBuilder builder = new ManifestBuilder();
                builder.addManifestHeader("Dependencies", "org.jboss.as.controller-client,org.jboss.gravia");
                return builder.openStream();
            }
        });
        return archive;
    }

    @Test
    public void testRepositoryContentML() throws Exception {

        // Verify that we have no providers for the deployed feature
        Repository repository = provsioner.getRepository();
        Requirement req = new DefaultRequirementBuilder(IdentityNamespace.IDENTITY_NAMESPACE, "acme.foo.feature").getRequirement();
        Assert.assertTrue("No providers", repository.findProviders(req).isEmpty());

        URL resourceUrl = getClass().getResource("/repository/acme.foo.feature.xml");
        ServerDeploymentHelper server = new ServerDeploymentHelper(managementClient.getControllerClient());
        String runtimeName = server.deploy("acme.foo.feature" + CamelConstants.REPOSITORY_CONTENT_FILE_SUFFIX, resourceUrl.openStream());
        Collection<Capability> caps;
        try {
            caps = repository.findProviders(req);
            Assert.assertEquals("One provider", 1, caps.size());
        } finally {
            server.undeploy(runtimeName);
        }

        // Remove the resource from the repository
        Capability cap = caps.iterator().next();
        repository.adapt(RepositoryStorage.class).removeResource(cap.getResource().getIdentity());
        Assert.assertTrue("No providers", repository.findProviders(req).isEmpty());
    }

    @Test
    public void testRepositoryContentJar() throws Exception {

        // Verify that we have no providers for the deployed feature
        Repository repository = provsioner.getRepository();
        Requirement req = new DefaultRequirementBuilder(IdentityNamespace.IDENTITY_NAMESPACE, "acme.foo.feature").getRequirement();
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
        Capability cap = caps.iterator().next();
        repository.adapt(RepositoryStorage.class).removeResource(cap.getResource().getIdentity());
        Assert.assertTrue("No providers", repository.findProviders(req).isEmpty());
    }

    @Deployment(name = REPOSITORY_CONTENT_JAR, managed = false, testable = false)
    public static JavaArchive getBundle() {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, REPOSITORY_CONTENT_JAR);
        archive.addAsResource("repository/acme.foo.feature.xml", CamelConstants.REPOSITORY_CONTENT_FILE_NAME);
        return archive;
    }
}
