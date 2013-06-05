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

package org.wildfly.camel;

import org.apache.camel.CamelContext;
import org.jboss.as.server.deployment.AttachmentKey;
import org.jboss.msc.service.ServiceName;

/**
 * Camel subsystem constants.
 *
 * @author Thomas.Diesler@jboss.com
 * @since 22-Apr-2013
 */
public interface CamelConstants {

    /** The base name for all camel services */
    ServiceName CAMEL_BASE_NAME = ServiceName.JBOSS.append("as", "camel");
    /** The base name for all camel context services */
    ServiceName CAMEL_CONTEXT_BASE_NAME = CAMEL_BASE_NAME.append("context");
    /** The name for the {@link CamelContextFactory} service */
    ServiceName CAMEL_CONTEXT_FACTORY_NAME = CAMEL_BASE_NAME.append("CamelContextFactory");
    /** The name for the {@link CamelContextRegistry} service */
    ServiceName CAMEL_CONTEXT_REGISTRY_NAME = CAMEL_BASE_NAME.append("CamelContextRegistry");

    /** The deployment names for spring camel context deployments */
    String CAMEL_CONTEXT_FILE_SUFFIX = "-camel-context.xml";
    String CAMEL_CONTEXT_FILE_NAME = "META-INF/jboss-camel-context.xml";

    /** */
    String CAMEL_CONTEXT_NAME_PROPERTY = "name";

    /** The {@link CamelContext} attachment key */
    AttachmentKey<CamelContext> CAMEL_CONTEXT_KEY = AttachmentKey.create(CamelContext.class);
    /** The {@link CamelContextRegistry} attachment key */
    AttachmentKey<CamelContextRegistry> CAMEL_CONTEXT_REGISTRY_KEY = AttachmentKey.create(CamelContextRegistry.class);

    /** The JNDI name for the {@link CamelContextFactory} binding */
    String CAMEL_CONTEXT_FACTORY_BINDING_NAME = "java:jboss/camel/CamelContextFactory";
    /** The JNDI name for the {@link CamelContextRegistry} binding */
    String CAMEL_CONTEXT_REGISTRY_BINDING_NAME = "java:jboss/camel/CamelContextRegistry";
}
