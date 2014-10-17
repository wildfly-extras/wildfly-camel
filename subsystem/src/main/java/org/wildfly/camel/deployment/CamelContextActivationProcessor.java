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


package org.wildfly.camel.deployment;

import static org.wildfly.camel.CamelMessages.MESSAGES;

import org.apache.camel.CamelContext;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.wildfly.camel.CamelConstants;

/**
 * Start/Stop the {@link CamelContext}
 *
 * @author Thomas.Diesler@jboss.com
 * @since 22-Apr-2013
 */
public class CamelContextActivationProcessor implements DeploymentUnitProcessor {

    @Override
    public void deploy(final DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        DeploymentUnit depUnit = phaseContext.getDeploymentUnit();
        CamelContext camelctx = depUnit.getAttachment(CamelConstants.CAMEL_CONTEXT_KEY);
        if (camelctx == null)
            return;

        // Start the camel context
        try {
            camelctx.start();
        } catch (Exception ex) {
            throw MESSAGES.cannotStartCamelContext(ex, camelctx);
        }
    }

    @Override
    public void undeploy(final DeploymentUnit depUnit) {
        // Stop the camel context
        CamelContext camelctx = depUnit.getAttachment(CamelConstants.CAMEL_CONTEXT_KEY);
        if (camelctx != null) {
            try {
                camelctx.stop();
            } catch (Exception ex) {
                throw MESSAGES.cannotStopCamelContext(ex, camelctx);
            }
        }
    }
}
