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

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;

import org.apache.camel.CamelContext;
import org.apache.camel.spi.ModelJAXBContextFactory;
import org.wildfly.extension.camel.ContextCreateHandler;

/**
 * A {@link ContextCreateHandler} which wraps {@link ModelJAXBContextFactory} in Camel context.
 * TCCL is needed when initializing JAXB 2.2.5 DatatypeFactory. Otherwise JBoss-Modules JAXP factories
 * can not be found.
 *
 * @author ggrzybek@redhat.com
 * @since 08-May-2015
 */
public final class ModelJAXBContextFactoryWrapperHandler implements ContextCreateHandler {

    @Override
    public void setup(CamelContext camelctx) {

        final ModelJAXBContextFactory factory = camelctx.getModelJAXBContextFactory();
        camelctx.setModelJAXBContextFactory(new ModelJAXBContextFactory() {
            @Override
            public JAXBContext newJAXBContext() throws JAXBException {
                ClassLoader tccl = Thread.currentThread().getContextClassLoader();
                if (tccl == null) {
                    Thread.currentThread().setContextClassLoader(ModelJAXBContextFactoryWrapperHandler.class.getClassLoader());
                }
                try {
                    return factory.newJAXBContext();
                } finally {
                    Thread.currentThread().setContextClassLoader(tccl);
                }
            }
        });
    }
}
