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
import org.wildfly.camel.test.common.HttpResponse;

import javax.ws.rs.core.MediaType;

@RunAsClient
@RunWith(Arquillian.class)
public class JaxwsExampleTest {
    private static final String ENDPOINT_ADDRESS = "http://localhost:8080/example-camel-jaxws/jaxws/";

    @Test
    public void testJaxwsGreeting() throws Exception {
        HttpResponse result = HttpRequest.post(ENDPOINT_ADDRESS)
                .header("Content-Type", MediaType.APPLICATION_FORM_URLENCODED)
                .content("name=Kermit")
                .getResponse();

        Assert.assertTrue(result.getBody().contains("Hello Kermit"));
    }

    @Test
    public void testJaxwsGreetingWithMessage() throws Exception {
        HttpResponse result = HttpRequest.post(ENDPOINT_ADDRESS)
                .header("Content-Type", MediaType.APPLICATION_FORM_URLENCODED)
                .content("name=Kermit&message=Goodbye")
                .getResponse();

        Assert.assertTrue(result.getBody().contains("Goodbye Kermit"));
    }

    @Test
    public void testJaxwsGreetingWithoutNameOrMessage() throws Exception {
        HttpResponse result = HttpRequest.post(ENDPOINT_ADDRESS)
                .header("Content-Type", MediaType.APPLICATION_FORM_URLENCODED)
                .content("name=&message=")
                .getResponse();

        Assert.assertTrue(result.getBody().contains("Hello unknown"));
    }
}
