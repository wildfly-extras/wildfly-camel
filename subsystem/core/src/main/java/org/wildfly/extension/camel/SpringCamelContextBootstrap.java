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
package org.wildfly.extension.camel;

import static org.wildfly.extension.camel.CamelLogger.LOGGER;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.camel.spring.SpringCamelContext;
import org.apache.camel.spring.handler.CamelNamespaceHandler;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.TypedStringValue;
import org.springframework.beans.factory.xml.NamespaceHandler;
import org.springframework.beans.factory.xml.NamespaceHandlerResolver;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.jndi.JndiObjectFactoryBean;
import org.wildfly.extension.camel.proxy.ProxiedAction;
import org.wildfly.extension.camel.proxy.ProxyUtils;

/**
 * SpringCamelContextBootstrap bootstraps a {@link SpringCamelContext}.
 */
public class SpringCamelContextBootstrap {

    private GenericApplicationContext applicationContext;

    /**
     * @param contextUrl The URL path to the Spring context descriptor
     * @param classLoader The ClassLoader that the Spring {@link GenericApplicationContext} should use
     */
    public SpringCamelContextBootstrap(final URL contextUrl, final ClassLoader classLoader) {
        this(new UrlResource(contextUrl), classLoader);
    }

    /**
     * @param bytes The byte array for the Spring context descriptor
     * @param classLoader The ClassLoader that the Spring {@link GenericApplicationContext} should use
     */
    public SpringCamelContextBootstrap(final byte[] bytes, final ClassLoader classLoader) {
        this(new ByteArrayResource(bytes), classLoader);
    }

    private SpringCamelContextBootstrap(final Resource resource, final ClassLoader classLoader) {
        loadBeanDefinitions(resource, classLoader);
    }

    /**
     * Initializes the Spring {@link GenericApplicationContext} and returns all instances of {@link SpringCamelContext} beans.
     *
     * Note that {@link SpringCamelContext} instances are created in the <b>stopped</b> state. Starting the {@link SpringCamelContext}
     * is left to the caller.
     *
     * @return Unmodifiable list of {@link SpringCamelContext} instances
     * @throws Exception
     */
    public List<SpringCamelContext> createSpringCamelContexts() throws Exception {
        if (applicationContext.isActive()) {
            throw new IllegalStateException("Unable to refresh Spring application context. Context is already initialized");
        }

        SpringCamelContext.setNoStart(true);
        ProxyUtils.invokeProxied(new ProxiedAction() {
            @Override
            public void run() throws Exception {
                applicationContext.refresh();
            }
        }, applicationContext.getClassLoader());
        SpringCamelContext.setNoStart(false);

        return getSpringCamelContexts();
    }

    /**
     * Returns all beans of type {@link SpringCamelContext} that are present in the Spring bean factory.
     * @return Unmodifiable list of {@link SpringCamelContext} instances
     */
    public List<SpringCamelContext> getSpringCamelContexts() {
        List<SpringCamelContext> beans = new ArrayList<>();
        for (String name : applicationContext.getBeanNamesForType(SpringCamelContext.class)) {
            beans.add(applicationContext.getBean(name, SpringCamelContext.class));
        }

        return Collections.unmodifiableList(beans);
    }

    /**
     * Gets JNDI names for all configured instances of {@link JndiObjectFactoryBean}
     *
     * Note: If this method is invoked before ApplicationContext.refresh() then any bean property
     * values containing property placeholders will not be resolved.
     *
     * @return the unmodifiable list of JNDI binding names
     */
    public List<String> getJndiNames() {
        List<String> bindings = new ArrayList<>();
        for(String beanName : applicationContext.getBeanDefinitionNames()) {
            BeanDefinition definition = applicationContext.getBeanDefinition(beanName);
            String beanClassName = definition.getBeanClassName();

            if (beanClassName != null && beanClassName.equals(JndiObjectFactoryBean.class.getName())) {
                MutablePropertyValues propertyValues = definition.getPropertyValues();
                Object jndiPropertyValue = propertyValues.get("jndiName");

                if (jndiPropertyValue == null) {
                    LOGGER.debug("Skipping JNDI binding dependency for bean: {}", beanName);
                    continue;
                }

                String jndiName = null;
                if (jndiPropertyValue instanceof String) {
                    jndiName = (String) jndiPropertyValue;
                } else if (jndiPropertyValue instanceof TypedStringValue) {
                    jndiName = ((TypedStringValue) jndiPropertyValue).getValue();
                } else {
                    LOGGER.debug("Ignoring unknown JndiObjectFactoryBean property value type {}", jndiPropertyValue.getClass().getSimpleName());
                }

                if (jndiName != null) {
                    bindings.add(jndiName);
                }
            }
        }
        return Collections.unmodifiableList(bindings);
    }

    /**
     * Gets the ClassLoader associated with the Spring {@link GenericApplicationContext}
     * @return the ClassLoader associated with the Spring {@link GenericApplicationContext}
     */
    public ClassLoader getClassLoader() {
        return applicationContext.getClassLoader();
    }

    private void loadBeanDefinitions(Resource resource, ClassLoader classLoader) {
        applicationContext = new GenericApplicationContext();
        applicationContext.setClassLoader(classLoader);
        XmlBeanDefinitionReader xmlReader = new XmlBeanDefinitionReader(applicationContext) {
            @Override
            protected NamespaceHandlerResolver createDefaultNamespaceHandlerResolver() {
                NamespaceHandlerResolver defaultResolver = super.createDefaultNamespaceHandlerResolver();
                return new SpringCamelContextBootstrap.CamelNamespaceHandlerResolver(defaultResolver);
            }
        };
        xmlReader.loadBeanDefinitions(resource);
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
