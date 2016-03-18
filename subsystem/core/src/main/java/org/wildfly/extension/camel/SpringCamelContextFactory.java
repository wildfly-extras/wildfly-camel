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

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.camel.CamelContext;
import org.apache.camel.spring.SpringCamelContext;
import org.apache.camel.spring.handler.CamelNamespaceHandler;
import org.jboss.gravia.utils.IllegalStateAssertion;
import org.springframework.beans.factory.xml.NamespaceHandler;
import org.springframework.beans.factory.xml.NamespaceHandlerResolver;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * A {@link CamelContext} factory utility.
 *
 * @author Thomas.Diesler@jboss.com
 * @since 19-Apr-2013
 */
public final class SpringCamelContextFactory {

    private static final String SPRING_BEANS_SYSTEM_ID = "http://www.springframework.org/schema/beans/spring-beans.xsd";
    private static final String CAMEL_SPRING_SYSTEM_ID = "http://camel.apache.org/schema/spring/camel-spring.xsd";

    // Hide ctor
    private SpringCamelContextFactory() {
    }

    /**
     * Create a single {@link SpringCamelContext} from the given URL
     * @throws IllegalStateException if the given URL does not contain a single context definition
     */
    public static SpringCamelContext createSingleCamelContext(URL contextUrl, ClassLoader classsLoader) throws Exception {
        List<SpringCamelContext> list = createCamelContextList(new UrlResource(contextUrl), classsLoader);
        IllegalStateAssertion.assertEquals(1, list.size(), "Single context expected in: " + contextUrl);
        return list.get(0);
    }

    /**
     * Create a {@link SpringCamelContext} list from the given URL
     */
    public static List<SpringCamelContext> createCamelContextList(URL contextUrl, ClassLoader classsLoader) throws Exception {
        return createCamelContextList(new UrlResource(contextUrl), classsLoader);
    }

    /**
     * Create a {@link SpringCamelContext} list from the given bytes
     */
    public static List<SpringCamelContext> createCamelContextList(byte[] bytes, ClassLoader classsLoader) throws Exception {
        return createCamelContextList(new ByteArrayResource(bytes), classsLoader);
    }

    private static List<SpringCamelContext> createCamelContextList(Resource resource, ClassLoader classLoader) throws Exception {
        GenericApplicationContext appContext = new GenericApplicationContext();
        appContext.setClassLoader(classLoader);
        XmlBeanDefinitionReader xmlReader = new XmlBeanDefinitionReader(appContext) {
            @Override
            protected NamespaceHandlerResolver createDefaultNamespaceHandlerResolver() {
                NamespaceHandlerResolver defaultResolver = super.createDefaultNamespaceHandlerResolver();
                return new CamelNamespaceHandlerResolver(defaultResolver);
            }
        };
        xmlReader.setEntityResolver(new EntityResolver() {
            @Override
            public InputSource resolveEntity(String publicId, String systemId) throws SAXException, IOException {
                InputStream inputStream = null;
                if (CAMEL_SPRING_SYSTEM_ID.equals(systemId)) {
                    inputStream = SpringCamelContext.class.getResourceAsStream("/camel-spring.xsd");
                } else if (SPRING_BEANS_SYSTEM_ID.equals(systemId)) {
                    inputStream = XmlBeanDefinitionReader.class.getResourceAsStream("spring-beans-3.1.xsd");
                }
                InputSource result = null;
                if (inputStream != null) {
                    result = new InputSource();
                    result.setSystemId(systemId);
                    result.setByteStream(inputStream);
                }
                return result;
            }
        });
        xmlReader.loadBeanDefinitions(resource);

        SpringCamelContext.setNoStart(true);
        appContext.refresh();

        List<SpringCamelContext> result = new ArrayList<>();
        for (String name : appContext.getBeanNamesForType(SpringCamelContext.class)) {
            result.add(appContext.getBean(name, SpringCamelContext.class));
        }

        return Collections.unmodifiableList(result);
    }

    private static class CamelNamespaceHandlerResolver implements NamespaceHandlerResolver {

        private final NamespaceHandlerResolver delegate;
        private final NamespaceHandler camelHandler;

        CamelNamespaceHandlerResolver(NamespaceHandlerResolver delegate) {
            this.delegate = delegate;
            this.camelHandler = new CamelNamespaceHandler();
            this.camelHandler.init();
        }

        @Override
        public NamespaceHandler resolve(String namespaceUri) {
            if ("http://camel.apache.org/schema/spring".equals(namespaceUri)) {
                return camelHandler;
            } else {
                return delegate.resolve(namespaceUri);
            }
        }
    }
}
