/*
 * #%L
 * Wildfly Camel :: Subsystem
 * %%
 * Copyright (C) 2013 - 2014 RedHat
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */


package org.wildfly.extension.camel;

import org.apache.camel.CamelContext;
import org.jboss.as.server.deployment.AttachmentKey;
import org.jboss.as.server.deployment.AttachmentList;
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
    /** The name for the {@link CamelContextFactory} service */
    ServiceName CAMEL_CONTEXT_FACTORY_SERVICE_NAME = CAMEL_BASE_NAME.append("CamelContextFactory");
    /** The name for the {@link CamelContextRegistry} service */
    ServiceName CAMEL_CONTEXT_REGISTRY_SERVICE_NAME = CAMEL_BASE_NAME.append("CamelContextRegistry");
    /** The name for the {@link ContextCreateHandlerRegistry} service */
    ServiceName CONTEXT_CREATE_HANDLER_REGISTRY_SERVICE_NAME = CAMEL_BASE_NAME.append("ContextCreateHandlerRegistry");
    /** The name for the camel subsystem service */
    ServiceName CAMEL_SUBSYSTEM_SERVICE_NAME = CAMEL_BASE_NAME.append("Subsystem");

    /** The deployment names for spring camel context deployments */
    String CAMEL_CONTEXT_FILE_SUFFIX = "-camel-context.xml";
    String CAMEL_CONTEXT_FILE_NAME = "META-INF/jboss-camel-context.xml";

    /** This file hold the components to use. */
    String CAMEL_COMPONENTS_FILE_NAME = "META-INF/jboss-camel-components.properties";

    /** The Switchyard marker file */
    String SWITCHYARD_MARKER_FILE = "META-INF/switchyard.xml";

    /** The deployment names for repository content deployments */
    String REPOSITORY_CONTENT_FILE_SUFFIX = "-repository-content.xml";
    String REPOSITORY_CONTENT_FILE_NAME = "META-INF/jboss-repository-content.xml";

    /** The {@link CamelContext} attachment key */
    AttachmentKey<AttachmentList<CamelContext>> CAMEL_CONTEXT_KEY = AttachmentKey.createList(CamelContext.class);
    /** The {@link CamelContextRegistry} attachment key */
    AttachmentKey<CamelContextRegistry> CAMEL_CONTEXT_REGISTRY_KEY = AttachmentKey.create(CamelContextRegistry.class);
    /** The {@link CamelContextFactory} attachment key */
    AttachmentKey<CamelContextFactory> CAMEL_CONTEXT_FACTORY_KEY = AttachmentKey.create(CamelContextFactory.class);
    /** The {@link ContextCreateHandlerRegistry} attachment key */
    AttachmentKey<ContextCreateHandlerRegistry> CONTEXT_CREATE_HANDLER_REGISTRY_KEY = AttachmentKey.create(ContextCreateHandlerRegistry.class);

    /** The JNDI name for the {@link CamelContextFactory} binding */
    String CAMEL_CONTEXT_FACTORY_BINDING_NAME = "java:jboss/camel/CamelContextFactory";
    /** The JNDI name for the {@link CamelContextRegistry} binding */
    String CAMEL_CONTEXT_REGISTRY_BINDING_NAME = "java:jboss/camel/CamelContextRegistry";
    /** The name for the {@link CamelContext} service */
    String CAMEL_CONTEXT_BINDING_NAME = "java:jboss/camel/context";
}
