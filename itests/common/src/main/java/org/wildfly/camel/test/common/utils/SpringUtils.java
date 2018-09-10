/*
 * #%L
 * Wildfly Camel :: Testsuite
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

package org.wildfly.camel.test.common.utils;

import org.apache.camel.CamelContext;
import org.apache.camel.spring.SpringCamelContext;
import org.springframework.context.ApplicationContext;
import org.wildfly.camel.utils.IllegalArgumentAssertion;
import org.wildfly.camel.utils.IllegalStateAssertion;

/**
 * Collection of Spring utilities
 */
public final class SpringUtils {

    // hide ctor
    private SpringUtils() {
    }

    public static ApplicationContext getApplicationContext(CamelContext camelctx) {
        IllegalArgumentAssertion.assertTrue(camelctx instanceof SpringCamelContext, "Not an SpringCamelContext: " + camelctx);
        return ((SpringCamelContext)camelctx).getApplicationContext();
    }

    public static <T> T getMandatoryBean(CamelContext camelctx, Class<T> type, String name) {
        Object value = getApplicationContext(camelctx).getBean(name);
        IllegalStateAssertion.assertNotNull(value, "No spring bean found for name <" + name + ">");
        IllegalStateAssertion.assertTrue(type.isInstance(value), "Bean is not of type <" + type.getName() + ">");
        return type.cast(value);
    }
}
