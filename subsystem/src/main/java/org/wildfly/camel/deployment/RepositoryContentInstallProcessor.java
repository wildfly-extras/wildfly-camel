/*
 * #%L
 * Wildfly Camel Subsystem
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


package org.wildfly.camel.deployment;

import static org.wildfly.camel.CamelMessages.MESSAGES;

import java.io.IOException;
import java.net.URL;

import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.gravia.repository.DefaultRepositoryXMLReader;
import org.jboss.gravia.repository.Repository;
import org.jboss.gravia.repository.RepositoryReader;
import org.jboss.gravia.repository.RepositoryStorage;
import org.jboss.gravia.resource.Resource;
import org.jboss.vfs.VirtualFile;
import org.wildfly.camel.CamelConstants;
import org.wildfly.extension.gravia.GraviaConstants;

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

        if (contentURL != null) {
            Repository repository = depUnit.getAttachment(GraviaConstants.REPOSITORY_KEY);
            RepositoryReader reader;
            try {
                reader = new DefaultRepositoryXMLReader(contentURL.openStream());
            } catch (IOException ex) {
                throw new DeploymentUnitProcessingException(ex);
            }
            Resource res = reader.nextResource();
            while (res != null) {
                RepositoryStorage storage = repository.adapt(RepositoryStorage.class);
                if (storage.getResource(res.getIdentity()) == null) {
                    storage.addResource(res);
                }
                res = reader.nextResource();
            }
        }

    }

    @Override
    public void undeploy(final DeploymentUnit depUnit) {
    }
}