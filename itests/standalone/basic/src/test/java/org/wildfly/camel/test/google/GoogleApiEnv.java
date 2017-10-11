package org.wildfly.camel.test.google;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public enum GoogleApiEnv {
    GOOGLE_API_APPLICATION_NAME("applicationName"), //
    GOOGLE_API_CLIENT_ID("clientId"), //
    GOOGLE_API_CLIENT_SECRET("clientSecret"), //
    GOOGLE_API_REFRESH_TOKEN("refreshToken");
    public static Map<String, Object> byConfigPropertyName() {
        Map<String, Object> result = new HashMap<>();
        for (GoogleApiEnv googleApiEnv : GoogleApiEnv.values()) {
            final Object val = System.getenv(googleApiEnv.name());
            if (val == null || "".equals(val)) {
                return Collections.emptyMap();
            }
            result.put(googleApiEnv.getConfigPropertyName(), val);
        }
        return Collections.unmodifiableMap(result);
    }

    private String configPropertyName;

    private GoogleApiEnv(String configPropertyName) {
        this.configPropertyName = configPropertyName;
    }

    public String getConfigPropertyName() {
        return configPropertyName;
    }
}
