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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The Subsystem Logger
 *
 * @author Thomas.Diesler@jboss.com
 * @since 22-Apr-2013
 */
public interface CamelLogger {

    /**
     * A logger with the category {@code org.wildfly.camel}.
     */
    Logger LOGGER = LoggerFactory.getLogger(CamelLogger.class.getPackage().getName());
}
