/*
 * #%L
 * Wildfly Camel Subsystem
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


package org.wildfly.camel;

import org.apache.camel.CamelContext;
import org.jboss.gravia.provision.ProvisionException;
import org.jboss.gravia.resource.Resource;
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

    @Message(id = 20107, value = "Cannot load component from module: %s")
    IllegalStateException cannotLoadComponentFromModule(@Cause Throwable th, ModuleIdentifier moduleId);

    @Message(id = 20108, value = "Cannot load component type for name: %s")
    IllegalStateException cannotLoadComponentType(@Cause Throwable th, String name);

    @Message(id = 20109, value = "Type is not a Component implementation. Found: %s")
    IllegalStateException componentTypeException(Class<?> type);

    @Message(id = 20110, value = "Cannot initialize naming context")
    IllegalStateException cannotInitializeNamingContext(@Cause Throwable th);

    @Message(id = 20111, value = "Cannot install feature to repository: %s")
    IllegalStateException cannotInstallCamelFeature(String name);

    @Message(id = 20112, value = "Cannot install resource to environment: %s")
    IllegalStateException cannotInstallResourceToEnvironment(String name);

    @Message(id = 20113, value = "Camel component with that name already registered: %s")
    IllegalStateException camelComponentAlreadyRegistered(String contextName);

    @Message(id = 20114, value = "Cannot create repository storage area")
    IllegalStateException cannotCreateRepositoryStorageArea(@Cause Throwable th);

    @Message(id = 20115, value = "Cannot provision resource: %s")
    ProvisionException cannotProvisionResource(@Cause Throwable th, Resource res);

    @Message(id = 20116, value = "Cannot uninstall provisioned resource: %s")
    ProvisionException cannotUninstallProvisionedResource(@Cause Throwable th, Resource res);
}

