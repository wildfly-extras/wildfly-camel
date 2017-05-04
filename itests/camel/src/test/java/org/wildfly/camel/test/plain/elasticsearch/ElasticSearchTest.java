/*
 * #%L
 * Wildfly Camel :: Testsuite
 * %%
 * Copyright (C) 2013 - 2015 RedHat
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
package org.wildfly.camel.test.plain.elasticsearch;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.Map;

import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.elasticsearch.ElasticsearchComponent;
import org.apache.camel.component.elasticsearch.ElasticsearchConstants;
import org.apache.camel.impl.DefaultCamelContext;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.node.Node;
import org.elasticsearch.node.NodeBuilder;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class ElasticSearchTest {

    private static final Path DATA_PATH = Paths.get("target", "elasticsearch", "data");
    private static final Path HOME_PATH = Paths.get("target", "elasticsearch", "home");

    private Node node;

    @Before
    public void before() throws IOException {
        deleteDataDirectory();
        node = NodeBuilder.nodeBuilder().local(true)
                .settings(Settings.settingsBuilder().put("http.enabled", false).put("path.data", DATA_PATH).put("path.home", HOME_PATH)).node();
    }

    @Test
    public void testIndexContentUsingHeaders() throws Exception {

        CamelContext camelctx = new DefaultCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:start").to("elasticsearch://local");
            }
        });

        camelctx.getComponent("elasticsearch", ElasticsearchComponent.class).setClient(node.client());

        camelctx.start();
        try {
            Map<String, String> indexedData = new HashMap<>();
            indexedData.put("content", "test");

            Map<String, Object> headers = new HashMap<>();
            headers.put(ElasticsearchConstants.PARAM_OPERATION, ElasticsearchConstants.OPERATION_INDEX);
            headers.put(ElasticsearchConstants.PARAM_INDEX_NAME, "twitter");
            headers.put(ElasticsearchConstants.PARAM_INDEX_TYPE, "tweet");

            ProducerTemplate template = camelctx.createProducerTemplate();

            String indexId = template.requestBodyAndHeaders("direct:start", indexedData, headers, String.class);
            Assert.assertNotNull("Index id should not be null", indexId);
        } finally {
            camelctx.stop();
        }
    }

    private static void deleteDataDirectory() throws IOException {
        // ES component currently has no method of configuring the data dir for a local server, hence manual cleanup
        if (DATA_PATH.toFile().isDirectory()) {
            Files.walkFileTree(DATA_PATH, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Files.delete(file);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exception) throws IOException {
                    exception.printStackTrace();
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exception) throws IOException {
                    if (exception == null) {
                        Files.delete(dir);
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        }
    }
}
