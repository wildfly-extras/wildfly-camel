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


package org.wildfly.camel.parser;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;


/**
 * The Camel subsystem state.
 *
 * @author Thomas.Diesler@jboss.com
 * @since 22-Apr-2013
 */
public final class SubsystemState  {

    private Map<String, String> contextDefinitions = new HashMap<String,String>();

    public Set<String> getContextDefinitionNames() {
        synchronized (contextDefinitions) {
            return contextDefinitions.keySet();
        }
    }

    public String getContextDefinition(String name) {
        synchronized (contextDefinitions) {
            return contextDefinitions.get(name);
        }
    }

    public void putContextDefinition(String name, String contextDefinition) {
        synchronized (contextDefinitions) {
            contextDefinitions.put(name, contextDefinition);
        }
    }

    public String removeContextDefinition(String name) {
        synchronized (contextDefinitions) {
            return contextDefinitions.remove(name);
        }
    }

}
