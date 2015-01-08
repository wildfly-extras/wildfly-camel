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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.camel.CamelContext;
import org.apache.camel.dataformat.bindy.annotation.CsvRecord;
import org.apache.camel.dataformat.bindy.annotation.FixedLengthRecord;
import org.apache.camel.impl.DefaultPackageScanClassResolver;
import org.apache.camel.spi.PackageScanClassResolver;
import org.apache.camel.spi.PackageScanFilter;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.annotation.CompositeIndex;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.modules.ModuleClassLoader;
import org.wildfly.extension.camel.CamelConstants;
import org.wildfly.extension.camel.CamelContextFactory;
import org.wildfly.extension.camel.CamelContextFactory.ContextCreateHandler;

/**
 * Process the bindy annotations
 *
 * @author Thomas.Diesler@jboss.com
 * @since 08-Jan-2015
 */
public class PackageScanResolverProcessor implements DeploymentUnitProcessor {

    private ContextCreateHandler contextCreateHandler;
    
    public final void deploy(final DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        
        final DeploymentUnit depUnit = phaseContext.getDeploymentUnit();
        final CompositeIndex index = depUnit.getAttachment(Attachments.COMPOSITE_ANNOTATION_INDEX);
        if (index == null) 
            return;
        
        List<AnnotationInstance> annotations = new ArrayList<>();
        annotations.addAll(index.getAnnotations(DotName.createSimple(CsvRecord.class.getName())));
        annotations.addAll(index.getAnnotations(DotName.createSimple(FixedLengthRecord.class.getName())));
        
        // Collect infos about annotated classes
        final Map<DotName, Set<ClassInfo>> annotatedClasses = new HashMap<>();
        for (AnnotationInstance instance : annotations) {
            DotName name = instance.name();
            AnnotationTarget target = instance.target();
            if (target instanceof ClassInfo) {
                Set<ClassInfo> set = annotatedClasses.get(name);
                if (set == null) {
                    annotatedClasses.put(name, set = new HashSet<>());
                }
                set.add((ClassInfo) target);
            }
        }
        if (annotatedClasses.isEmpty()) 
            return;
        
        final ModuleClassLoader classLoader = depUnit.getAttachment(Attachments.MODULE).getClassLoader();
        contextCreateHandler = new ContextCreateHandler() {
            @Override
            public void setup(CamelContext camelctx) {
                PackageScanClassResolver resolver = new DefaultPackageScanClassResolver() {
                    protected void find(PackageScanFilter filter, String packageName, ClassLoader loader, Set<Class<?>> classes) {                        
                        if (loader == classLoader) {
                            for (Entry<DotName, Set<ClassInfo>> entry : annotatedClasses.entrySet()) {
                                for (ClassInfo classInfo : entry.getValue()) {
                                    Class<?> targetClass = loadClass(classLoader, classInfo.name());
                                    if (filter.matches(targetClass)) {
                                        classes.add(targetClass);
                                    }
                                }
                            }
                        }
                    }
                };
                resolver.addClassLoader(classLoader);
                camelctx.setPackageScanClassResolver(resolver);
            }
        };
        
        CamelContextFactory factory = depUnit.getAttachment(CamelConstants.CAMEL_CONTEXT_FACTORY_KEY);
        factory.addContextCreateHandler(classLoader, contextCreateHandler);
    }

    private Class<?> loadClass(ModuleClassLoader classLoader, DotName dotname) {
        try {
            return classLoader.loadClass(dotname.toString());
        } catch (ClassNotFoundException ex) {
            throw new IllegalStateException(ex);
        }
    }
    
    public void undeploy(DeploymentUnit depUnit) {
        CamelContextFactory factory = depUnit.getAttachment(CamelConstants.CAMEL_CONTEXT_FACTORY_KEY);
        ModuleClassLoader classLoader = depUnit.getAttachment(Attachments.MODULE).getClassLoader();
        factory.removeContextCreateHandler(classLoader, contextCreateHandler);
    }
}
