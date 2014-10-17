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

import static org.jboss.logging.Logger.Level.INFO;

import org.jboss.logging.BasicLogger;
import org.jboss.logging.Logger;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;

/**
 * Logging Id ranges: 20000-20099
 *
 * https://community.jboss.org/wiki/LoggingIds
 *
 * @author Thomas.Diesler@jboss.com
 * @since 22-Apr-2013
 */
@MessageLogger(projectCode = "JBAS")
public interface CamelLogger extends BasicLogger {

    /**
     * A logger with the category {@code org.wildfly.camel}.
     */
    CamelLogger LOGGER = Logger.getMessageLogger(CamelLogger.class, "org.wildfly.camel");

    @LogMessage(level = INFO)
    @Message(id = 20000, value = "Activating Camel Subsystem")
    void infoActivatingSubsystem();

    @LogMessage(level = INFO)
    @Message(id = 20001, value = "Register camel context: %s")
    void infoRegisterCamelContext(String name);

    @LogMessage(level = INFO)
    @Message(id = 20002, value = "Bound camel naming object: %s")
    void infoBoundCamelNamingObject(String jndiName);

    @LogMessage(level = INFO)
    @Message(id = 20003, value = "Unbind camel naming object: %s")
    void infoUnbindCamelNamingObject(String jndiName);

    @LogMessage(level = INFO)
    @Message(id = 20004, value = "Register camel component: %s")
    void infoRegisterCamelComponent(String name);
}
