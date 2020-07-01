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

package org.wildfly.camel.test.docker.servlet;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.camel.test.common.http.HttpRequest;
import org.wildfly.camel.test.common.http.HttpRequest.HttpResponse;
import org.wildfly.camel.test.common.utils.TestUtils;
import org.wildfly.camel.test.docker.servlet.subA.SimpleServlet;

@RunAsClient
@RunWith(Arquillian.class)
public class SimpleServletTest {

    @Deployment(testable = false)
    public static WebArchive deployment() {
        final WebArchive archive = ShrinkWrap.create(WebArchive.class, "simple-servlet.war");
        archive.addClasses(SimpleServlet.class);
        return archive;
    }

    @Test
    public void testEndpoint() throws Exception {
    	
    	String dockerHost = TestUtils.getDockerHost();
    	HttpResponse res = HttpRequest.get("http://" + dockerHost + ":8080/simple-servlet/myservlet?name=Kermit").getResponse();
    	Assert.assertEquals("Unexpected: " + res, "Hello Kermit", res.getBody());
    }
}

