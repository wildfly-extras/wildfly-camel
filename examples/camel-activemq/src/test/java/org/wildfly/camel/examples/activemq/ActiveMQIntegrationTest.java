/*
 * #%L
 * Wildfly Camel :: Example :: Camel CDI
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
package org.wildfly.camel.examples.activemq;

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
public class ActiveMQIntegrationTest {

    @Test
    public void testFileToJpaRoute() throws Exception {
        // Give camel a chance to consume the test order file and pass on to AMQ
        Thread.sleep(2000);

        String res = HttpRequest.get(getEndpointAddress("/example-camel-activemq/orders"), 10, TimeUnit.SECONDS);
        Assert.assertEquals("Processed 1 order files for UK", res.trim());
    }

    private String getEndpointAddress(String contextPath) throws MalformedURLException {
        return "http://localhost:8080" + contextPath;
    }
}
