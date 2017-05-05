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
package org.wildfly.camel.test.jms;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.runner.RunWith;

@RunAsClient
@RunWith(Arquillian.class)
@ServerSetup({ AbstractJMSExampleTest.JmsQueueSetup.class })
public class JMSExampleTest extends AbstractJMSExampleTest {

    private static final String CONTEXT_PATH = "example-camel-jms";
    private static final String EXAMPLE_CAMEL_JMS_WAR = CONTEXT_PATH + ".war";

    @Deployment(testable = false)
    public static WebArchive createDeployment() {
        return ShrinkWrap.createFromZipFile(WebArchive.class, new File("target/examples/" + EXAMPLE_CAMEL_JMS_WAR));
    }

    @Override
    protected String sourceFilename() {
        return "order.xml";
    }

    @Override
    protected Path destinationPath() {
        return Paths.get(System.getProperty("jboss.home") + "/standalone/data/orders");
    }

    @Override
    protected Path processedPath() {
        return destinationPath().resolve("processed/UK");
    }

    @Override
    String getContextPath() {
        return CONTEXT_PATH;
    }

    @Override
    String getExpectedResponseText() {
        return "UK: 1";
    }
}
