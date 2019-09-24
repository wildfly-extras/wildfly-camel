/*
 * #%L
 * Wildfly Camel :: Testsuite
 * %%
 * Copyright (C) 2013 - 2018 RedHat
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
package org.wildfly.camel.test.elasticsearch.subA;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.function.Supplier;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;

import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.env.Environment;
import org.elasticsearch.node.InternalSettingsPreparer;
import org.elasticsearch.node.Node;
import org.elasticsearch.node.NodeValidationException;
import org.elasticsearch.plugins.Plugin;
import org.elasticsearch.transport.Netty4Plugin;
import org.wildfly.camel.test.common.utils.AvailablePortFinder;
import org.wildfly.extension.camel.CamelAware;

@CamelAware
@WebServlet(name = "ElasticsearchServerServlet", loadOnStartup = 1)
@SuppressWarnings("serial")
public class ElasticsearchServerServlet extends HttpServlet {

    public static final Path DATA_PATH = Paths.get("target", "elasticsearch", "data");
    public static final Path HOME_PATH = Paths.get("target", "elasticsearch", "home");

    private ElasticsearchNode node;

    @Override
    public void init() throws ServletException {
        System.setProperty("es.set.netty.runtime.available.processors", "false");

        Settings settings = Settings.builder()
            .put("http.port", AvailablePortFinder.getAndStoreNextAvailable("es-port.txt"))
            .put("http.cors.enabled", true)
            .put("http.cors.allow-origin", "*")
            .put("path.data", DATA_PATH)
            .put("path.home", HOME_PATH)
            .build();

        List<Class<? extends Plugin>> plugins = new ArrayList<>();
        plugins.add(Netty4Plugin.class);

        node = new ElasticsearchNode(settings, plugins);
        try {
            node.start();
        } catch (NodeValidationException e) {
            throw new ServletException(e);
        }
    }

    @Override
    public void destroy() {
        System.clearProperty("es.set.netty.runtime.available.processors");

        if (node != null) {
            try {
                node.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static final class ElasticsearchNode extends Node {

        public ElasticsearchNode(Settings preparedSettings, Collection<Class<? extends Plugin>> classpathPlugins) {
            super(prepareEnvironment(preparedSettings), classpathPlugins, true);
        }

        public String getPort() {
            return getEnvironment().settings().get("http.port");
        }

        private static Environment prepareEnvironment(Settings preparedSettings) {
            Environment env = InternalSettingsPreparer.prepareEnvironment(preparedSettings, new HashMap<>(), null, defaultNodeName());
            return env;
        }

        private static Supplier<String> defaultNodeName() {
            Supplier<String> supplier = new Supplier<String>() {
                public String get() {
                    return "someNode";
                }
            };
            return supplier;
        }
    }
}
