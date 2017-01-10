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
package org.wildfly.camel.test.git;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.git.GitConstants;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.impl.DefaultCamelContext;
import org.eclipse.jgit.revwalk.RevCommit;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.extension.camel.CamelAware;

@CamelAware
@RunWith(Arquillian.class)
public class GitIntegrationTest {

    private static final String GIT_FILE = "myFile.txt";
    private static final Path GIT_REPO = Paths.get("target/repository");
    private static final Path GIT_REPO_FILE = GIT_REPO.resolve(GIT_FILE);

    @Deployment
    public static JavaArchive createDeployment() {
        return ShrinkWrap.create(JavaArchive.class, "camel-git-tests.jar");
    }

    @Test
    public void testGitEndpoint() throws Exception {
        CamelContext camelctx = new DefaultCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("git://" + GIT_REPO + "?type=commit")
                .to("mock:end");
            }
        });

        MockEndpoint mockEndpoint = camelctx.getEndpoint("mock:end", MockEndpoint.class);
        mockEndpoint.expectedMessageCount(1);

        camelctx.start();
        try {
            String endpointURI = "git://" + GIT_REPO;
            ProducerTemplate template = camelctx.createProducerTemplate();

            // Init git repository
            template.sendBody(endpointURI + "?operation=init", null);

            // Add & stage content
            Files.createFile(GIT_REPO_FILE);
            template.sendBodyAndHeader(endpointURI + "?operation=add", null,
                GitConstants.GIT_FILE_NAME, GIT_FILE);

            // Commit
            template.sendBodyAndHeader(endpointURI + "?operation=commit", "",
                GitConstants.GIT_COMMIT_MESSAGE, "Hello Kermit");

            mockEndpoint.assertIsSatisfied();

            Exchange exchange = mockEndpoint.getExchanges().get(0);
            RevCommit commit = exchange.getOut().getBody(RevCommit.class);
            Assert.assertEquals("Hello Kermit", commit.getFullMessage());
        } finally {
            camelctx.stop();
        }
    }
}
