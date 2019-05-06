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

package org.wildfly.camel.utils;

import java.net.URL;
import java.util.List;

import org.apache.camel.CamelContext;
import org.apache.camel.spring.SpringCamelContext;

/**
 * A {@link CamelContext} factory utility.
 *
 * @author Thomas.Diesler@jboss.com
 * @since 19-Apr-2013
 */
public final class SpringCamelContextFactory {

    // Hide ctor
    private SpringCamelContextFactory() {
    }

    /**
     * Create a single {@link SpringCamelContext} from the given URL
     * @throws IllegalStateException if the given URL does not contain a single context definition
     */
    public static SpringCamelContext createSingleCamelContext(URL contextUrl, ClassLoader classsLoader) throws Exception {
        SpringCamelContextBootstrap bootstrap = new SpringCamelContextBootstrap(contextUrl, classsLoader);
        List<SpringCamelContext> list = bootstrap.createSpringCamelContexts();
        IllegalStateAssertion.assertEquals(1, list.size(), "Single context expected in: " + contextUrl);
        return list.get(0);
    }

    /**
     * Create a {@link SpringCamelContext} list from the given URL
     */
    public static List<SpringCamelContext> createCamelContextList(URL contextUrl, ClassLoader classsLoader) throws Exception {
        SpringCamelContextBootstrap bootstrap = new SpringCamelContextBootstrap(contextUrl, classsLoader);
        return bootstrap.createSpringCamelContexts();
    }

    /**
     * Create a {@link SpringCamelContext} list from the given bytes
     */
    public static List<SpringCamelContext> createCamelContextList(byte[] bytes, ClassLoader classsLoader) throws Exception {
        SpringCamelContextBootstrap bootstrap = new SpringCamelContextBootstrap(bytes, classsLoader);
        return bootstrap.createSpringCamelContexts();
    }
}
