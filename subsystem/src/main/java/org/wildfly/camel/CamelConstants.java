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


package org.wildfly.camel;

import org.apache.camel.CamelContext;
import org.jboss.as.server.deployment.AttachmentKey;
import org.jboss.gravia.provision.Provisioner;
import org.jboss.gravia.repository.Repository;
import org.jboss.msc.service.ServiceName;

/**
 * Camel subsystem constants.
 *
 * @author Thomas.Diesler@jboss.com
 * @since 22-Apr-2013
 */
public interface CamelConstants {

    /** The base name for all camel services */
    ServiceName CAMEL_BASE_NAME = ServiceName.JBOSS.append("wildfly", "camel");
    /** The base name for all camel context services */
    ServiceName CAMEL_COMPONENT_BASE_NAME = CAMEL_BASE_NAME.append("component");
    /** The name for the {@link CamelComponentRegistry} service */
    ServiceName CAMEL_COMPONENT_REGISTRY_SERVICE_NAME = CAMEL_BASE_NAME.append("CamelComponentRegistry");
    /** The base name for all camel context services */
    ServiceName CAMEL_CONTEXT_BASE_NAME = CAMEL_BASE_NAME.append("context");
    /** The name for the {@link CamelContextFactory} service */
    ServiceName CAMEL_CONTEXT_FACTORY_SERVICE_NAME = CAMEL_BASE_NAME.append("CamelContextFactory");
    /** The name for the {@link CamelContextRegistry} service */
    ServiceName CAMEL_CONTEXT_REGISTRY_SERVICE_NAME = CAMEL_BASE_NAME.append("CamelContextRegistry");
    /** The name for the camel subsystem service */
    ServiceName CAMEL_SUBSYSTEM_SERVICE_NAME = CAMEL_BASE_NAME.append("Subsystem");
    /** The name for the {@link Environment} service */
    ServiceName ENVIRONMENT_SERVICE_NAME = CAMEL_BASE_NAME.append("Environment");
    /** The name for the {@link Provisioner} service */
    ServiceName PROVISIONER_SERVICE_NAME = CAMEL_BASE_NAME.append("ResourceProvisioner");
    /** The name for the {@link Repository} service */
    ServiceName REPOSITORY_SERVICE_NAME = CAMEL_BASE_NAME.append("Repository");
    /** The name for the {@link Resolver} service */
    ServiceName RESOLVER_SERVICE_NAME = CAMEL_BASE_NAME.append("Resolver");

    /** The deployment names for spring camel context deployments */
    String CAMEL_CONTEXT_FILE_SUFFIX = "-camel-context.xml";
    String CAMEL_CONTEXT_FILE_NAME = "META-INF/jboss-camel-context.xml";

    /** The deployment names for repository content deployments */
    String REPOSITORY_CONTENT_FILE_SUFFIX = "-repository-content.xml";
    String REPOSITORY_CONTENT_FILE_NAME = "META-INF/jboss-repository-content.xml";

    /** The {@link CamelContext} attachment key */
    AttachmentKey<CamelContext> CAMEL_CONTEXT_KEY = AttachmentKey.create(CamelContext.class);
    /** The {@link CamelComponentRegistry} attachment key */
    AttachmentKey<CamelComponentRegistry> CAMEL_COMPONENT_REGISTRY_KEY = AttachmentKey.create(CamelComponentRegistry.class);
    /** The {@link CamelContextRegistry} attachment key */
    AttachmentKey<CamelContextRegistry> CAMEL_CONTEXT_REGISTRY_KEY = AttachmentKey.create(CamelContextRegistry.class);
    /** The {@link Repository} attachment key */
    AttachmentKey<Repository> REPOSITORY_KEY = AttachmentKey.create(Repository.class);

    /** The JNDI name for the {@link CamelContextFactory} binding */
    String CAMEL_CONTEXT_FACTORY_BINDING_NAME = "java:jboss/camel/CamelContextFactory";
    /** The JNDI name for the {@link CamelContextRegistry} binding */
    String CAMEL_CONTEXT_REGISTRY_BINDING_NAME = "java:jboss/camel/CamelContextRegistry";
}
