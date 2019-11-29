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
package org.wildfly.extension.camel.deployment;

import java.net.URL;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.jboss.as.server.deployment.AttachmentKey;

/**
 * Immutable settings associated with a deployment.
 *
 * Created by chirino on 2/23/15.
 *
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 */
public final class CamelDeploymentSettings {

    public static final AttachmentKey<CamelDeploymentSettings> ATTACHMENT_KEY = AttachmentKey.create(CamelDeploymentSettings.class);
    public static final AttachmentKey<CamelDeploymentSettings.Builder> BUILDER_ATTACHMENT_KEY = AttachmentKey.create(CamelDeploymentSettings.Builder.class);
    private static final Map<String, CamelDeploymentSettings> deploymentSettingsMap = new HashMap<>();

    private final List<String> dependencies;
    private final List<URL> camelContextUrls;
    private final boolean enabled;

    private CamelDeploymentSettings(List<String> dependencies, List<URL> camelContextUrls, boolean enabled) {
        this.dependencies = dependencies;
        this.camelContextUrls = camelContextUrls;
        this.enabled = enabled;
    }

    public static CamelDeploymentSettings get(String name) {
        synchronized (deploymentSettingsMap) {
            return deploymentSettingsMap.get(name);
        }
    }

    public static void remove(String deploymentName) {
        synchronized (deploymentSettingsMap) {
            deploymentSettingsMap.remove(deploymentName);
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public List<String> getModuleDependencies() {
        return dependencies;
    }

    public List<URL> getCamelContextUrls() {
        return camelContextUrls;
    }

    public static class Builder {
        private boolean camelActivationAnnotationPresent;
        private List<URL> camelContextUrls = new ArrayList<>();
        private List<Map.Entry<CamelDeploymentSettings.Builder, Consumer<CamelDeploymentSettings>>> children = new ArrayList<>();
        private List<String> dependencies = new ArrayList<>();
        private String deploymentName;
        private boolean deploymentValid;
        private boolean disabledByJbossAll;
        private final Object lock = new Object();

        /**
         * Note that this method calls {@link #build()} for every {@link #children} {@link Builder}, then it calls the
         * associated {@link Consumer} using the newly built child {@link CamelDeploymentSettings} and finally it also
         * registers the returned {@link CamelDeploymentSettings} to
         * {@link CamelDeploymentSettings#deploymentSettingsMap}.
         *
         * @return a new immutable {@link CamelDeploymentSettings}
         */
        public CamelDeploymentSettings build() {
            final boolean enabled;
            final List<String> deps;
            final List<URL> urls;
            synchronized (lock) {
                enabled = isEnabled();
                for (Map.Entry<CamelDeploymentSettings.Builder, Consumer<CamelDeploymentSettings>> e : children) {
                    final CamelDeploymentSettings childDepSettings = e.getKey().build();
                    e.getValue().accept(childDepSettings);
                }
                this.children = null;
                deps = Collections.unmodifiableList(this.dependencies);
                this.dependencies = null;
                urls = Collections.unmodifiableList(this.camelContextUrls);
                this.camelContextUrls = null;
            }
            final CamelDeploymentSettings result = new CamelDeploymentSettings(deps, urls, enabled);
            synchronized (deploymentSettingsMap) {
                deploymentSettingsMap.put(deploymentName, result);
            }
            return result;
        }

        public Builder camelActivationAnnotationPresent(boolean camelActivationAnnotationPresent) {
            synchronized (lock) {
                this.camelActivationAnnotationPresent = camelActivationAnnotationPresent;
            }
            return this;
        }

        public Builder camelContextUrl(URL camelContextUrl) {
            synchronized (lock) {
                this.camelContextUrls.add(camelContextUrl);
            }
            return this;
        }

        public Builder child(Builder depSettingsBuilder, Consumer<CamelDeploymentSettings> consumer) {
            synchronized (lock) {
                this.children.add(new AbstractMap.SimpleImmutableEntry<>(depSettingsBuilder, consumer));
            }
            return this;
        }

        public Builder dependency(String dependency) {
            synchronized (lock) {
                this.dependencies.add(dependency);
                this.disabledByJbossAll = false;
            }
            return this;
        }

        public Builder deploymentName(String deploymentName) {
            synchronized (lock) {
                this.deploymentName = deploymentName;
            }
            return this;
        }

        public Builder deploymentValid(boolean deploymentValid) {
            synchronized (lock) {
                this.deploymentValid = deploymentValid;
            }
            return this;
        }

        public Builder disabledByJbossAll(boolean disabledByJbossAll) {
            synchronized (lock) {
                this.disabledByJbossAll = disabledByJbossAll;
            }
            return this;
        }

        public boolean isDeploymentValid() {
            synchronized (lock) {
                return deploymentValid;
            }
        }

        public boolean isDisabledByJbossAll() {
            synchronized (lock) {
                return disabledByJbossAll;
            }
        }

        /**
         * Always call under {@link #lock}.
         *
         * @return {@code true} if Camel is enable for the associated deployment; {@code false} otherwise.
         */
        private boolean isEnabled() {
            // Disabling camel in jboss-all.xml takes precedence over other enablement criteria
            if (disabledByJbossAll) {
                return false;
            }

            // Verify that we have a valid deployment before performing other enablement checks
            if (deploymentValid) {
                if (!this.camelContextUrls.isEmpty()) {
                    return true;
                }

                // Valid child implies valid parent
                for (Map.Entry<CamelDeploymentSettings.Builder, Consumer<CamelDeploymentSettings>> e : this.children) {
                    if (e.getKey().isEnabled()) {
                        return true;
                    }
                }

                // Camel activation annotations are present
                if (camelActivationAnnotationPresent) {
                    return true;
                }

                // Declaration of individual camel components to enable in jboss-all.xml
                if (!dependencies.isEmpty()) {
                    return true;
                }
            }

            return false;
        }
    }
}
