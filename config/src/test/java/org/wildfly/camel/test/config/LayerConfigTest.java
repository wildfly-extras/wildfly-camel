/*
 * #%L
 * Fuse EAP :: Config
 * %%
 * Copyright (C) 2015 RedHat
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
package org.wildfly.camel.test.config;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.wildfly.extras.config.ConfigContext;
import org.wildfly.extras.config.ConfigPlugin;
import org.wildfly.extras.config.ConfigSupport;
import org.wildfly.extras.config.LayerConfig;
import org.wildfly.extras.config.LayerConfig.Type;

public class LayerConfigTest {

    @Test
    public void testApplyLayerConfigChange() throws Exception {
        ConfigPlugin plugin = new ConfigPlugin () {

            @Override
            public String getConfigName() {
                return "foo";
            }

            @Override
            public List<LayerConfig> getLayerConfigs() {
                return Arrays.asList(
                        new LayerConfig("fuse_6.2.1", Type.INSTALLING, -10),
                        new LayerConfig("soa_6.2.1", Type.INSTALLING, -10),
                        new LayerConfig("brms_6.2.1", Type.OPTIONAL, -11)
                );
            }

            @Override
            public boolean applyStandaloneConfigChange(ConfigContext context, boolean enable) {
                return false;
            }

            @Override
            public boolean applyDomainConfigChange(ConfigContext context, boolean enable) {
                return false;
            }
        };

        // Install/Uninstall scenario.
        List<String> layers;
        layers = ConfigSupport.applyLayerChanges(plugin, list("foo", "example"), true);
        Assert.assertEquals(list("fuse_6.2.1", "soa_6.2.1", "foo", "example"), layers);
        layers = ConfigSupport.applyLayerChanges(plugin, layers, false);
        Assert.assertEquals(list("foo", "example"), layers);

        // Upgrade/Uninstall scenario.
        layers = list("foo", "fuse_6.1", "soa", "example");
        layers = ConfigSupport.applyLayerChanges(plugin, layers, true);
        Assert.assertEquals(list("fuse_6.2.1", "soa_6.2.1", "foo", "example"), layers);
        layers = ConfigSupport.applyLayerChanges(plugin, layers, false);
        Assert.assertEquals(list("foo", "example"), layers);

        // Another Upgrade/Uninstall scenario.
        layers = list("foo", "fuse", "example");
        layers = ConfigSupport.applyLayerChanges(plugin, layers, true);
        Assert.assertEquals(list("fuse_6.2.1", "soa_6.2.1", "foo", "example"), layers);
        layers = ConfigSupport.applyLayerChanges(plugin, layers, false);
        Assert.assertEquals(list("foo", "example"), layers);

    }

    @Test
    public void testApplyLayerConfigChange2() throws Exception {
        ConfigPlugin plugin = new ConfigPlugin() {

            @Override
            public String getConfigName() {
                return "foo";
            }

            @Override
            public List<LayerConfig> getLayerConfigs() {
                return Arrays.asList(
                        new LayerConfig("fuse_6.2.1", Type.REQUIRED, -10),
                        new LayerConfig("soa_6.2.1", Type.REQUIRED, -10),
                        new LayerConfig("brms_6.2.1", Type.INSTALLING, -9)
                );
            }

            @Override
            public boolean applyStandaloneConfigChange(ConfigContext context, boolean enable) {
                return false;
            }

            @Override
            public boolean applyDomainConfigChange(ConfigContext context, boolean enable) {
                return false;
            }
        };

        // Install/Uninstall scenario.
        List<String> layers;
        try {
            layers = list("foo", "example");
            ConfigSupport.applyLayerChanges(plugin, layers, true);
            Assert.fail("Expecting exception since required modules are not installed");
        } catch (Exception e) {
        }

        layers = list("fuse", "soa");
        layers = ConfigSupport.applyLayerChanges(plugin, layers, true);
        Assert.assertEquals(list("fuse", "soa", "brms_6.2.1"), layers);
    }

    private ArrayList<String> list(String... args) {
        return new ArrayList<>(Arrays.asList(args));
    }
}
