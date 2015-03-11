/*
 * #%L
 * Wildfly Camel :: Example :: Camel Email
 * %%
 * Copyright (C) 2013 - 2015 RedHat
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
package org.wildfly.camel.examples.mail;

import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.camel.test.common.HttpRequest;

import java.net.MalformedURLException;
import java.util.concurrent.TimeUnit;

@RunAsClient
@RunWith(Arquillian.class)
public class CamelEmailIntegrationTest {

    @Test
    @Ignore("[FIXME #402] CamelEmailIntegrationTest cannot connect")
    public void sendEmailTest() throws Exception {
        StringBuilder endpointURL = new StringBuilder(getEndpointAddress("/example-camel-mail/send"));
        endpointURL.append("?from=user1@localhost")
                   .append("&to=user2@localhost")
                   .append("&subject=Greetings")
                   .append("&message=Hello");

        String res = HttpRequest.post(endpointURL.toString(), 10, TimeUnit.SECONDS);
        Assert.assertTrue("Sent successful: " + res, res.contains("Message sent successfully"));
    }

    private String getEndpointAddress(String contextPath) throws MalformedURLException {
        return "http://localhost:8080" + contextPath;
    }
}
