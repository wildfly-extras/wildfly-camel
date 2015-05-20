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
package org.wildfly.extension.camel.deployment;

import java.util.ArrayList;
import java.util.List;

import org.jboss.as.server.deployment.AttachmentKey;

/**
 * Created by chirino on 2/23/15.
 */
public final class CamelDeploymentSettings {

    public static final AttachmentKey<CamelDeploymentSettings> ATTACHMENT_KEY = AttachmentKey.create(CamelDeploymentSettings.class);

    private boolean enabled = true;
    private ArrayList<String> modules = new ArrayList<>();

    boolean isEnabled() {
        return enabled;
    }

    void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    void addModule(String module) {
        modules.add(module);
    }

    List<String> getModules() {
        return new ArrayList<>(modules);
    }

    void setModules(List<String> value) {
        this.modules = new ArrayList<>(value);
    }
}
