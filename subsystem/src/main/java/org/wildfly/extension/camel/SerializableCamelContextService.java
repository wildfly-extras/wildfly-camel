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

import org.apache.camel.CamelContext;
import org.jboss.gravia.utils.IllegalArgumentAssertion;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;

/**
 * A general MSC service for a camel context
 *
 * @author Thomas.Diesler@jboss.com
 * @since 30-Oct-2015
 */
public class SerializableCamelContextService implements Service<SerializableCamelContext> {

    private final SerializableCamelContext contextDef;
    private CamelContext camelctx;

    public SerializableCamelContextService(SerializableCamelContext contextDef) {
        IllegalArgumentAssertion.assertNotNull(contextDef, "contextDef");
        this.contextDef = contextDef;
    }

    @Override
    public void start(StartContext context) throws StartException {
        try {
            camelctx = contextDef.getCamelContext();
            camelctx.start();
        } catch (RuntimeException rte) {
            throw rte;
        } catch (Exception ex) {
            throw new StartException(ex);
        }
    }

    @Override
    public void stop(StopContext context) {
        try {
            if (camelctx != null) {
                camelctx.stop();
            }
        } catch (RuntimeException rte) {
            throw rte;
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    @Override
    public SerializableCamelContext getValue() {
        return contextDef;
    }
}
