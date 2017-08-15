/*
 * #%L
 * Wildfly Camel :: Testsuite
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

package org.wildfly.camel.test.spring;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.dmr.ModelNode;
import org.jboss.gravia.resource.ManifestBuilder;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.camel.test.common.utils.DMRUtils;
import org.wildfly.camel.test.common.utils.LogUtils;
import org.wildfly.camel.test.spring.subD.LogBean;
import org.wildfly.extension.camel.CamelAware;
import org.wildfly.extension.camel.CamelContextRegistry;

@CamelAware
@RunWith(Arquillian.class)
@ServerSetup({SpringLogIntegrationTest.LogSetupTask.class})
public class SpringLogIntegrationTest {

    private static final Path CUSTOM_LOG_FILE = Paths.get(System.getProperty("jboss.server.log.dir"), "camel-log-test.log");

    static class LogSetupTask implements ServerSetupTask {

        public static final String LOG_PROFILE_PREFIX = "subsystem=logging/logging-profile=camel-logging-profile";

        @Override
        public void setup(ManagementClient managementClient, String s) throws Exception {
            ModelNode batchNode = DMRUtils.batchNode()
                .addStep(LOG_PROFILE_PREFIX, "add")
                .addStep(LOG_PROFILE_PREFIX + "/file-handler=camel-log-file", "add(file={path=>camel-log-test.log,relative-to=>jboss.server.log.dir})")
                .addStep(LOG_PROFILE_PREFIX + "/file-handler=camel-log-file", "change-log-level(level=INFO))")
                .addStep(LOG_PROFILE_PREFIX + "/logger=" + LogBean.class.getName(), "add(level=INFO,handlers=[handler=camel-log-file])")
                .build();

            ModelNode result = managementClient.getControllerClient().execute(batchNode);
            Assert.assertEquals("success", result.get("outcome").asString());
        }

        @Override
        public void tearDown(ManagementClient managementClient, String s) throws Exception {
            ModelNode batchNode = DMRUtils.batchNode().addStep(LOG_PROFILE_PREFIX, "remove").build();
            managementClient.getControllerClient().execute(batchNode);
        }
    }

    @ArquillianResource
    CamelContextRegistry contextRegistry;

    @Deployment
    public static JavaArchive createDeployment() {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "spring-log-tests")
            .addClasses(LogBean.class, LogUtils.class)
            .setManifest(() -> {
                ManifestBuilder builder = new ManifestBuilder();
                builder.addManifestHeader("Logging-Profile", "camel-logging-profile");
                return builder.openStream();
            })
            .addAsResource("spring/logging-camel-context.xml", "logging-camel-context.xml");
        return archive;
    }

    @Test
    public void testBeanLogger() throws Exception {
        CamelContext camelctx = contextRegistry.getCamelContext("spring-context");
        ProducerTemplate producer = camelctx.createProducerTemplate();
        producer.requestBody("direct:start", "Hello Kermit");
        assertLogFileContainsContent(".*" + LogBean.class.getName() + ".*Hello Kermit$");
    }

    private void assertLogFileContainsContent(String assertion) throws IOException {
        boolean logMessagePresent = LogUtils.awaitLogMessage(assertion, 5000, CUSTOM_LOG_FILE);
        Assert.assertTrue("Gave up waiting to find matching log message", logMessagePresent);
    }
}
