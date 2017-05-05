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
package org.wildfly.camel.test.jpa;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.camel.test.common.FileConsumingTestSupport;
import org.wildfly.camel.test.common.http.HttpRequest;
import org.wildfly.camel.test.common.http.HttpRequest.HttpResponse;

@RunAsClient
@RunWith(Arquillian.class)
public class JPAExampleTest extends FileConsumingTestSupport {

    @Deployment(testable = false)
    public static WebArchive createDeployment() {
        return ShrinkWrap.createFromZipFile(WebArchive.class, new File("target/examples/example-camel-jpa.war"));
    }

    @Test
    public void testFileToJPARoute() throws Exception {
        HttpResponse result = HttpRequest.get("http://localhost:8080/example-camel-jpa/customers").getResponse();
        Assert.assertTrue(result.getBody().contains("John Doe"));
    }

    @Override
    protected String sourceFilename() {
        return "customer.xml";
    }

    @Override
    protected Path destinationPath() {
        return Paths.get(System.getProperty("jboss.home") + "/standalone/data/customers");
    }

    @Override
    protected Path processedPath() {
        return destinationPath().resolve("processed");
    }
}
