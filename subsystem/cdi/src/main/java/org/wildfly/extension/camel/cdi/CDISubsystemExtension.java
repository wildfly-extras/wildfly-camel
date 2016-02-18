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
package org.wildfly.extension.camel.cdi;

import org.jboss.as.server.DeploymentProcessorTarget;
import org.jboss.as.server.deployment.Phase;
import org.wildfly.extension.camel.CamelSubsytemExtension;
import org.wildfly.extension.camel.parser.CamelExtension;
import org.wildfly.extension.camel.parser.CamelSubsystemAdd;
import org.wildfly.extension.camel.parser.SubsystemState;

public class CDISubsystemExtension implements CamelSubsytemExtension {

    @Override
    public void addDeploymentProcessor(DeploymentProcessorTarget processorTarget, SubsystemState subsystemState) {
        processorTarget.addDeploymentProcessor(CamelExtension.SUBSYSTEM_NAME, Phase.INSTALL, CamelSubsystemAdd.INSTALL_CDI_BEAN_ARCHIVE_PROCESSOR, new CDIBeanArchiveProcessor());
    }
}
