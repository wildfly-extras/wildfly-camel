/*
 * #%L
 * Fabric8 :: SPI
 * %%
 * Copyright (C) 2014 Red Hat
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
 * Legal state assertions
 *
 * @author thomas.diesler@jboss.com
 * @since 18-Apr-2014
 */
public final class IllegalStateAssertion {

    // hide ctor
    private IllegalStateAssertion() {
    }

    /**
     * Throws an IllegalStateException when the given value is not null.
     * @param value the value to assert if null
     * @param message the message to display if value is not null
     * @param <T> The generic type of the value to assert if null
     * @return the value
     */
    public static <T> T assertNull(T value, String message) {
        if (value != null)
            throw new IllegalStateException(message);
        return value;
    }

    /**
     * Throws an IllegalStateException when the given value is null.
     * @param value the value to assert if not null
     * @param message the message to display if value is null
     * @param <T> The generic type of the value to assert if not null
     * @return the value
     */
    public static <T> T assertNotNull(T value, String message) {
        if (value == null)
            throw new IllegalStateException(message);
        return value;
    }

    /**
     * Throws an IllegalStateException when the given value is not true.
     * @param value the value to assert if true
     * @param message the message to display if the value is false
     * @return the value
     */
    public static Boolean assertTrue(Boolean value, String message) {
        if (!Boolean.valueOf(value))
            throw new IllegalStateException(message);

        return value;
    }

    /**
     * Throws an IllegalStateException when the given value is not false.
     * @param value the value to assert if false
     * @param message the message to display if the value is false
     * @return the value
     */
    public static Boolean assertFalse(Boolean value, String message) {
        if (Boolean.valueOf(value))
            throw new IllegalStateException(message);
        return value;
    }

    /**
     * Throws an IllegalStateException when the given values are not equal.
     * @param exp The expected value
     * @param was The actual value
     * @param message The message to display if the compared values are not equal
     * @param <T> The generic type of the expected value
     * @param <T> The generic type of the actual value
     * @return The actual value
     */
    public static <T> T assertEquals(T exp, T was, String message) {
        assertNotNull(exp, message);
        assertNotNull(was, message);
        assertTrue(exp.equals(was), message);
        return was;
    }

    /**
     * Throws an IllegalStateException when the given values are not equal.
     * @param exp The expected value
     * @param was The actual value
     * @param message The message to display if the compared values are not equal
     * @param <T> The generic type of the expected value
     * @param <T> The generic type of the actual value
     * @return The actual value
     */
    public static <T> T assertSame(T exp, T was, String message) {
        assertNotNull(exp, message);
        assertNotNull(was, message);
        assertTrue(exp == was, message);
        return was;
    }
}
