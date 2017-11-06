/*
 * #%L
 * Wildfly Camel :: Subsystem
 * %%
 * Copyright (C) 2013 - 2017 RedHat
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

import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;

import org.apache.camel.CamelContext;
import org.jboss.msc.service.AbstractService;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.wildfly.extension.camel.SpringCamelContextFactory;
import org.wildfly.extension.camel.proxy.ProxyUtils;

public final class CamelContextActivationService extends AbstractService<Void> {

    private final List<URL> camelContextUrls;
    private final List<CamelContext> camelContexts = new ArrayList<>();
    private final ClassLoader classLoader;
    private final String runtimeName;

    public CamelContextActivationService(List<URL> camelContextUrls, ClassLoader classLoader, String runtimeName) {
        this.camelContextUrls = camelContextUrls;
        this.classLoader = classLoader;
        this.runtimeName = runtimeName;
    }

    @Override
    public void start(StartContext context) throws StartException {
        for (URL contextURL : camelContextUrls) {
            ClassLoader tccl = Thread.currentThread().getContextClassLoader();
            try {
                Thread.currentThread().setContextClassLoader(classLoader);
                for (final CamelContext camelctx : SpringCamelContextFactory.createCamelContextList(contextURL, classLoader)) {
                    try {
                        ProxyUtils.invokeProxied(new Callable<Void>() {
                            @Override
                            public Void call() throws Exception {
                                camelctx.start();
                                return null;
                            }
                        }, classLoader);
                        camelContexts.add(camelctx);
                    } catch (Exception ex) {
                        LOGGER.error("Cannot start camel context: " + camelctx.getName(), ex);
                    }
                }
            } catch (Exception ex) {
                throw new IllegalStateException("Cannot create camel context: " + runtimeName, ex);
            } finally {
                Thread.currentThread().setContextClassLoader(tccl);
            }
        }
    }

    @Override
    public void stop(StopContext context) {
        Collections.reverse(camelContexts);
        for (CamelContext camelctx : camelContexts) {
            try {
                camelctx.stop();
            } catch (Exception ex) {
                LOGGER.warn("Cannot stop camel context: " + camelctx.getName(), ex);
            }
        }
    }
}
