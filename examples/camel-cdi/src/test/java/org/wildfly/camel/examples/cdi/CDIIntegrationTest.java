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
package org.wildfly.camel.examples.cdi;

import java.net.MalformedURLException;
import java.util.concurrent.TimeUnit;

import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.camel.examples.HttpRequest;

/**
 * Test the CDI component.
 *
 * @author thomas.diesler@jboss.com
 * @since 20-Oct-2014
 */
@RunAsClient
@RunWith(Arquillian.class)
public class CDIIntegrationTest {

    @Test
    public void testSimpleWar() throws Exception {
    	String res = HttpRequest.get(getEndpointAddress("/example-camel-cdi?name=Kermit"), 10, TimeUnit.SECONDS);
        Assert.assertEquals("Hello Kermit", res);
    }

    private String getEndpointAddress(String contextPath) throws MalformedURLException {
        return "http://localhost:8080" + contextPath;
    }
}
