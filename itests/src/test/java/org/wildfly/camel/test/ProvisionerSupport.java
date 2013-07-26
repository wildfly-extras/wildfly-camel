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


package org.wildfly.camel.test;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jboss.gravia.provision.ProvisionResult;
import org.jboss.gravia.provision.Provisioner;
import org.jboss.gravia.provision.Provisioner.ResourceHandle;
import org.jboss.gravia.repository.DefaultRepositoryXMLReader;
import org.jboss.gravia.repository.Repository;
import org.jboss.gravia.repository.RepositoryReader;
import org.jboss.gravia.repository.RepositoryStorage;
import org.jboss.gravia.resource.DefaultRequirementBuilder;
import org.jboss.gravia.resource.Requirement;
import org.jboss.gravia.resource.Resource;

/**
 * @author Thomas.Diesler@jboss.com
 * @since 10-May-2013
 */
public class ProvisionerSupport {

    private Provisioner provisioner;

    public ProvisionerSupport(Provisioner provisioner) {
        this.provisioner = provisioner;
    }

    public Provisioner getResourceProvisioner() {
        return provisioner;
    }

    public Repository getRepository() {
        return provisioner.getRepository();
    }

    public List<ResourceHandle> installCapabilities(String namespace, String... features) throws Exception {
        if (namespace == null)
            throw new IllegalArgumentException("Null namespace");
        if (features == null)
            throw new IllegalArgumentException("Null features");

        Requirement[] reqs = new Requirement[features.length];
        for (int i=0; i < features.length; i++) {
            reqs[i] = new DefaultRequirementBuilder(namespace, features[i]).getRequirement();
        }
        return installCapabilities(reqs);
    }

    public List<ResourceHandle> installCapabilities(Requirement... reqs) throws Exception {
        if (reqs == null)
            throw new IllegalArgumentException("Null reqs");

        // Obtain provisioner result
        ProvisionResult result = provisioner.findResources(new HashSet<Requirement>(Arrays.asList(reqs)));
        Set<Requirement> unsat = result.getUnsatisfiedRequirements();
        if (!unsat.isEmpty())
            throw new IllegalStateException("Unsatisfied requirements: " + unsat);

        Exception installError = null;
        List<ResourceHandle> reshandles = new ArrayList<ResourceHandle>();

        // Install the provision result
        for (Resource res : result.getResources()) {
            try {
                ResourceHandle handle = provisioner.installResource(res, result.getMapping());
                reshandles.add(0, handle);
            } catch (Exception th) {
                installError = th;
                break;
            }
        }

        // Uninstall in case of error
        if (installError != null) {
            for (ResourceHandle handle : reshandles) {
                try {
                    handle.uninstall();
                } catch (Exception ex) {
                    // ignore
                }
            }
            throw installError;
        }

        return Collections.unmodifiableList(reshandles);
    }

    public void populateRepository(ClassLoader classLoader, String... features) throws IOException {
        if (features == null)
            throw new IllegalArgumentException("Null features");

        for (String feature : features) {
            InputStream input = getFeatureResource(classLoader, feature);
            if (input != null) {
                RepositoryReader reader = new DefaultRepositoryXMLReader(input);
                Resource auxres = reader.nextResource();
                while (auxres != null) {
                    RepositoryStorage storage = getRepository().adapt(RepositoryStorage.class);
                    if (storage.getResource(auxres.getIdentity()) == null) {
                        storage.addResource(auxres);
                    }
                    auxres = reader.nextResource();
                }
            }
        }
    }

    private InputStream getFeatureResource(ClassLoader classLoader, String feature) {
        // [TODO] parameterize this
        return classLoader.getResourceAsStream("/repository/" + feature  + ".xml");
    }
}
