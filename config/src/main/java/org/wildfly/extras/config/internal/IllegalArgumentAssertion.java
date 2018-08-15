/*
 * #%L
 * Gravia :: Resource
 * %%
 * Copyright (C) 2010 - 2014 JBoss by Red Hat
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
package org.wildfly.extras.config.internal;

/**
 * Legal argument assertions
 *
 * @author thomas.diesler@jboss.com
 * @since 27-Sep-2013
 */
public final class IllegalArgumentAssertion {

    // hide ctor
    private IllegalArgumentAssertion() {
    }

    /**
     * Throws an IllegalArgumentException when the given value is null.
     * @param value the value to assert if not null
     * @param name the name of the argument
     * @param <T> The generic type of the value to assert if not null
     * @return the value
     */
    public static <T> T assertNotNull(T value, String name) {
        if (value == null)
            throw new IllegalArgumentException("Null " + name);

        return value;
    }

    /**
     * Throws an IllegalArgumentException when the given value is not true.
     * @param value the value to assert if true
     * @param message the message to display if the value is false
     * @return the value
     */
    public static Boolean assertTrue(Boolean value, String message) {
        if (!Boolean.valueOf(value))
            throw new IllegalArgumentException(message);
        return value;
    }

    /**
     * Throws an IllegalArgumentException when the given value is not false.
     * @param value the value to assert if false
     * @param message the message to display if the value is false
     * @return the value
     */
    public static Boolean assertFalse(Boolean value, String message) {
        if (Boolean.valueOf(value))
            throw new IllegalArgumentException(message);
        return value;
    }
}
