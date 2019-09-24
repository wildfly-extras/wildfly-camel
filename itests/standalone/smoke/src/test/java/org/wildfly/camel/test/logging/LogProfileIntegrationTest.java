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

package org.wildfly.camel.test.logging;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.impl.DefaultCamelContext;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.LoggerFactory;
import org.wildfly.camel.test.common.utils.DMRUtils;
import org.wildfly.camel.test.common.utils.LogUtils;
import org.wildfly.camel.test.common.utils.ManifestBuilder;
import org.wildfly.extension.camel.CamelAware;

@CamelAware
@RunWith(Arquillian.class)
@ServerSetup({LogProfileIntegrationTest.LogSetupTask.class})
public class LogProfileIntegrationTest {

    static class LogSetupTask implements ServerSetupTask {

        public static final String LOG_PROFILE_PREFIX = "subsystem=logging/logging-profile=camel-logging-profile";

        @Override
        public void setup(ManagementClient managementClient, String s) throws Exception {
            ModelNode batchNode = DMRUtils.batchNode()
                .addStep(LOG_PROFILE_PREFIX, "add")
                .addStep(LOG_PROFILE_PREFIX + "/file-handler=camel-log-file", "add(file={path=>camel-log-test.log,relative-to=>jboss.server.log.dir})")
                .addStep(LOG_PROFILE_PREFIX + "/file-handler=camel-log-file", "change-log-level(level=DEBUG))")
                .addStep(LOG_PROFILE_PREFIX + "/logger=" + LogProfileIntegrationTest.class.getName(), "add(level=DEBUG,handlers=[handler=camel-log-file])")
                .build();
            managementClient.getControllerClient().execute(batchNode);
        }

        @Override
        public void tearDown(ManagementClient managementClient, String s) throws Exception {
            ModelNode batchNode = DMRUtils.batchNode().addStep(LOG_PROFILE_PREFIX, "remove").build();
            managementClient.getControllerClient().execute(batchNode);
        }
    }

    @Deployment
    public static JavaArchive createDeployment() {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "log-profile-tests")
            .addClass(LogUtils.class)
            .setManifest(() -> {
                ManifestBuilder builder = new ManifestBuilder();
                builder.addManifestHeader("Logging-Profile", "camel-logging-profile");
                return builder.openStream();
            });
        return archive;
    }

    @Test
    public void testWildFlyLogProfile() throws Exception {
        CamelContext camelctx = new DefaultCamelContext();
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:start")
                .log(LoggingLevel.DEBUG, LoggerFactory.getLogger(LogProfileIntegrationTest.class.getName()), "Hello ${body}");
            }
        });

        camelctx.start();
        try {
            ProducerTemplate producer = camelctx.createProducerTemplate();
            producer.requestBody("direct:start", "Kermit");
            assertLogFileContainsContent(".*LogProfileIntegrationTest.*Hello Kermit$");
        } finally {
            camelctx.close();
        }
    }

    @Test
    public void testWildFlyLogProfileGloabalLogConfig() throws Exception {
        CamelContext camelctx = new DefaultCamelContext();
        camelctx.getGlobalOptions().put(Exchange.LOG_EIP_NAME, LogProfileIntegrationTest.class.getName());

        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:start")
                .log("Goodbye ${body}");
            }
        });

        camelctx.start();
        try {
            ProducerTemplate producer = camelctx.createProducerTemplate();
            producer.requestBody("direct:start", "Kermit");
            assertLogFileContainsContent(".*LogProfileIntegrationTest.*Goodbye Kermit$");
        } finally {
            camelctx.close();
        }
    }

    private void assertLogFileContainsContent(String assertion) {
        String logDir = System.getProperty("jboss.server.log.dir");
        Path logFilePath = Paths.get(logDir, "camel-log-test.log");
        boolean logMessagePresent = LogUtils.awaitLogMessage(assertion, 5000, logFilePath);
        Assert.assertTrue("Gave up waiting to find matching log message" , logMessagePresent);
    }
}
