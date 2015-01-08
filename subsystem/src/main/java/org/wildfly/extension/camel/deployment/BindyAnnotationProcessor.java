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
import java.util.Collections;
import java.util.List;

import org.apache.camel.dataformat.bindy.annotation.CsvRecord;
import org.apache.camel.dataformat.bindy.annotation.FixedLengthRecord;
import org.jboss.as.ee.component.deployers.BooleanAnnotationInformationFactory;
import org.jboss.as.ee.metadata.ClassAnnotationInformationFactory;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.annotation.CompositeIndex;

/**
 * Process the bindy annotations
 *
 * @author Thomas.Diesler@jboss.com
 * @since 08-Jan-2015
 */
@SuppressWarnings( "rawtypes" )
public class BindyAnnotationProcessor implements DeploymentUnitProcessor {

    final List<ClassAnnotationInformationFactory> factories;
    
    public BindyAnnotationProcessor() {
        List<ClassAnnotationInformationFactory> factories = new ArrayList<ClassAnnotationInformationFactory>();
        factories.add(new BooleanAnnotationInformationFactory<CsvRecord>(CsvRecord.class));
        factories.add(new BooleanAnnotationInformationFactory<FixedLengthRecord>(FixedLengthRecord.class));
        this.factories = Collections.unmodifiableList(factories);
    }
    
    public final void deploy(final DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        final CompositeIndex index = deploymentUnit.getAttachment(Attachments.COMPOSITE_ANNOTATION_INDEX);
        if (index == null) 
            return;

        for (final ClassAnnotationInformationFactory factory : factories) {
            factory.createAnnotationInformation(index, false);
        }
    }

    public void undeploy(final DeploymentUnit context) {

    }
}
