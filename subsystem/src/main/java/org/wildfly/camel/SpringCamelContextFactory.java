/*
 * #%L
 * Wildfly Camel Subsystem
 * %%
 * Copyright (C) 2013 JBoss by Red Hat
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as 
 * published by the Free Software Foundation, either version 2.1 of the 
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public 
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-2.1.html>.
 * #L%
 */

package org.wildfly.camel;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import org.apache.camel.CamelContext;
import org.apache.camel.spring.SpringCamelContext;
import org.apache.camel.spring.handler.CamelNamespaceHandler;
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
     * Create a {@link SpringCamelContext} from the given URL
     */
    public static CamelContext createSpringCamelContext(URL contextUrl, ClassLoader classsLoader) throws Exception {
        return createSpringCamelContext(new UrlResource(contextUrl), classsLoader);
    }

    /**
     * Create a {@link SpringCamelContext} from the given bytes
     */
    public static CamelContext createSpringCamelContext(byte[] bytes, ClassLoader classsLoader) throws Exception {
        return createSpringCamelContext(new ByteArrayResource(bytes), classsLoader);
    }

    private static  CamelContext createSpringCamelContext(Resource resource, ClassLoader classLoader) throws Exception {
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
        appContext.refresh();
        return SpringCamelContext.springCamelContext(appContext);
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
