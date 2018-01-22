/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.wildfly.camel.test.jcr;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;

import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;

import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.jcr.JcrConstants;
import org.apache.jackrabbit.core.TransientRepository;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.gravia.resource.ManifestBuilder;
import org.jboss.gravia.utils.IOUtils;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.camel.test.common.utils.FileUtils;
import org.wildfly.extension.camel.CamelAware;
import org.wildfly.extension.camel.WildFlyCamelContext;

@CamelAware
@RunWith(Arquillian.class)
public class JcrIntegrationTest {

    static final Path REPO_PATH = Paths.get("target/repository-simple-security");

    private Repository repository;

    @Deployment
    public static JavaArchive createDeployment() {
        JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "camel-jcr-tests.jar");
        archive.addAsResource("jcr/repository-simple-security.xml");
        archive.addClasses(FileUtils.class, IOUtils.class);
        archive.setManifest(() -> {
            ManifestBuilder builder = new ManifestBuilder();
            builder.addManifestHeader("Dependencies", "org.apache.jackrabbit");
            return builder.openStream();
        });
        return archive;
    }

    @Before
    public void before() throws Exception {
        FileUtils.deleteDirectory(REPO_PATH);
        REPO_PATH.toFile().mkdirs();

        File configFile = REPO_PATH.resolve("repository-simple-security.xml").toFile();
        InputStream input = getClass().getClassLoader().getResourceAsStream("jcr/repository-simple-security.xml");
        try (OutputStream output = new FileOutputStream(configFile)) {
            IOUtils.copyStream(input, output);
        }

        repository = new TransientRepository(configFile, REPO_PATH.toFile());
    }

    @Test
    public void testJcrProducer() throws Exception {

        WildFlyCamelContext camelctx = new WildFlyCamelContext();
        camelctx.getNamingContext().bind("repository", repository);

        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                getContext().setUseBreadcrumb(false);
                from("direct:a").to("jcr://user:pass@repository/home/test");
            }
        });

        camelctx.start();
        try {
            String content = "<hello>world!</hello>";
            HashMap<String, Object> headers = new HashMap<>();
            headers.put(JcrConstants.JCR_NODE_NAME, "node");
            headers.put("my.contents.property", content);
            ProducerTemplate template = camelctx.createProducerTemplate();
            String result = template.requestBodyAndHeaders("direct:a", null, headers, String.class);
            Assert.assertNotNull(result);
            Session session = openSession();
            try {
                Node node = session.getNodeByIdentifier(result);
                Assert.assertEquals("/home/test/node", node.getPath());
                Assert.assertEquals(content, node.getProperty("my.contents.property").getString());
            } finally {
                session.logout();
            }
        } finally {
            camelctx.stop();
        }
    }

    private Session openSession() throws RepositoryException {
        return repository.login(new SimpleCredentials("user", "pass".toCharArray()));
    }
}
