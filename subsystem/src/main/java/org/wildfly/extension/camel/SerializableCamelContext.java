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

package org.wildfly.extension.camel;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.net.URL;

import org.apache.camel.CamelContext;
import org.jboss.as.server.Services;
import org.jboss.gravia.runtime.ServiceLocator;
import org.jboss.gravia.utils.IOUtils;
import org.jboss.gravia.utils.IllegalArgumentAssertion;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.modules.ModuleLoader;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.ServiceController;

/**
 * A camel context definition that is serializable
 *
 * @author Thomas.Diesler@jboss.com
 * @since 30-Oct-2015
 */
public class SerializableCamelContext implements Serializable {

    private static final long serialVersionUID = -7600138397651523862L;

    private final ModuleIdentifier moduleId;
    private final byte[] contextDef;

    private transient CamelContext camelctx;

    public SerializableCamelContext(ModuleIdentifier moduleId, URL contextUrl) {
        IllegalArgumentAssertion.assertNotNull(moduleId, "moduleId");
        IllegalArgumentAssertion.assertNotNull(contextUrl, "contextUrl");
        this.moduleId = moduleId;
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            IOUtils.copyStream(contextUrl.openStream(), baos);
            this.contextDef = baos.toByteArray();
        } catch (IOException ex) {
            throw new IllegalStateException(ex);
        }
    }

    public SerializableCamelContext(ModuleIdentifier moduleId, byte[] contextDef) {
        IllegalArgumentAssertion.assertNotNull(moduleId, "moduleId");
        IllegalArgumentAssertion.assertNotNull(contextDef, "contextDef");
        this.contextDef = contextDef;
        this.moduleId = moduleId;
    }

    public ModuleIdentifier getModuleIdentifier() {
        return moduleId;
    }

    public byte[] getContextDefinition() {
        return contextDef;
    }

    public synchronized CamelContext getCamelContext() throws Exception {
        if (camelctx == null) {

            ServiceContainer container = ServiceLocator.getRequiredService(ServiceContainer.class);
            ServiceController<?> service = container.getRequiredService(Services.JBOSS_SERVICE_MODULE_LOADER);
            ModuleLoader moduleLoader = (ModuleLoader) service.getValue();
            Module module = moduleLoader.loadModule(moduleId);

            camelctx = SpringCamelContextFactory.createCamelContext(contextDef, module.getClassLoader());
        }
        return camelctx;
    }
}
