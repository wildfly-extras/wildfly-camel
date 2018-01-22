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
package org.wildfly.camel.test.elasticsearch5.subA;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;

import javax.enterprise.inject.Disposes;
import javax.enterprise.inject.Produces;
import javax.inject.Singleton;

import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.node.InternalSettingsPreparer;
import org.elasticsearch.node.Node;
import org.elasticsearch.plugins.Plugin;
import org.elasticsearch.transport.Netty4Plugin;
import org.wildfly.camel.test.common.utils.AvailablePortFinder;
import org.wildfly.camel.test.common.utils.FileUtils;

public class ElasticsearchNodeProducer {

    public static final int ES_TRANSPORT_PORT = AvailablePortFinder.getNextAvailable();

    public static final Path DATA_PATH = Paths.get("target", "elasticsearch5", "data");
    public static final Path HOME_PATH = Paths.get("target", "elasticsearch5", "home");

    @Produces
    @Singleton
    @SuppressWarnings("resource")
    public Node getElasticsearchNode() throws Exception {

        FileUtils.deleteDirectory(DATA_PATH);

        class PluginConfigurableNode extends Node {
            PluginConfigurableNode(Settings settings, Collection<Class<? extends Plugin>> classpathPlugins) {
                super(InternalSettingsPreparer.prepareEnvironment(settings, null), classpathPlugins);
            }
        }

        // create an embedded node to resume
        return new PluginConfigurableNode(Settings.builder()
            .put("http.enabled", true)
            .put("path.data", DATA_PATH)
            .put("path.home", HOME_PATH)
            .put("transport.profiles.default.port", ES_TRANSPORT_PORT)
            .build(), Arrays.asList(Netty4Plugin.class)).start();
    }

    public void close(@Disposes Node node) throws IOException {
        node.close();
    }
}
