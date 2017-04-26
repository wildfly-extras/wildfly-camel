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
package org.wildfly.camel.test.elasticsearch.subA;

import javax.enterprise.inject.Disposes;
import javax.enterprise.inject.Produces;
import javax.inject.Singleton;

import org.elasticsearch.client.Client;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.node.NodeBuilder;

import java.nio.file.Path;
import java.nio.file.Paths;

public class ElasticSearchClientProducer {

    public static final Path DATA_PATH = Paths.get("target", "elasticsearch", "data");
    public static final Path HOME_PATH = Paths.get("target", "elasticsearch", "home");

    @Produces
    @Singleton
    public Client getClient() {
        Settings.Builder settings = Settings.settingsBuilder()
            .put("http.enabled", false)
            .put("path.data", DATA_PATH)
            .put("path.home", HOME_PATH);

        return NodeBuilder.nodeBuilder()
                .local(true)
                .settings(settings)
                .node()
                .client();
    }

    public void close(@Disposes Client client) {
        client.close();
    }
}
