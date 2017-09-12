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

import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.camel.test.common.utils.LogUtils;
import org.wildfly.extension.camel.CamelAware;
import org.wildfly.extension.camel.CamelContextRegistry;

@CamelAware
@RunWith(Arquillian.class)
public class SpringLogIntegrationTest {

    private static final String LOG_MESSAGE = String.format("Hello from %s", SpringLogIntegrationTest.class.getName());
    private static final String LOG_ENDPOINT_REGEX = String.format(".*org.wildfly.camel.test.spring.*%s\\]$", LOG_MESSAGE);
    private static final String LOG_DSL_REGEX = String.format(".*org.wildfly.camel.test.spring.*%s$", LOG_MESSAGE);

    @ArquillianResource
    CamelContextRegistry contextRegistry;

    @Deployment
    public static JavaArchive createDeployment() {
        final JavaArchive archive = ShrinkWrap.create(JavaArchive.class, "spring-log-tests")
            .addClass(LogUtils.class)
            .addAsResource("logging/loggingA-camel-context.xml", "logging-camel-context.xml");
        return archive;
    }

    @Test
    public void testCamelSpringLogging() throws Exception {
        CamelContext camelctx = contextRegistry.getCamelContext("spring-logging-context-a");
        Assert.assertNotNull("spring-logging-context-a is null", camelctx);

        ProducerTemplate producer = camelctx.createProducerTemplate();
        producer.requestBody("direct:log-endpoint", LOG_MESSAGE);
        assertLogFileContainsContent(LOG_ENDPOINT_REGEX);

        producer.requestBody("direct:log-dsl", LOG_MESSAGE);
        assertLogFileContainsContent(LOG_DSL_REGEX);
    }

    private void assertLogFileContainsContent(String assertion) {
        boolean logMessagePresent = LogUtils.awaitLogMessage(assertion, 5000);
        Assert.assertTrue("Gave up waiting to find matching log message", logMessagePresent);
    }
}
