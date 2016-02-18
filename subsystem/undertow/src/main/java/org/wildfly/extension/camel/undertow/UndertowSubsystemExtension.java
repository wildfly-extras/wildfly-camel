/*
 * #%L
 * Wildfly Camel :: Subsystem
 * %%
 * Copyright (C) 2013 - 2015 RedHat
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
package org.wildfly.extension.camel.undertow;

import org.apache.camel.Component;
import org.jboss.msc.service.ServiceTarget;
import org.wildfly.extension.camel.CamelSubsytemExtension;
import org.wildfly.extension.camel.parser.SubsystemRuntimeState;

public class UndertowSubsystemExtension implements CamelSubsytemExtension {

    @Override
    public void addExtensionServices(ServiceTarget serviceTarget, SubsystemRuntimeState runtimeState) {
        CamelUndertowHostService.addService(serviceTarget, runtimeState);
    }

    @Override
    public Component resolveComponent(String name, SubsystemRuntimeState runtimeState) {
        return name.equals("undertow") ? new WildFlyUndertowComponent(runtimeState) : null;
    }
}
