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
package org.wildfly.camel.test.mail;

import java.io.File;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.runner.RunWith;
import org.wildfly.extension.camel.CamelAware;

@RunWith(Arquillian.class)
@ServerSetup({ AbstractMailExampleTest.MailSessionSetupTask.class })
@CamelAware
public class MailSpringExampleTest extends AbstractMailExampleTest {

    private static final String CONTEXT_PATH = "example-camel-mail-spring";
    private static final String EXAMPLE_CAMEL_MAIL_WAR = CONTEXT_PATH + ".war";

    @Deployment(managed = false, testable = false, name = EXAMPLE_CAMEL_MAIL_WAR)
    public static WebArchive createCamelMailDeployment() {
        return ShrinkWrap.createFromZipFile(WebArchive.class, new File("target/examples/" + EXAMPLE_CAMEL_MAIL_WAR));
    }

    @Override
    String getContextPath() {
        return CONTEXT_PATH;
    }

    @Override
    String getDeploymentName() {
        return EXAMPLE_CAMEL_MAIL_WAR;
    }
}
