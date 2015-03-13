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

package org.wildfly.extension.camel.handler;

import org.apache.camel.CamelContext;
import org.apache.camel.Endpoint;
import org.apache.camel.Route;
import org.apache.camel.StartupListener;
import org.apache.camel.component.cxf.CxfEndpoint;
import org.wildfly.extension.camel.ContextCreateHandler;

/**
 * A {@link ContextCreateHandler} for that validates the camel context
 *
 * @author Thomas.Diesler@jboss.com
 * @since 13-Mar-2015
 */
public final class ContextValidationHandler implements ContextCreateHandler {

    @Override
    public void setup(CamelContext camelctx) {

        // Ensure context does not contain CXF consumer endpoints
        StartupListener startupListener = new StartupListener() {
            @Override
            public void onCamelContextStarted(CamelContext camelctx, boolean alreadyStarted) throws Exception {
                if (!alreadyStarted) {
                    assertNoCxfEndpoint(camelctx);
                }
            }
        };

        try {
            camelctx.addStartupListener(startupListener);
        } catch (RuntimeException rte) {
            throw rte;
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    private void assertNoCxfEndpoint(CamelContext camelctx) {
        String cxfpackage = CxfEndpoint.class.getPackage().getName();
        for (Route route : camelctx.getRoutes()) {
            Endpoint endpoint = route.getEndpoint();
            String eppackage = endpoint.getClass().getPackage().getName();
            if (route.getConsumer() != null && eppackage.startsWith(cxfpackage)) {
                throw new UnsupportedOperationException("CXF Endpoint consumers are not allowed");
            }
        }
    }
}
