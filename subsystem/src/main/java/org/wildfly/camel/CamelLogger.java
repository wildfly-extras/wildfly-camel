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
