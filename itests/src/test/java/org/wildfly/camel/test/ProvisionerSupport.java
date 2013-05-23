/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.wildfly.camel.test;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.client.helpers.standalone.ServerDeploymentHelper;
import org.jboss.as.controller.client.helpers.standalone.ServerDeploymentHelper.ServerDeploymentException;
import org.jboss.osgi.provision.ProvisionResult;
import org.jboss.osgi.provision.XResourceProvisioner;
import org.jboss.osgi.repository.RepositoryReader;
import org.jboss.osgi.repository.RepositoryXMLReader;
import org.jboss.osgi.repository.XPersistentRepository;
import org.jboss.osgi.resolver.XEnvironment;
import org.jboss.osgi.resolver.XIdentityCapability;
import org.jboss.osgi.resolver.XRequirement;
import org.jboss.osgi.resolver.XRequirementBuilder;
import org.jboss.osgi.resolver.XResource;
import org.jboss.shrinkwrap.api.ConfigurationBuilder;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.UrlAsset;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.importer.ZipImporter;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.osgi.service.repository.RepositoryContent;

/**
 * @author Thomas.Diesler@jboss.com
 * @since 10-May-2013
 */
public class ProvisionerSupport {

    private final XResourceProvisioner provision;
    private final ServerDeploymentHelper serverDeployer;

    public ProvisionerSupport(XResourceProvisioner provision, ModelControllerClient controllerClient) {
        this.serverDeployer = new ServerDeploymentHelper(controllerClient);
        this.provision = provision;
    }

    public List<String> installCapabilities(XEnvironment env, String... features) throws Exception {
        XRequirement[] reqs = new XRequirement[features.length];
        for (int i = 0; i < features.length; i++) {
            reqs[i] = XRequirementBuilder.create(XResource.MODULE_IDENTITY_NAMESPACE, features[i]).getRequirement();
        }
        return installCapabilities(env, reqs);
    }

    public List<String> installCapabilities(XEnvironment env, XRequirement... reqs) throws Exception {
        populateRepository(provision.getRepository(), reqs);
        ProvisionResult result = provision.findResources(env, new HashSet<XRequirement>(Arrays.asList(reqs)));
        Set<XRequirement> unsat = result.getUnsatisfiedRequirements();
        Assert.assertTrue("Nothing unsatisfied: " + unsat, unsat.isEmpty());

        List<String> runtimeNames = new ArrayList<String>();
        for (XResource res : result.getResources()) {
            String rtname;
            String name = res.getIdentityCapability().getName();
            URL deploymentStructure = getResource(name + "/jboss-deployment-structure.xml");
            if (deploymentStructure != null) {
                ConfigurationBuilder config = new ConfigurationBuilder().classLoaders(Collections.singleton(ShrinkWrap.class.getClassLoader()));
                JavaArchive archive = ShrinkWrap.createDomain(config).getArchiveFactory().create(JavaArchive.class, "wrapped-resource.jar");
                archive.as(ZipImporter.class).importFrom(((RepositoryContent) res).getContent());
                JavaArchive wrapper = ShrinkWrap.createDomain(config).getArchiveFactory().create(JavaArchive.class, "wrapped:" + name);
                wrapper.addAsManifestResource(new UrlAsset(deploymentStructure), "jboss-deployment-structure.xml");
                wrapper.add(archive, "/", ZipExporter.class);
                InputStream input = wrapper.as(ZipExporter.class).exportAsInputStream();
                rtname = serverDeployer.deploy(wrapper.getName(), input);
            } else {
                List<String> rtnames = provision.installResources(Collections.singletonList(res), String.class);
                rtname = rtnames.get(0);
            }
            runtimeNames.add(rtname);
        }
        return runtimeNames;
    }

    public void uninstallCapabilities(List<String> runtimeNames) throws ServerDeploymentException {
        if (runtimeNames != null) {
            for (String rtname : runtimeNames) {
                serverDeployer.undeploy(rtname);
            }
        }
    }

    private void populateRepository(XPersistentRepository repository, XRequirement[] reqs) throws IOException {
        for (XRequirement req : reqs) {
            String nsvalue = (String) req.getAttribute(req.getNamespace());
            URL resourceURL = getResource(nsvalue + ".xml");
            if (resourceURL != null) {
                RepositoryReader reader = RepositoryXMLReader.create(resourceURL.openStream());
                XResource auxres = reader.nextResource();
                while (auxres != null) {
                    XIdentityCapability icap = auxres.getIdentityCapability();
                    nsvalue = (String) icap.getAttribute(icap.getNamespace());
                    XRequirement ireq = XRequirementBuilder.create(icap.getNamespace(), nsvalue).getRequirement();
                    if (repository.findProviders(ireq).isEmpty()) {
                        repository.getRepositoryStorage().addResource(auxres);
                    }
                    auxres = reader.nextResource();
                }
            }
        }
    }

    private URL getResource(String resname) {
        return ProvisionerSupport.class.getResource("/repository/" + resname);
    }
}
