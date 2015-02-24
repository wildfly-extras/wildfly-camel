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
package org.wildfly.extension.camel.deployment.config;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by chirino on 2/23/15.
 */
public class CamelDeploymentSettings {

    private boolean enabled = true;
    private ArrayList<String> modules = new ArrayList<>();


    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void addModule(String module) {
        modules.add(module);
    }

    public List<String> getModules() {
        return new ArrayList<>(modules);
    }

    public void setModules(List<String> value) {
        this.modules = new ArrayList<>(value);
    }
}
