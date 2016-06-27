/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.wildfly.camel.test.ldap;

import java.util.UUID;

import org.apache.commons.io.FileUtils;
import org.apache.directory.server.annotations.CreateLdapServer;
import org.apache.directory.server.core.annotations.ApplyLdifFiles;
import org.apache.directory.server.core.api.DirectoryService;
import org.apache.directory.server.core.api.changelog.ChangeLog;
import org.apache.directory.server.core.factory.DSAnnotationProcessor;
import org.apache.directory.server.core.factory.DefaultDirectoryServiceFactory;
import org.apache.directory.server.core.factory.DirectoryServiceFactory;
import org.apache.directory.server.core.factory.PartitionFactory;
import org.apache.directory.server.factory.ServerAnnotationProcessor;
import org.apache.directory.server.ldap.LdapServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DirectoryServiceBuilder {

    private static final Logger LOG = LoggerFactory.getLogger(DirectoryServiceBuilder.class);

    static class SetupResult {
        private DirectoryService directoryService;
        private LdapServer ldapServer;
        public DirectoryService getDirectoryService() {
            return directoryService;
        }
        public LdapServer getLdapServer() {
            return ldapServer;
        }
    }

    // Hide ctor
    private DirectoryServiceBuilder() {
    }

    public static SetupResult setupDirectoryService(Class<?> testClass) throws Exception {

        // Define a default class DS then
        DirectoryServiceFactory dsf = DefaultDirectoryServiceFactory.class.newInstance();

        SetupResult result = new SetupResult();
        result.directoryService = dsf.getDirectoryService();
        result.directoryService.getChangeLog().setEnabled(true);

        dsf.init("default" + UUID.randomUUID().toString());

        // Apply the class LDIFs
        ApplyLdifFiles applyLdifFiles = testClass.getAnnotation(ApplyLdifFiles.class);
        if (applyLdifFiles != null) {
            LOG.debug("Applying {} to {}", applyLdifFiles.value(), testClass.getName());
            DSAnnotationProcessor.injectLdifFiles(applyLdifFiles.clazz(), result.directoryService, applyLdifFiles.value());
        }

        // check if it has a LdapServerBuilder then use the DS created above
        CreateLdapServer ldapServerAnnotation = testClass.getAnnotation(CreateLdapServer.class);
        if (ldapServerAnnotation != null) {
            result.ldapServer = ServerAnnotationProcessor.instantiateLdapServer(ldapServerAnnotation, result.directoryService);
            result.ldapServer.setDirectoryService(result.directoryService);
            result.ldapServer.start();
        }

        // print out information which partition factory we use
        DirectoryServiceFactory dsFactory = DefaultDirectoryServiceFactory.class.newInstance();
        PartitionFactory partitionFactory = dsFactory.getPartitionFactory();
        LOG.debug("Using partition factory {}", partitionFactory.getClass().getSimpleName());

        return result;
    }

    public static void shutdownDirectoryService(DirectoryService service) throws Exception {
        if (service != null) {
            LOG.debug("Shuting down DS for {}", service.getInstanceId());
            service.shutdown();
            FileUtils.deleteDirectory(service.getInstanceLayout().getInstanceDirectory());
        }
    }

    public static long getCurrentRevision(DirectoryService dirService) throws Exception {
        if ((dirService != null) && (dirService.getChangeLog().isEnabled())) {
            long revision = dirService.getChangeLog().getCurrentRevision();
            LOG.debug("Create revision {}", revision);
            return revision;
        }
        return 0;
    }

    public static void revert(DirectoryService dirService, long revision) throws Exception {
        ChangeLog cl = dirService.getChangeLog();
        if (cl.isEnabled() && (revision < cl.getCurrentRevision())) {
            LOG.debug("Revert revision {}", revision);
            dirService.revert(revision);
        }
    }
}
