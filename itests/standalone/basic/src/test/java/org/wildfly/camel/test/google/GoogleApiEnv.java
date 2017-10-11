/*
 * #%L
 * Wildfly Camel :: Testsuite
 * %%
 * Copyright (C) 2013 - 2017 RedHat
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

package org.wildfly.camel.test.google;

import java.util.Arrays;

import org.apache.camel.util.IntrospectionSupport;
import org.jboss.logging.Logger;
import org.junit.Assume;

/**
 * A utility to read the {@code GOOGLE_API_*} variables from the environment and set them into a given Google API related component's configuration.
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 */
public enum GoogleApiEnv {
    GOOGLE_API_APPLICATION_NAME("applicationName"), //
    GOOGLE_API_CLIENT_ID("clientId"), //
    GOOGLE_API_CLIENT_SECRET("clientSecret"), //
    GOOGLE_API_REFRESH_TOKEN("refreshToken");

    public static void configure(Object configuration, Class<?> testClass, Logger log) {
        final GoogleApiEnv[] vals = GoogleApiEnv.values();
        for (GoogleApiEnv googleApiEnv : vals) {
            final String val = System.getenv(googleApiEnv.name());
            final boolean assumption = val != null && !val.isEmpty();
            if (!assumption) {
                final String msg = "Environment variable "+ googleApiEnv.name() +" not set - skipping "+ testClass.getSimpleName() +". You need to set all of "+  Arrays.asList(vals) +" to run it. You may want read google-api-testing.adoc in the rood directory of the current Maven module.";
                log.warn(msg);
                Assume.assumeTrue(msg, assumption);
            }
            try {
                IntrospectionSupport.setProperty(configuration, googleApiEnv.getConfigPropertyName(), val);
            } catch (Exception e) {
                throw new RuntimeException("Could not set " + configuration.getClass().getSimpleName() + "." + googleApiEnv.getConfigPropertyName(), e);
            }
        }
    }

    private String configPropertyName;

    private GoogleApiEnv(String configPropertyName) {
        this.configPropertyName = configPropertyName;
    }

    public String getConfigPropertyName() {
        return configPropertyName;
    }
}
