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
package org.wildfly.camel.test.jpa;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.Assert;
import org.junit.Test;
import org.wildfly.camel.test.common.FileConsumingTestSupport;
import org.wildfly.camel.test.common.http.HttpRequest;

public abstract class AbstractJPAExampleTest extends FileConsumingTestSupport {

    @Test
    public void testFileToJPARoute() throws Exception {
        HttpRequest.HttpResponse result = HttpRequest.get("http://localhost:8080/" + getContextPath() + "/customers").getResponse();
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

    abstract String getContextPath();
}
