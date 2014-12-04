/*
 * #%L
 * Wildfly Camel :: Example :: Camel REST
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
package org.wildfly.camel.examples.rest;

import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.camel.examples.HttpRequest;

import java.net.MalformedURLException;
import java.util.concurrent.TimeUnit;


@RunAsClient
@RunWith(Arquillian.class)
public class RestIntegrationTest {

    public static final String REST_PATH = "/example-camel-rest/rest/greet/hello/";

    @Test
    public void testRestRoute() throws Exception {
        String res = HttpRequest.get(getEndpointAddress(REST_PATH + "Kermit"), 10, TimeUnit.SECONDS);
        Assert.assertTrue("Starts with 'Hello Kermit': " + res, res.startsWith("Hello Kermit"));
    }

    private String getEndpointAddress(String restPath) throws MalformedURLException {
        return "http://localhost:8080" + restPath;
    }
}
