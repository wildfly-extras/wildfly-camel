/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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
import org.jboss.logging.Messages;
import org.jboss.logging.annotations.Cause;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageBundle;
import org.jboss.modules.ModuleIdentifier;

/**
 * Logging Id ranges: 20100-20199
 *
 * https://community.jboss.org/wiki/LoggingIds
 *
 * @author Thomas.Diesler@jboss.com
 * @since 22-Apr-2013
 */
@MessageBundle(projectCode = "JBAS")
public interface CamelMessages {

    /**
     * The messages.
     */
    CamelMessages MESSAGES = Messages.getBundle(CamelMessages.class);

    @Message(id = 20100, value = "%s is null")
    IllegalArgumentException illegalArgumentNull(String name);

    @Message(id = 20101, value = "Cannot create camel context: %s")
    IllegalStateException cannotCreateCamelContext(@Cause Throwable th, String runtimeName);

    @Message(id = 20102, value = "Cannot start camel context: %s")
    IllegalStateException cannotStartCamelContext(@Cause Throwable th, CamelContext context);

    @Message(id = 20103, value = "Cannot stop camel context: %s")
    IllegalStateException cannotStopCamelContext(@Cause Throwable th, CamelContext context);

    @Message(id = 20104, value = "Illegal camel context name: %s")
    IllegalArgumentException illegalCamelContextName(String name);

    @Message(id = 20105, value = "Camel context with that name already registered: %s")
    IllegalStateException camelContextAlreadyRegistered(String contextName);

    @Message(id = 20106, value = "Cannot find component properties in: %s")
    IllegalStateException cannotFindComponentProperties(ModuleIdentifier moduleId);

    @Message(id = 20107, value = "Cannot load component properties from: %s")
    IllegalStateException cannotLoadComponentProperties(@Cause Throwable th, ModuleIdentifier moduleId);

    @Message(id = 20108, value = "Cannot load component type for name: %s")
    IllegalStateException cannotLoadComponentType(@Cause Throwable th, String name);

    @Message(id = 20109, value = "Type is not a Component implementation. Found: %s")
    IllegalStateException componentTypeException(Class<?> type);

    @Message(id = 20110, value = "Cannot initialize naming context")
    IllegalStateException cannotInitializeNamingContext(@Cause Throwable th);

    @Message(id = 20111, value = "Cannot install feature to repository: %s")
    IllegalStateException cannotInstallCamelFeature(String name);
}

