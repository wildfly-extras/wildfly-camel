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
package org.wildfly.extension.camel.service;

import static org.wildfly.extension.camel.CamelLogger.LOGGER;

import java.util.Collections;
import java.util.List;

import org.apache.camel.CamelContext;
import org.apache.camel.spring.SpringCamelContext;
import org.jboss.msc.service.AbstractService;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.wildfly.camel.proxy.ProxiedAction;
import org.wildfly.camel.proxy.ProxyUtils;
import org.wildfly.extension.camel.SpringCamelContextBootstrap;

/**
 * Activates and starts all {@link SpringCamelContext} instances associated with
 * the application once all dependent services are available
 */
public final class CamelContextActivationService extends AbstractService<Void> {

    private final List<SpringCamelContextBootstrap> bootstraps;
    private final String runtimeName;

    public CamelContextActivationService(List<SpringCamelContextBootstrap> bootstraps, String runtimeName) {
        this.bootstraps = bootstraps;
        this.runtimeName = runtimeName;
    }

    @Override
    public void start(StartContext context) throws StartException {
        ClassLoader tccl = SecurityActions.getContextClassLoader();
        for (SpringCamelContextBootstrap bootstrap : bootstraps) {
            try {
                SecurityActions.setContextClassLoader(bootstrap.getClassLoader());
                try {
                    for (CamelContext camelctx : bootstrap.createSpringCamelContexts()) {
                        try {
                            ProxyUtils.invokeProxied(new ProxiedAction() {
                                @Override
                                public void run() throws Exception {
                                    camelctx.start();
                                }
                            }, bootstrap.getClassLoader());
                        } catch (Exception ex) {
                            throw new StartException("Cannot start camel context: " + camelctx.getName(), ex);
                        }
                    }
                } catch (Exception e) {
                    throw new StartException("Cannot create camel context: " + runtimeName, e);
                }
            } finally {
                SecurityActions.setContextClassLoader(tccl);
            }
        }
    }

    @Override
    public void stop(StopContext context) {
        Collections.reverse(bootstraps);
        for (SpringCamelContextBootstrap bootstrap: bootstraps) {
            List<SpringCamelContext> camelctxList = bootstrap.getSpringCamelContexts();
            for (CamelContext camelctx : camelctxList) {
                try {
                    camelctx.stop();
                } catch (Exception ex) {
                    LOGGER.warn("Cannot stop camel context: " + camelctx.getName(), ex);
                }
            }
        }
    }
}
