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

package org.wildfly.camel.deployment;

import static org.wildfly.camel.CamelMessages.MESSAGES;

import java.io.IOException;
import java.net.URL;
import org.jboss.as.osgi.OSGiConstants;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.osgi.repository.RepositoryReader;
import org.jboss.osgi.repository.RepositoryStorage;
import org.jboss.osgi.repository.RepositoryXMLReader;
import org.jboss.osgi.repository.XPersistentRepository;
import org.jboss.osgi.repository.XRepository;
import org.jboss.osgi.resolver.XIdentityCapability;
import org.jboss.osgi.resolver.XRequirement;
import org.jboss.osgi.resolver.XRequirementBuilder;
import org.jboss.osgi.resolver.XResource;
import org.jboss.vfs.VirtualFile;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.wildfly.camel.CamelConstants;

/**
 * Processes repository content deployments.
 *
 * @author Thomas.Diesler@jboss.com
 * @since 14-Jun-2013
 */
public class RepositoryContentInstallProcessor implements DeploymentUnitProcessor {

    @Override
    public void deploy(final DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit depUnit = phaseContext.getDeploymentUnit();
        final String runtimeName = depUnit.getName();

        URL contentURL = null;
        try {
            if (runtimeName.endsWith(CamelConstants.REPOSITORY_CONTENT_FILE_SUFFIX)) {
                contentURL = depUnit.getAttachment(Attachments.DEPLOYMENT_CONTENTS).asFileURL();
            } else {
                VirtualFile rootFile = depUnit.getAttachment(Attachments.DEPLOYMENT_ROOT).getRoot();
                VirtualFile child = rootFile.getChild(CamelConstants.REPOSITORY_CONTENT_FILE_NAME);
                contentURL = child.isFile() ? child.asFileURL() : null;
            }
        } catch (IOException ex) {
            throw MESSAGES.cannotCreateCamelContext(ex, runtimeName);
        }

        BundleContext syscontext = depUnit.getAttachment(OSGiConstants.SYSTEM_CONTEXT_KEY);
        if (contentURL == null || syscontext == null)
            return;

        ServiceReference<XRepository> sref = syscontext.getServiceReference(XRepository.class);
        XPersistentRepository repository = (XPersistentRepository) syscontext.getService(sref);

        RepositoryReader reader;
        try {
            reader = RepositoryXMLReader.create(contentURL.openStream());
        } catch (IOException ex) {
            throw new DeploymentUnitProcessingException(ex);
        }
        XResource auxres = reader.nextResource();
        while (auxres != null) {
            XIdentityCapability icap = auxres.getIdentityCapability();
            String nsvalue = (String) icap.getAttribute(icap.getNamespace());
            XRequirement ireq = XRequirementBuilder.create(icap.getNamespace(), nsvalue).getRequirement();
            RepositoryStorage storage = repository.adapt(RepositoryStorage.class);
            if (repository.findProviders(ireq).isEmpty()) {
                storage.addResource(auxres);
            }
            auxres = reader.nextResource();
        }
    }

    @Override
    public void undeploy(final DeploymentUnit depUnit) {
    }
}
