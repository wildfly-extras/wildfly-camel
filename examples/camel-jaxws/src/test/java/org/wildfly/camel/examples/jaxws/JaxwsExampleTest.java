/*
 * #%L
 * Wildfly Camel :: Example :: JAX-WS
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
package org.wildfly.camel.examples.jaxws;

import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.camel.test.common.HttpRequest;

import java.util.concurrent.TimeUnit;

@RunAsClient
@RunWith(Arquillian.class)
public class JaxwsExampleTest {
    private static final String ENDPOINT_ADDRESS = "http://localhost:8080/example-camel-jaxws/jaxws/";

    @Test
    public void testJaxwsGreeting() throws Exception {
        String result = HttpRequest.post(ENDPOINT_ADDRESS + "?name=Kermit", 10, TimeUnit.SECONDS);
        Assert.assertTrue(result.contains("Hello Kermit"));
    }

    @Test
    public void testJaxwsGreetingWithMessage() throws Exception {
        String result = HttpRequest.post(ENDPOINT_ADDRESS + "?name=Kermit&message=Goodbye", 10, TimeUnit.SECONDS);
        Assert.assertTrue(result.contains("Goodbye Kermit"));
    }

    @Test
    public void testJaxwsGreetingWithoutNameOrMessage() throws Exception {
        String result = HttpRequest.post(ENDPOINT_ADDRESS + "?name=&message=", 10, TimeUnit.SECONDS);
        Assert.assertTrue(result.contains("Hello unknown"));
    }
}
