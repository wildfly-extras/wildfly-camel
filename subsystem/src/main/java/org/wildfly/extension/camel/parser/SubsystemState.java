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


package org.wildfly.extension.camel.parser;

import static org.wildfly.extension.camel.CamelLogger.LOGGER;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jboss.modules.ModuleIdentifier;


/**
 * The Camel subsystem state.
 *
 * @author Thomas.Diesler@jboss.com
 * @since 22-Apr-2013
 */
public final class SubsystemState  {

    private final Map<String, String> contextDefinitions = new HashMap<String,String>();
    private final List<ModuleIdentifier> componentIds = new ArrayList<>();

    public SubsystemState() {
        try (InputStream ins = getClass().getResourceAsStream("/META-INF/camel-component-ids.txt")) {
            BufferedReader br = new BufferedReader(new InputStreamReader(ins));
            String line = br.readLine();
            while (line != null && line.length() > 0) {
                componentIds.add(ModuleIdentifier.create(line));
                line = br.readLine();
            }
        } catch (IOException ex) {
            LOGGER.error("Cannot read camel component ids", ex);
        }
    }

    public List<ModuleIdentifier> getComponentIds() {
        return Collections.unmodifiableList(componentIds);
    }

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
