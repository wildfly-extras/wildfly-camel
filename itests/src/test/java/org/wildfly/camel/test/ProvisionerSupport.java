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

import static org.jboss.osgi.provision.ProvisionLogger.LOGGER;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
import org.junit.Assert;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.namespace.IdentityNamespace;
import org.osgi.service.repository.RepositoryContent;

/**
 * @author Thomas.Diesler@jboss.com
 * @since 10-May-2013
 */
public class ProvisionerSupport {

    private final BundleContext syscontext;
    private final XResourceProvisioner provision;
    private final XEnvironment environment;

    public interface ResourceHandle {

        <T> T adapt(Class<T> type);

        void uninstall();
    }

    public ProvisionerSupport(BundleContext syscontext) {
        this.syscontext = syscontext;
        this.provision = syscontext.getService(syscontext.getServiceReference(XResourceProvisioner.class));
        this.environment = syscontext.getService(syscontext.getServiceReference(XEnvironment.class));
    }

    public List<ResourceHandle> installCapability(String namespace, String... features) throws Exception {
        XRequirement[] reqs = new XRequirement[features.length];
        for (int i=0; i < features.length; i++) {
            reqs[i] = XRequirementBuilder.create(namespace, features[i]).getRequirement();
        }
        return installCapabilities(reqs);
    }

    public List<ResourceHandle> installCapabilities(XRequirement... reqs) throws Exception {

        // Populate the repository with the feature definitions
        populateRepository(provision.getRepository(), reqs);

        // Obtain provisioner result
        ProvisionResult result = provision.findResources(environment, new HashSet<XRequirement>(Arrays.asList(reqs)));
        Set<XRequirement> unsat = result.getUnsatisfiedRequirements();
        Assert.assertTrue("Nothing unsatisfied: " + unsat, unsat.isEmpty());

        // Install the provision result
        List<ResourceHandle> reshandles = new ArrayList<ResourceHandle>();
        for (XResource res : result.getResources()) {
            XIdentityCapability icap = res.getIdentityCapability();
            if (!icap.getNamespace().equals(IdentityNamespace.IDENTITY_NAMESPACE))
                throw new IllegalArgumentException("Unsupported type: " + icap);

            final InputStream input = ((RepositoryContent) res).getContent();
            final Bundle bundle = syscontext.installBundle(icap.getName(), input);
            ResourceHandle handle = new ResourceHandle() {

                @Override
                @SuppressWarnings("unchecked")
                public <T> T adapt(Class<T> type) {
                    return (T) (type == Bundle.class ? bundle : null);
                }

                @Override
                public void uninstall() {
                    try {
                        bundle.uninstall();
                    } catch (Exception ex) {
                        LOGGER.warnf(ex, "Cannot uninstall bundle: %s", bundle);
                    }
                }
            };
            reshandles.add(handle);
        }
        // Start the provisioned bundles
        for (ResourceHandle handle : reshandles) {
            Bundle bundle = handle.adapt(Bundle.class);
            bundle.start();
        }
        return reshandles;
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
