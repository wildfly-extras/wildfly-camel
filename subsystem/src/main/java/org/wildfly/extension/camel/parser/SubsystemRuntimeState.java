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


package org.wildfly.extension.camel.parser;

import static org.wildfly.extension.camel.CamelLogger.LOGGER;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;


/**
 * The Camel subsystem runtime state.
 *
 * @author Thomas.Diesler@jboss.com
 * @since 26-Jan-2016
 */
public final class SubsystemRuntimeState  {

    private final Set<URL> endpoints = new LinkedHashSet<>();
    private URL httpHost;

    public URL getHttpHost() {
        return httpHost;
    }

    public void setHttpHost(URL httpHost) {
        this.httpHost = httpHost;
    }

    public List<URL> getEndpointURLs() {
        synchronized (endpoints) {
            return Collections.unmodifiableList(new ArrayList<>(endpoints));
        }
    }

    public void addHttpContext(String contextPath) {
        addEndpointURL(concatURL(httpHost, contextPath));
    }

    public boolean removeHttpContext(String contextPath) {
        return removeEndpointURL(concatURL(httpHost, contextPath));
    }

    public void addEndpointURL(URL endpointURL) {
        synchronized (endpoints) {
            LOGGER.info("Add Camel endpoint: {}", endpointURL);
            endpoints.add(endpointURL);
        }
    }

    public boolean removeEndpointURL(URL endpointURL) {
        synchronized (endpoints) {
            LOGGER.info("Remove Camel endpoint: {}", endpointURL);
            return endpoints.remove(endpointURL);
        }
    }

    public static URL concatURL(URL hostURL, String contextPath) {
        URL endpointURL;
        try {
            endpointURL = new URL(hostURL + "" + contextPath);
        } catch (MalformedURLException ex) {
            throw new IllegalStateException(ex);
        }
        return endpointURL;
    }
}
