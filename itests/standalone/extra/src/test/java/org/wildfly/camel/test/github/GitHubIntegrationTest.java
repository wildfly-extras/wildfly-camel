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
package org.wildfly.camel.test.github;

import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.impl.DefaultCamelContext;
import org.eclipse.egit.github.core.CommitFile;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.extension.camel.CamelAware;

@CamelAware
@RunWith(Arquillian.class)
public class GitHubIntegrationTest {

    private static String GITHUB_REPO_OWNER = "wfcbot";
    private static String GITHUB_REPO_NAME = "camel-testing";

    @Deployment
    public static JavaArchive createDeployment() {
        return ShrinkWrap.create(JavaArchive.class, "camel-github-tests.jar");
    }

    @Test
    public void testGitHubComponent() throws Exception {
        String oauthKey = System.getenv("CAMEL_GITHUB_OAUTH_KEY");
        Assume.assumeNotNull("CAMEL_GITHUB_OAUTH_KEY not null", oauthKey);

        CamelContext camelctx = new DefaultCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:start")
                .toF("github:GETCOMMITFILE?oauthToken=%s&repoOwner=%s&repoName=%s", oauthKey, GITHUB_REPO_OWNER, GITHUB_REPO_NAME);
            }
        });

        camelctx.start();
        try {
            CommitFile commitFile = new CommitFile();
            commitFile.setSha("29222e8ccbb39c3570aa1cd29388e737a930a88d");

            ProducerTemplate template = camelctx.createProducerTemplate();
            String result = template.requestBody("direct:start", commitFile, String.class);
            Assert.assertEquals("Hello Kermit", result.trim());
        } finally {
            camelctx.stop();
        }
    }
}
