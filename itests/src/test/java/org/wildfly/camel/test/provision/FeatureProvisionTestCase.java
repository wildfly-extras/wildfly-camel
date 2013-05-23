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
import java.util.List;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.modules.ModuleClassLoader;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.modules.ModuleLoader;
import org.jboss.osgi.metadata.ManifestBuilder;
import org.jboss.osgi.provision.XResourceProvisioner;
import org.jboss.osgi.resolver.XEnvironment;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.camel.test.ProvisionerSupport;

/**
 * Test feature provisioning.
 *
 * @author thomas.diesler@jboss.com
 * @since 18-May-2013
 */
@RunWith(Arquillian.class)
public class FeatureProvisionTestCase {

    @ArquillianResource
    XResourceProvisioner provisioner;

    @ArquillianResource
    XEnvironment environment;

    @ArquillianResource
    ManagementClient managementClient;

    @Deployment
    public static JavaArchive createdeployment() {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "resource-provisioner-tests");
        archive.addClasses(ProvisionerSupport.class);
        archive.addAsResource("repository/camel.jms.feature.xml");
        archive.addAsResource("repository/org.apache.camel.component.jms/jboss-deployment-structure.xml");
        archive.setManifest(new Asset() {
            @Override
            public InputStream openStream() {
                ManifestBuilder builder = ManifestBuilder.newInstance();
                builder.addManifestHeader("Dependencies", "org.jboss.as.controller-client,org.jboss.osgi.provision,org.jboss.shrinkwrap.core");
                return builder.openStream();
            }
        });
        return archive;
    }

    @Test
    public void testFeatureProvisioning() throws Exception {
        ModelControllerClient controllerClient = managementClient.getControllerClient();
        ProvisionerSupport provisionerSupport = new ProvisionerSupport(provisioner, controllerClient);
        List<String> rtnames = provisionerSupport.installCapabilities(environment, "camel.jms.feature");
        try {
            ModuleLoader moduleLoader = ((ModuleClassLoader)getClass().getClassLoader()).getModule().getModuleLoader();
            moduleLoader.loadModule(ModuleIdentifier.create("org.apache.camel.component.jms"));
        } finally {
            provisionerSupport.uninstallCapabilities(rtnames);
        }
    }
}
