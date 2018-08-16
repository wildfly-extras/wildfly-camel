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
package org.wildfly.camel.test.cxf.rs;

import java.io.InputStream;

import javax.ws.rs.core.MediaType;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.gravia.resource.ManifestBuilder;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.camel.test.common.types.GreetingService;
import org.wildfly.extension.camel.CamelAware;

@CamelAware
@RunWith(Arquillian.class)
public class CXFRSUndeployIntegrationTest {
    private static final String CXFRSUndeployIntegrationTest_WAR = "CXFRSUndeployIntegrationTest.war";
    private static final String GREET_URL = "http://localhost:8080/cxfrestful/greet/hello/Kermit";

    @ArquillianResource
    Deployer deployer;

    @Deployment(name = CXFRSUndeployIntegrationTest_WAR, managed = false, testable = false)
    public static WebArchive deployment() {
        final WebArchive archive = ShrinkWrap.create(WebArchive.class, "cxfrs-consumer-tests.war");
        archive.addClasses(GreetingService.class);
        archive.addAsResource("cxf/spring/cxfrs-consumer-camel-context.xml", "cxfrs-consumer-camel-context.xml");
        archive.setManifest(new Asset() {
            @Override
            public InputStream openStream() {
                ManifestBuilder builder = new ManifestBuilder();
                builder.addManifestHeader("Dependencies", "org.jboss.resteasy.resteasy-jaxrs");
                return builder.openStream();
            }
        });
        return archive;
    }

    @Test
    public void testCXFConsumer() throws Exception {
        try (CloseableHttpClient httpclient = HttpClients.createDefault()) {
            HttpGet request = new HttpGet(GREET_URL);
            request.setHeader("Content-Type", MediaType.APPLICATION_JSON);
            try (CloseableHttpResponse response = httpclient.execute(request)) {
                final int actualCode = response.getStatusLine().getStatusCode();
                Assert.assertEquals(404, actualCode);
            }
            deployer.deploy(CXFRSUndeployIntegrationTest_WAR);
            try {
                try (CloseableHttpResponse response = httpclient.execute(request)) {
                    final int actualCode = response.getStatusLine().getStatusCode();
                    Assert.assertEquals(200, actualCode);
                }
            } finally {
                deployer.undeploy(CXFRSUndeployIntegrationTest_WAR);
            }
            try (CloseableHttpResponse response = httpclient.execute(request)) {
                final int actualCode = response.getStatusLine().getStatusCode();
                Assert.assertEquals(404, actualCode);
            }
        }
    }

}
