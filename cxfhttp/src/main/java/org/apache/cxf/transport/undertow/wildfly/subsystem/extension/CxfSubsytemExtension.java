/*
 * #%L
 * Wildfly Camel :: Subsystem
 * %%
 * Copyright (C) 2013 - 2018 RedHat
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
package org.apache.cxf.transport.undertow.wildfly.subsystem.extension;

import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.ServiceTarget;
import org.wildfly.extension.camel.CamelSubsytemExtension;
import org.wildfly.extension.camel.ContextCreateHandler;
import org.wildfly.extension.camel.parser.SubsystemState;

/**
 * A {@link CamelSubsytemExtension} that provides {@link CxfDefaultBusHandler}.
 *
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 */
public class CxfSubsytemExtension implements CamelSubsytemExtension {

    @Override
    public ContextCreateHandler getContextCreateHandler(ServiceContainer serviceContainer, ServiceTarget serviceTarget,
            SubsystemState subsystemState) {
        return new CxfDefaultBusHandler();
    }

}
